package com.rongzer.connector.bigfilesftp.dto;

import java.math.BigDecimal;

/**
 * SFTP 同步配置响应。
 *
 * @param host 源 SFTP 服务器地址
 * @param port 源 SFTP 服务器端口
 * @param username 源 SFTP 用户名
 * @param password 源 SFTP 密码
 * @param sftpPath 源 SFTP 绝对文件路径
 * @param bandwidthLimitMbps 总传输限速，单位 MB/s
 * @param parallelCount 并发同步文件数
 * @param targetType 同步目标类型
 * @param syncPath 本地文件同步路径
 * @param targetHost 目标 SFTP 服务器地址
 * @param targetPort 目标 SFTP 服务器端口
 * @param targetUsername 目标 SFTP 用户名
 * @param targetPassword 目标 SFTP 密码
 * @param targetPath 目标 SFTP 绝对文件路径
 */
public record SftpConfigResponse(
        String host,
        Integer port,
        String username,
        String password,
        String sftpPath,
        BigDecimal bandwidthLimitMbps,
        Integer parallelCount,
        String targetType,
        String syncPath,
        String targetHost,
        Integer targetPort,
        String targetUsername,
        String targetPassword,
        String targetPath,
        String targetS3Endpoint,
        String targetS3AccessKey,
        String targetS3SecretKey,
        String targetS3Bucket,
        String targetS3Prefix,
        String targetS3Region,
        Boolean targetS3PathStyleAccess,
        String targetSmbHost,
        String targetSmbShare,
        String targetSmbDomain,
        String targetSmbUsername,
        String targetSmbPassword,
        String targetSmbPath,
        String targetWebdavBaseUrl,
        String targetWebdavUsername,
        String targetWebdavPassword,
        String targetWebdavPath,
        String targetHttpUrl,
        String targetHttpMethod,
        String targetHttpUsername,
        String targetHttpPassword,
        String targetHttpFileField,
        String targetHttpPathParam
) {
}
