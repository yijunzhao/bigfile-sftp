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
    @Column(name = "target_smb_host", length = 255)
    private String targetSmbHost;
    @Column(name = "target_smb_share", length = 100)
    private String targetSmbShare;
    @Column(name = "target_smb_domain", length = 100)
    private String targetSmbDomain;
    @Column(name = "target_smb_username", length = 100)
    private String targetSmbUsername;
    @Column(name = "target_smb_password", length = 100)
    private String targetSmbPassword;
    @Column(name = "target_smb_path", length = 255)
    private String targetSmbPath;
    @Column(name = "target_webdav_base_url", length = 500)
    private String targetWebdavBaseUrl;
    @Column(name = "target_webdav_username", length = 100)
    private String targetWebdavUsername;
    @Column(name = "target_webdav_password", length = 100)
    private String targetWebdavPassword;
    @Column(name = "target_webdav_path", length = 255)
    private String targetWebdavPath;
    @Column(name = "target_http_url", length = 500)
    private String targetHttpUrl;
    @Column(name = "target_http_method", length = 20)
    private String targetHttpMethod;
    @Column(name = "target_http_username", length = 100)
    private String targetHttpUsername;
    @Column(name = "target_http_password", length = 100)
    private String targetHttpPassword;
    @Column(name = "target_http_file_field", length = 100)
    private String targetHttpFileField;
    @Column(name = "target_http_path_param", length = 100)
    private String targetHttpPathParam;

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

    public String getTargetSmbHost() { return targetSmbHost; }
    public void setTargetSmbHost(String targetSmbHost) { this.targetSmbHost = targetSmbHost; }
    public String getTargetSmbShare() { return targetSmbShare; }
    public void setTargetSmbShare(String targetSmbShare) { this.targetSmbShare = targetSmbShare; }
    public String getTargetSmbDomain() { return targetSmbDomain; }
    public void setTargetSmbDomain(String targetSmbDomain) { this.targetSmbDomain = targetSmbDomain; }
    public String getTargetSmbUsername() { return targetSmbUsername; }
    public void setTargetSmbUsername(String targetSmbUsername) { this.targetSmbUsername = targetSmbUsername; }
    public String getTargetSmbPassword() { return targetSmbPassword; }
    public void setTargetSmbPassword(String targetSmbPassword) { this.targetSmbPassword = targetSmbPassword; }
    public String getTargetSmbPath() { return targetSmbPath; }
    public void setTargetSmbPath(String targetSmbPath) { this.targetSmbPath = targetSmbPath; }
    public String getTargetWebdavBaseUrl() { return targetWebdavBaseUrl; }
    public void setTargetWebdavBaseUrl(String targetWebdavBaseUrl) { this.targetWebdavBaseUrl = targetWebdavBaseUrl; }
    public String getTargetWebdavUsername() { return targetWebdavUsername; }
    public void setTargetWebdavUsername(String targetWebdavUsername) { this.targetWebdavUsername = targetWebdavUsername; }
    public String getTargetWebdavPassword() { return targetWebdavPassword; }
    public void setTargetWebdavPassword(String targetWebdavPassword) { this.targetWebdavPassword = targetWebdavPassword; }
    public String getTargetWebdavPath() { return targetWebdavPath; }
    public void setTargetWebdavPath(String targetWebdavPath) { this.targetWebdavPath = targetWebdavPath; }
    public String getTargetHttpUrl() { return targetHttpUrl; }
    public void setTargetHttpUrl(String targetHttpUrl) { this.targetHttpUrl = targetHttpUrl; }
    public String getTargetHttpMethod() { return targetHttpMethod; }
    public void setTargetHttpMethod(String targetHttpMethod) { this.targetHttpMethod = targetHttpMethod; }
    public String getTargetHttpUsername() { return targetHttpUsername; }
    public void setTargetHttpUsername(String targetHttpUsername) { this.targetHttpUsername = targetHttpUsername; }
    public String getTargetHttpPassword() { return targetHttpPassword; }
    public void setTargetHttpPassword(String targetHttpPassword) { this.targetHttpPassword = targetHttpPassword; }
    public String getTargetHttpFileField() { return targetHttpFileField; }
    public void setTargetHttpFileField(String targetHttpFileField) { this.targetHttpFileField = targetHttpFileField; }
    public String getTargetHttpPathParam() { return targetHttpPathParam; }
    public void setTargetHttpPathParam(String targetHttpPathParam) { this.targetHttpPathParam = targetHttpPathParam; }
}
