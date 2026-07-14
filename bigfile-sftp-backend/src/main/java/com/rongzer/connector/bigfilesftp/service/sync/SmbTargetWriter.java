package com.rongzer.connector.bigfilesftp.service.sync;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;

/**
 * SMB/NAS 共享目录目标端操作器。
 */
public class SmbTargetWriter implements TargetWriter {

    private final String host;
    private final String shareName;
    private final String domain;
    private final String username;
    private final String password;
    private final String rootPath;

    public SmbTargetWriter(String host, String shareName, String domain, String username, String password, String rootPath) {
        this.host = host;
        this.shareName = shareName;
        this.domain = domain;
        this.username = username;
        this.password = password;
        this.rootPath = normalizeSmbPath(rootPath);
    }

    @Override
    public String temporaryFileName(String fileName) {
        return fileName + ".part";
    }

    @Override
    public long finalSize(String fileName) throws Exception {
        try (SmbConnection connection = connect()) {
            return size(connection.share(), smbFilePath(fileName));
        }
    }

    @Override
    public long temporarySize(String fileName) throws Exception {
        try (SmbConnection connection = connect()) {
            return size(connection.share(), smbFilePath(temporaryFileName(fileName)));
        }
    }

    @Override
    public void moveFinalToTemporary(String fileName) throws Exception {
        try (SmbConnection connection = connect()) {
            String finalPath = smbFilePath(fileName);
            String temporaryPath = smbFilePath(temporaryFileName(fileName));
            ensureParentDirectories(connection.share(), temporaryPath);
            removeIfExists(connection.share(), temporaryPath);
            rename(connection.share(), finalPath, temporaryPath);
        }
    }

    @Override
    public void promoteTemporaryToFinal(String fileName) throws Exception {
        try (SmbConnection connection = connect()) {
            String finalPath = smbFilePath(fileName);
            String temporaryPath = smbFilePath(temporaryFileName(fileName));
            ensureParentDirectories(connection.share(), finalPath);
            removeIfExists(connection.share(), finalPath);
            rename(connection.share(), temporaryPath, finalPath);
        }
    }

    @Override
    public void appendToTemporary(String fileName, InputStream inputStream) throws Exception {
        try (SmbConnection connection = connect()) {
            String temporaryPath = smbFilePath(temporaryFileName(fileName));
            ensureParentDirectories(connection.share(), temporaryPath);
            long offset = size(connection.share(), temporaryPath);
            try (File file = openWritableFile(connection.share(), temporaryPath)) {
                byte[] buffer = new byte[1024 * 1024];
                int count;
                while ((count = inputStream.read(buffer)) != -1) {
                    file.write(buffer, offset, 0, count);
                    offset += count;
                }
            }
        }
    }

    private SmbConnection connect() throws IOException {
        SMBClient client = new SMBClient();
        Connection connection = client.connect(host);
        AuthenticationContext authenticationContext = new AuthenticationContext(username, password.toCharArray(), domain);
        com.hierynomus.smbj.session.Session session = connection.authenticate(authenticationContext);
        DiskShare share = (DiskShare) session.connectShare(shareName);
        return new SmbConnection(client, connection, session, share);
    }

    private File openWritableFile(DiskShare share, String path) {
        return share.openFile(
                path,
                EnumSet.of(AccessMask.GENERIC_WRITE, AccessMask.FILE_READ_ATTRIBUTES),
                EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN_IF,
                EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
        );
    }

    private void rename(DiskShare share, String sourcePath, String targetPath) {
        try (File file = share.openFile(
                sourcePath,
                EnumSet.of(AccessMask.DELETE, AccessMask.FILE_READ_ATTRIBUTES),
                EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
        )) {
            file.rename(targetPath, true);
        }
    }

    private long size(DiskShare share, String path) {
        try {
            if (!share.fileExists(path)) {
                return 0L;
            }
            return share.getFileInformation(path).getStandardInformation().getEndOfFile();
        } catch (Exception exception) {
            return 0L;
        }
    }

    private void removeIfExists(DiskShare share, String path) {
        try {
            if (share.fileExists(path)) {
                share.rm(path);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("删除SMB文件失败: " + path, exception);
        }
    }

    private void ensureParentDirectories(DiskShare share, String filePath) {
        String parentPath = parentPath(filePath);
        if (parentPath.isBlank()) {
            return;
        }
        StringBuilder currentPath = new StringBuilder();
        for (String segment : parentPath.split("\\\\+")) {
            if (segment.isBlank()) {
                continue;
            }
            if (!currentPath.isEmpty()) {
                currentPath.append('\\');
            }
            currentPath.append(segment);
            String directory = currentPath.toString();
            if (!share.folderExists(directory)) {
                share.mkdir(directory);
            }
        }
    }

    private String smbFilePath(String fileName) {
        String relativePath = normalizeSmbPath(fileName);
        return rootPath.isBlank() ? relativePath : rootPath + "\\" + relativePath;
    }

    private String parentPath(String path) {
        int lastSeparator = path.lastIndexOf('\\');
        return lastSeparator < 0 ? "" : path.substring(0, lastSeparator);
    }

    private static String normalizeSmbPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        return path.trim().replace('/', '\\').replaceAll("^\\\\+", "").replaceAll("\\\\+$", "");
    }

    /**
     * SMB 连接资源封装。
     */
    private record SmbConnection(SMBClient client, Connection connection, com.hierynomus.smbj.session.Session session, DiskShare share) implements AutoCloseable {

        @Override
        public void close() throws IOException {
            IOException closeException = null;
            closeException = closeResource(share, closeException);
            closeException = closeResource(session, closeException);
            closeException = closeResource(connection, closeException);
            closeException = closeResource(client, closeException);
            if (closeException != null) {
                throw closeException;
            }
        }

        private IOException closeResource(AutoCloseable resource, IOException closeException) {
            if (resource == null) {
                return closeException;
            }
            try {
                resource.close();
            } catch (Exception exception) {
                if (closeException == null) {
                    return exception instanceof IOException ioException ? ioException : new IOException(exception);
                }
                closeException.addSuppressed(exception);
            }
            return closeException;
        }
    }
}
