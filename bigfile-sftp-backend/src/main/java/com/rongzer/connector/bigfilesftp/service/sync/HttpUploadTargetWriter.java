package com.rongzer.connector.bigfilesftp.service.sync;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * HTTP 自定义上传接口目标端操作器。
 */
public class HttpUploadTargetWriter implements TargetWriter {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String url;
    private final String method;
    private final String username;
    private final String password;
    private final String fileField;
    private final String pathParam;

    public HttpUploadTargetWriter(String url, String method, String username, String password, String fileField, String pathParam) {
        this.url = url;
        this.method = method;
        this.username = username;
        this.password = password;
        this.fileField = fileField;
        this.pathParam = pathParam;
    }

    @Override
    public String temporaryFileName(String fileName) {
        return fileName;
    }

    @Override
    public long finalSize(String fileName) {
        return 0L;
    }

    @Override
    public long temporarySize(String fileName) {
        return 0L;
    }

    @Override
    public void moveFinalToTemporary(String fileName) {
    }

    @Override
    public void promoteTemporaryToFinal(String fileName) {
    }

    @Override
    public void appendToTemporary(String fileName, InputStream inputStream) throws Exception {
        String boundary = "----bigfile-sftp-" + UUID.randomUUID();
        HttpRequest.BodyPublisher bodyPublisher = multipartBodyPublisher(boundary, fileName, inputStream);
        HttpRequest request = withAuthorization(HttpRequest.newBuilder(URI.create(url)))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .method(method, bodyPublisher)
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP上传失败，HTTP状态码=" + response.statusCode() + "，响应=" + response.body());
        }
    }

    @Override
    public boolean supportsAppendResume() {
        return false;
    }

    /**
     * 获取页面展示用目标地址。
     */
    public String displayUri() {
        return url;
    }

    private HttpRequest.BodyPublisher multipartBodyPublisher(String boundary, String fileName, InputStream inputStream) {
        byte[] prefix = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + pathParam + "\"\r\n\r\n"
                + fileName + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + baseName(fileName) + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        return HttpRequest.BodyPublishers.ofInputStream(() -> new MultipartInputStream(prefix, inputStream, suffix));
    }

    private String baseName(String fileName) {
        int index = fileName.lastIndexOf('/');
        return index < 0 ? fileName : fileName.substring(index + 1);
    }

    private HttpRequest.Builder withAuthorization(HttpRequest.Builder builder) {
        if (username == null || username.isBlank()) {
            return builder;
        }
        String token = Base64.getEncoder().encodeToString((username + ":" + Objects.toString(password, "")).getBytes(StandardCharsets.UTF_8));
        return builder.header("Authorization", "Basic " + token);
    }

    /**
     * 组合 multipart 前缀、文件流和后缀的输入流。
     */
    private static class MultipartInputStream extends InputStream {

        private final List<InputStream> streams;
        private int currentIndex;

        private MultipartInputStream(byte[] prefix, InputStream fileStream, byte[] suffix) {
            this.streams = List.of(new java.io.ByteArrayInputStream(prefix), fileStream, new java.io.ByteArrayInputStream(suffix));
        }

        @Override
        public int read() throws IOException {
            while (currentIndex < streams.size()) {
                int value = streams.get(currentIndex).read();
                if (value != -1) {
                    return value;
                }
                currentIndex++;
            }
            return -1;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            while (currentIndex < streams.size()) {
                int count = streams.get(currentIndex).read(buffer, offset, length);
                if (count != -1) {
                    return count;
                }
                currentIndex++;
            }
            return -1;
        }

        @Override
        public void close() throws IOException {
            IOException closeException = null;
            for (InputStream stream : streams) {
                try {
                    stream.close();
                } catch (IOException exception) {
                    if (closeException == null) {
                        closeException = exception;
                    } else {
                        closeException.addSuppressed(exception);
                    }
                }
            }
            if (closeException != null) {
                throw closeException;
            }
        }
    }
}
