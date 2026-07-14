package com.rongzer.connector.bigfilesftp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * SFTP 同步配置请求。
 *
 * @param host 源 SFTP 服务器地址
 * @param port 源 SFTP 服务器端口
 * @param username 源 SFTP 用户名
 * @param password 源 SFTP 密码
 * @param sftpPath 源 SFTP 绝对文件路径
 * @param bandwidthLimitMbps 总传输限速，单位 MB/s，为空表示不限速
 * @param parallelCount 并发同步文件数
 * @param targetType 同步目标类型，LOCAL 表示本地路径，SFTP 表示远程 SFTP 服务
 * @param syncPath 本地文件同步路径
 * @param targetHost 目标 SFTP 服务器地址
 * @param targetPort 目标 SFTP 服务器端口
 * @param targetUsername 目标 SFTP 用户名
 * @param targetPassword 目标 SFTP 密码
 * @param targetPath 目标 SFTP 绝对文件路径
 */
public record SftpConfigRequest(
        @NotBlank(message = "SFTP服务器地址不能为空")
        @Size(max = 255, message = "SFTP服务器地址最长255字符")
        String host,

        @NotNull(message = "SFTP服务器端口不能为空")
        @Min(value = 1, message = "SFTP服务器端口必须大于0")
        @Max(value = 65535, message = "SFTP服务器端口不能超过65535")
        Integer port,

        @NotBlank(message = "用户名不能为空")
        @Size(max = 20, message = "用户名最长20字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(max = 50, message = "密码最长50字符")
        String password,

        @NotBlank(message = "SFTP文件路径不能为空")
        @Size(max = 255, message = "SFTP文件路径最长255字符")
        @Pattern(regexp = "^/.*", message = "SFTP文件路径必须是以/开头的绝对路径")
        String sftpPath,

        @jakarta.validation.constraints.DecimalMin(value = "0.01", message = "传输限速必须大于等于0.01 MB/s")
        BigDecimal bandwidthLimitMbps,

        @Min(value = 1, message = "并发文件数必须大于0")
        @Max(value = 16, message = "并发文件数不能超过16")
        Integer parallelCount,

        @NotBlank(message = "目标类型不能为空")
        @Pattern(regexp = "LOCAL|SFTP|S3", message = "目标类型只能是LOCAL、SFTP或S3")
        String targetType,

        @Size(max = 255, message = "文件同步路径最长255字符")
        String syncPath,

        @Size(max = 255, message = "目标SFTP服务器地址最长255字符")
        String targetHost,

        @Min(value = 1, message = "目标SFTP服务器端口必须大于0")
        @Max(value = 65535, message = "目标SFTP服务器端口不能超过65535")
        Integer targetPort,

        @Size(max = 20, message = "目标用户名最长20字符")
        String targetUsername,

        @Size(max = 50, message = "目标密码最长50字符")
        String targetPassword,

        @Size(max = 255, message = "目标SFTP文件路径最长255字符")
        String targetPath,

        @Size(max = 255, message = "S3 Endpoint最长255字符")
        String targetS3Endpoint,

        @Size(max = 100, message = "S3 Access Key最长100字符")
        String targetS3AccessKey,

        @Size(max = 100, message = "S3 Secret Key最长100字符")
        String targetS3SecretKey,

        @Size(max = 100, message = "S3 Bucket最长100字符")
        String targetS3Bucket,

        @Size(max = 255, message = "S3对象前缀最长255字符")
        String targetS3Prefix,

        @Size(max = 50, message = "S3 Region最长50字符")
        String targetS3Region,

        Boolean targetS3PathStyleAccess
) {
}
