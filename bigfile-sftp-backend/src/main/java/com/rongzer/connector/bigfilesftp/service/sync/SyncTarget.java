package com.rongzer.connector.bigfilesftp.service.sync;

/**
 * 同步目标信息。
 *
 * @param uri 日志展示用目标 URI
 * @param displayPath 页面展示用目标路径
 * @param writer 文件型目标写入器
 * @param s3Target S3 兼容对象存储目标配置
 */
public record SyncTarget(String uri, String displayPath, TargetWriter writer, S3Target s3Target) {
}
