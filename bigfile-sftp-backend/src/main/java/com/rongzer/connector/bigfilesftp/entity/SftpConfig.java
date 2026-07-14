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
}
