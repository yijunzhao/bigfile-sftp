package com.rongzer.connector.bigfilesftp.service.sync;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * WebDAV 和 HTTP 上传目标共用工具。
 */
final class HttpTargetSupport {

    private HttpTargetSupport() {
    }

    /**
     * 编码相对 URL 路径，保留目录分隔符。
     */
    static String encodeRelativePath(String path) {
        String[] segments = path.replace('\\', '/').replaceAll("^/+", "").split("/", -1);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < segments.length; index++) {
            if (index > 0) {
                builder.append('/');
            }
            if (!segments[index].isBlank()) {
                builder.append(URLEncoder.encode(segments[index], StandardCharsets.UTF_8).replace("+", "%20"));
            }
        }
        return builder.toString();
    }

    /**
     * 提取相对路径的父目录。
     */
    static String parentDirectory(String fileName) {
        int lastSeparator = fileName.lastIndexOf('/');
        return lastSeparator < 0 ? "" : fileName.substring(0, lastSeparator);
    }
}
