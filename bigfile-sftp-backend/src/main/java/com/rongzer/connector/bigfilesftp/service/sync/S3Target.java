package com.rongzer.connector.bigfilesftp.service.sync;

/**
 * S3 兼容对象存储目标配置。
 *
 * @param endpoint 服务地址
 * @param accessKey 访问密钥 ID
 * @param secretKey 访问密钥 Secret
 * @param bucket 存储桶
 * @param prefix 对象前缀
 * @param region 区域
 * @param pathStyleAccess 是否使用 Path Style 访问方式
 */
public record S3Target(String endpoint, String accessKey, String secretKey, String bucket, String prefix, String region, boolean pathStyleAccess) {
}
