package com.rongzer.connector.bigfilesftp.service.sync;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * WebDAV 目标端操作器。
 */
public class WebdavTargetWriter implements TargetWriter {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String baseUrl;
    private final String username;
    private final String password;
    private final String rootPath;

    public WebdavTargetWriter(String baseUrl, String username, String password, String rootPath) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.username = username;
        this.password = password;
        this.rootPath = rootPath;
    }

    @Override
    public String temporaryFileName(String fileName) {
        return fileName + ".part";
    }

    @Override
    public long finalSize(String fileName) throws Exception {
        return contentLength(fileUri(fileName));
    }

    @Override
    public long temporarySize(String fileName) throws Exception {
        return contentLength(fileUri(temporaryFileName(fileName)));
    }

    @Override
    public void moveFinalToTemporary(String fileName) throws Exception {
        deleteIfExists(fileUri(temporaryFileName(fileName)));
        move(fileUri(fileName), fileUri(temporaryFileName(fileName)));
    }

    @Override
    public void promoteTemporaryToFinal(String fileName) throws Exception {
        deleteIfExists(fileUri(fileName));
        move(fileUri(temporaryFileName(fileName)), fileUri(fileName));
    }

    @Override
    public void appendToTemporary(String fileName, InputStream inputStream) throws Exception {
        ensureDirectories(fileName);
        HttpRequest request = withAuthorization(HttpRequest.newBuilder(fileUri(temporaryFileName(fileName))))
                .PUT(HttpRequest.BodyPublishers.ofInputStream(() -> inputStream))
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        requireSuccess(response.statusCode(), "WebDAV上传失败");
    }

    @Override
    public boolean supportsAppendResume() {
        return false;
    }

    /**
     * 获取页面展示用目标地址。
     */
    public String displayUri() {
        return baseUrl + (rootPath.isBlank() ? "" : "/" + rootPath);
    }

    private long contentLength(URI uri) throws Exception {
        HttpRequest request = withAuthorization(HttpRequest.newBuilder(uri)).method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() == 404) {
            return 0L;
        }
        requireSuccess(response.statusCode(), "WebDAV读取文件大小失败");
        return response.headers().firstValueAsLong("Content-Length").orElse(0L);
    }

    private void ensureDirectories(String fileName) throws Exception {
        String directoryPath = HttpTargetSupport.parentDirectory(rootPath.isBlank() ? fileName : rootPath + "/" + fileName);
        if (directoryPath.isBlank()) {
            return;
        }
        StringBuilder currentPath = new StringBuilder();
        for (String segment : directoryPath.split("/+")) {
            if (segment.isBlank()) {
                continue;
            }
            if (!currentPath.isEmpty()) {
                currentPath.append('/');
            }
            currentPath.append(segment);
            mkcol(directoryUri(currentPath.toString()));
        }
    }

    private void mkcol(URI uri) throws Exception {
        HttpRequest request = withAuthorization(HttpRequest.newBuilder(uri)).method("MKCOL", HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != 201 && response.statusCode() != 405) {
            requireSuccess(response.statusCode(), "WebDAV创建目录失败");
        }
    }

    private void deleteIfExists(URI uri) throws Exception {
        HttpRequest request = withAuthorization(HttpRequest.newBuilder(uri)).DELETE().build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != 404 && response.statusCode() >= 400) {
            throw new IOException("WebDAV删除文件失败，HTTP状态码=" + response.statusCode());
        }
    }

    private void move(URI sourceUri, URI targetUri) throws Exception {
        HttpRequest request = withAuthorization(HttpRequest.newBuilder(sourceUri))
                .header("Destination", targetUri.toString())
                .header("Overwrite", "T")
                .method("MOVE", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        requireSuccess(response.statusCode(), "WebDAV重命名文件失败");
    }

    private URI fileUri(String fileName) {
        String relativePath = rootPath.isBlank() ? fileName : rootPath + "/" + fileName;
        return directoryUri(relativePath);
    }

    private URI directoryUri(String relativePath) {
        return URI.create(baseUrl + "/" + HttpTargetSupport.encodeRelativePath(relativePath));
    }

    private HttpRequest.Builder withAuthorization(HttpRequest.Builder builder) {
        if (username == null || username.isBlank()) {
            return builder;
        }
        String token = Base64.getEncoder().encodeToString((username + ":" + Objects.toString(password, "")).getBytes(StandardCharsets.UTF_8));
        return builder.header("Authorization", "Basic " + token);
    }

    private void requireSuccess(int statusCode, String message) throws IOException {
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException(message + "，HTTP状态码=" + statusCode);
        }
    }
}
