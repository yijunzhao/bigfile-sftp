package com.rongzer.connector.bigfilesftp.service.sync;

/**
 * 源文件信息。
 *
 * @param relativePath 相对源目录的文件路径
 * @param size 文件大小，单位字节
 */
public record SourceFile(String relativePath, long size) {
}
