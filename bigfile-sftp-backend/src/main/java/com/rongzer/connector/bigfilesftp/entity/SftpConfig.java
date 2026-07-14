package com.rongzer.connector.bigfilesftp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "sftp_config")
/**
 * SFTP 同步配置实体。
 *
 * <p>该表只保存一份页面配置，使用固定主键进行覆盖式更新。</p>
 */
public class SftpConfig {

    @Id
    private Long id;

    @Column(name = "host", nullable = false, length = 255)
    private String host;

    @Column(name = "port", nullable = false)
    private Integer port;
    @Column(name = "username", nullable = false, length = 20)
    private String username;
    @Column(name = "password", nullable = false, length = 50)
    private String password;
    @Column(name = "sftp_path", length = 255)
    private String sftpPath;
    @Column(name = "bandwidth_limit_mbps", precision = 10, scale = 3)
    private BigDecimal bandwidthLimitMbps;
    @Column(name = "parallel_count")
    private Integer parallelCount;
    @Column(name = "target_type", length = 20)
    private String targetType;
    @Column(name = "sync_path", length = 255)
    private String syncPath;
    @Column(name = "target_host", length = 255)
    private String targetHost;
    @Column(name = "target_port")
    private Integer targetPort;
    @Column(name = "target_username", length = 20)
    private String targetUsername;
    @Column(name = "target_password", length = 50)
    private String targetPassword;
    @Column(name = "target_path", length = 255)
    private String targetPath;
    @Column(name = "target_s3_endpoint", length = 255)
    private String targetS3Endpoint;
    @Column(name = "target_s3_access_key", length = 100)
    private String targetS3AccessKey;
    @Column(name = "target_s3_secret_key", length = 100)
    private String targetS3SecretKey;
    @Column(name = "target_s3_bucket", length = 100)
    private String targetS3Bucket;
    @Column(name = "target_s3_prefix", length = 255)
    private String targetS3Prefix;
    @Column(name = "target_s3_region", length = 50)
    private String targetS3Region;
    @Column(name = "target_s3_path_style_access")
    private Boolean targetS3PathStyleAccess;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSftpPath() {
        return sftpPath;
    }

    public void setSftpPath(String sftpPath) {
        this.sftpPath = sftpPath;
    }

    public BigDecimal getBandwidthLimitMbps() {
        return bandwidthLimitMbps;
    }

    public void setBandwidthLimitMbps(BigDecimal bandwidthLimitMbps) {
        this.bandwidthLimitMbps = bandwidthLimitMbps;
    }

    public Integer getParallelCount() {
        return parallelCount;
    }

    public void setParallelCount(Integer parallelCount) {
        this.parallelCount = parallelCount;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getSyncPath() {
        return syncPath;
    }

    public void setSyncPath(String syncPath) {
        this.syncPath = syncPath;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    public Integer getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(Integer targetPort) {
        this.targetPort = targetPort;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public String getTargetPassword() {
        return targetPassword;
    }

    public void setTargetPassword(String targetPassword) {
        this.targetPassword = targetPassword;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getTargetS3Endpoint() {
        return targetS3Endpoint;
    }

    public void setTargetS3Endpoint(String targetS3Endpoint) {
        this.targetS3Endpoint = targetS3Endpoint;
    }

    public String getTargetS3AccessKey() {
        return targetS3AccessKey;
    }

    public void setTargetS3AccessKey(String targetS3AccessKey) {
        this.targetS3AccessKey = targetS3AccessKey;
    }

    public String getTargetS3SecretKey() {
        return targetS3SecretKey;
    }

    public void setTargetS3SecretKey(String targetS3SecretKey) {
        this.targetS3SecretKey = targetS3SecretKey;
    }

    public String getTargetS3Bucket() {
        return targetS3Bucket;
    }

    public void setTargetS3Bucket(String targetS3Bucket) {
        this.targetS3Bucket = targetS3Bucket;
    }

    public String getTargetS3Prefix() {
        return targetS3Prefix;
    }

    public void setTargetS3Prefix(String targetS3Prefix) {
        this.targetS3Prefix = targetS3Prefix;
    }

    public String getTargetS3Region() {
        return targetS3Region;
    }

    public void setTargetS3Region(String targetS3Region) {
        this.targetS3Region = targetS3Region;
    }

    public Boolean getTargetS3PathStyleAccess() {
        return targetS3PathStyleAccess;
    }

    public void setTargetS3PathStyleAccess(Boolean targetS3PathStyleAccess) {
        this.targetS3PathStyleAccess = targetS3PathStyleAccess;
    }
}
