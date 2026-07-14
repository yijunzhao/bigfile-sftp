package com.rongzer.connector.bigfilesftp.service.sync;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;
import java.util.Properties;

/**
 * 远程 SFTP 目标端操作器。
 */
public class SftpTargetWriter implements TargetWriter {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String rootPath;

    public SftpTargetWriter(String host, int port, String username, String password, String rootPath) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.rootPath = rootPath.replaceAll("/+$", "");
    }

    @Override
    public String temporaryFileName(String fileName) {
        return fileName + ".part";
    }

    @Override
    public long finalSize(String fileName) throws Exception {
        try (SftpConnection connection = connect()) {
            return size(connection.channel(), remoteFilePath(fileName));
        }
    }

    @Override
    public long temporarySize(String fileName) throws Exception {
        try (SftpConnection connection = connect()) {
            return size(connection.channel(), remoteFilePath(temporaryFileName(fileName)));
        }
    }

    @Override
    public void moveFinalToTemporary(String fileName) throws Exception {
        try (SftpConnection connection = connect()) {
            String finalPath = remoteFilePath(fileName);
            String temporaryPath = remoteFilePath(temporaryFileName(fileName));
            ensureParentDirectories(connection.channel(), temporaryPath);
            removeIfExists(connection.channel(), temporaryPath);
            connection.channel().rename(finalPath, temporaryPath);
        }
    }

    @Override
    public void promoteTemporaryToFinal(String fileName) throws Exception {
        try (SftpConnection connection = connect()) {
            String finalPath = remoteFilePath(fileName);
            String temporaryPath = remoteFilePath(temporaryFileName(fileName));
            ensureParentDirectories(connection.channel(), finalPath);
            removeIfExists(connection.channel(), finalPath);
            connection.channel().rename(temporaryPath, finalPath);
        }
    }

    @Override
    public void appendToTemporary(String fileName, InputStream inputStream) throws Exception {
        try (SftpConnection connection = connect()) {
            String temporaryPath = remoteFilePath(temporaryFileName(fileName));
            ensureParentDirectories(connection.channel(), temporaryPath);
            connection.channel().put(inputStream, temporaryPath, ChannelSftp.APPEND);
        }
    }

    private SftpConnection connect() throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect(15_000);
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect(15_000);
        return new SftpConnection(session, channel);
    }

    private long size(ChannelSftp channel, String path) throws Exception {
        try {
            return channel.stat(path).getSize();
        } catch (com.jcraft.jsch.SftpException exception) {
            if (exception.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return 0L;
            }
            throw exception;
        }
    }

    private void removeIfExists(ChannelSftp channel, String path) throws Exception {
        try {
            channel.rm(path);
        } catch (com.jcraft.jsch.SftpException exception) {
            if (exception.id != ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                throw exception;
            }
        }
    }

    /**
     * 自动创建目标 SFTP 文件的父目录，补齐手动流式写入时缺失的 autoCreate 能力。
     */
    private void ensureParentDirectories(ChannelSftp channel, String filePath) throws Exception {
        int lastSeparator = filePath.lastIndexOf('/');
        if (lastSeparator <= 0) {
            return;
        }
        String parentPath = filePath.substring(0, lastSeparator).replaceAll("/+$", "");
        if (parentPath.isBlank() || "/".equals(parentPath)) {
            return;
        }
        StringBuilder currentPath = new StringBuilder(filePath.startsWith("/") ? "/" : "");
        for (String segment : parentPath.replaceAll("^/+", "").split("/+")) {
            if (segment.isBlank()) {
                continue;
            }
            if (currentPath.length() > 1 && currentPath.charAt(currentPath.length() - 1) != '/') {
                currentPath.append('/');
            }
            currentPath.append(segment);
            String directory = currentPath.toString();
            try {
                channel.stat(directory);
            } catch (com.jcraft.jsch.SftpException exception) {
                if (exception.id != ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    throw exception;
                }
                channel.mkdir(directory);
            }
        }
    }

    private String remoteFilePath(String fileName) {
        return rootPath + "/" + fileName.replace('\\', '/').replaceAll("^/+", "");
    }
}
