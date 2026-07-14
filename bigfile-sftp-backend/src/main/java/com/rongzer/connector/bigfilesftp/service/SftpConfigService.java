package com.rongzer.connector.bigfilesftp.service;

import com.rongzer.connector.bigfilesftp.dto.SftpConfigRequest;
import com.rongzer.connector.bigfilesftp.dto.SftpConfigResponse;
import com.rongzer.connector.bigfilesftp.entity.SftpConfig;
import com.rongzer.connector.bigfilesftp.repository.SftpConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * SFTP 配置服务。
 *
 * <p>负责配置的读取、保存和目标端条件校验。</p>
 */
public class SftpConfigService {

    private static final long SINGLE_CONFIG_ID = 1L;

    private final SftpConfigRepository repository;

    public SftpConfigService(SftpConfigRepository repository) {
        this.repository = repository;
    }

    /**
     * 查询当前保存的配置。
     *
     * @return 当前配置；如果尚未保存，则返回默认空配置
     */
    @Transactional(readOnly = true)
    public SftpConfigResponse getConfig() {
        return repository.findById(SINGLE_CONFIG_ID)
                .map(this::toResponse)
                .orElse(new SftpConfigResponse("", 22, "", "", "", null, 1, "LOCAL", "", "", 22, "", "", "", "", "", "", "", "", "us-east-1", true, "", "", "", "", "", "", "", "", "", "", "", "POST", "", "", "file", "path"));
    }

    /**
     * 保存配置。
     *
     * @param request 配置请求
     * @return 保存后的配置响应
     */
    @Transactional
    public SftpConfigResponse saveConfig(SftpConfigRequest request) {
        validateTarget(request);
        SftpConfig config = repository.findById(SINGLE_CONFIG_ID).orElseGet(SftpConfig::new);
        config.setId(SINGLE_CONFIG_ID);
        config.setHost(request.host().trim());
        config.setPort(request.port());
        config.setUsername(request.username().trim());
        config.setPassword(request.password());
        config.setSftpPath(request.sftpPath().trim());
        config.setBandwidthLimitMbps(request.bandwidthLimitMbps());
        config.setParallelCount(request.parallelCount() == null ? 1 : request.parallelCount());
        config.setTargetType(request.targetType());
        config.setSyncPath(trimToEmpty(request.syncPath()));
        config.setTargetHost(trimToEmpty(request.targetHost()));
        config.setTargetPort(request.targetPort());
        config.setTargetUsername(trimToEmpty(request.targetUsername()));
        config.setTargetPassword(request.targetPassword());
        config.setTargetPath(trimToEmpty(request.targetPath()));
        config.setTargetS3Endpoint(trimToEmpty(request.targetS3Endpoint()));
        config.setTargetS3AccessKey(trimToEmpty(request.targetS3AccessKey()));
        config.setTargetS3SecretKey(request.targetS3SecretKey());
        config.setTargetS3Bucket(trimToEmpty(request.targetS3Bucket()));
        config.setTargetS3Prefix(trimToEmpty(request.targetS3Prefix()));
        config.setTargetS3Region(trimToEmpty(request.targetS3Region()).isBlank() ? "us-east-1" : trimToEmpty(request.targetS3Region()));
        config.setTargetS3PathStyleAccess(request.targetS3PathStyleAccess() == null || request.targetS3PathStyleAccess());
        config.setTargetSmbHost(trimToEmpty(request.targetSmbHost()));
        config.setTargetSmbShare(trimToEmpty(request.targetSmbShare()));
        config.setTargetSmbDomain(trimToEmpty(request.targetSmbDomain()));
        config.setTargetSmbUsername(trimToEmpty(request.targetSmbUsername()));
        config.setTargetSmbPassword(request.targetSmbPassword());
        config.setTargetSmbPath(trimToEmpty(request.targetSmbPath()));
        config.setTargetWebdavBaseUrl(trimToEmpty(request.targetWebdavBaseUrl()));
        config.setTargetWebdavUsername(trimToEmpty(request.targetWebdavUsername()));
        config.setTargetWebdavPassword(request.targetWebdavPassword());
        config.setTargetWebdavPath(trimToEmpty(request.targetWebdavPath()));
        config.setTargetHttpUrl(trimToEmpty(request.targetHttpUrl()));
        config.setTargetHttpMethod(trimToEmpty(request.targetHttpMethod()).isBlank() ? "POST" : trimToEmpty(request.targetHttpMethod()).toUpperCase());
        config.setTargetHttpUsername(trimToEmpty(request.targetHttpUsername()));
        config.setTargetHttpPassword(request.targetHttpPassword());
        config.setTargetHttpFileField(trimToEmpty(request.targetHttpFileField()).isBlank() ? "file" : trimToEmpty(request.targetHttpFileField()));
        config.setTargetHttpPathParam(trimToEmpty(request.targetHttpPathParam()).isBlank() ? "path" : trimToEmpty(request.targetHttpPathParam()));
        return toResponse(repository.save(config));
    }

    /**
     * 根据目标类型校验目标端配置。
     *
     * @param request 配置请求
     */
    public void validateTarget(SftpConfigRequest request) {
        if ("LOCAL".equals(request.targetType())) {
            requireText(request.syncPath(), "文件同步路径不能为空");
            return;
        }

        if ("S3".equals(request.targetType())) {
            requireText(request.targetS3Endpoint(), "S3 Endpoint不能为空");
            requireText(request.targetS3AccessKey(), "S3 Access Key不能为空");
            requireText(request.targetS3SecretKey(), "S3 Secret Key不能为空");
            requireText(request.targetS3Bucket(), "S3 Bucket不能为空");
            return;
        }

        if ("SMB".equals(request.targetType())) {
            requireText(request.targetSmbHost(), "SMB服务器地址不能为空");
            requireText(request.targetSmbShare(), "SMB共享名不能为空");
            requireText(request.targetSmbUsername(), "SMB用户名不能为空");
            requireText(request.targetSmbPassword(), "SMB密码不能为空");
            return;
        }

        if ("WEBDAV".equals(request.targetType())) {
            requireText(request.targetWebdavBaseUrl(), "WebDAV基础地址不能为空");
            return;
        }

        if ("HTTP".equals(request.targetType())) {
            requireText(request.targetHttpUrl(), "HTTP上传地址不能为空");
            return;
        }

        requireText(request.targetHost(), "目标SFTP服务器地址不能为空");
        if (request.targetPort() == null) {
            throw new IllegalArgumentException("目标SFTP服务器端口不能为空");
        }
        requireText(request.targetUsername(), "目标用户名不能为空");
        requireText(request.targetPassword(), "目标密码不能为空");
        requireText(request.targetPath(), "目标SFTP文件路径不能为空");
        if (!request.targetPath().trim().startsWith("/")) {
            throw new IllegalArgumentException("目标SFTP文件路径必须是以/开头的绝对路径");
        }
    }

    /**
     * 将实体转换为响应对象。
     *
     * @param config 配置实体
     * @return 配置响应
     */
    private SftpConfigResponse toResponse(SftpConfig config) {
        return new SftpConfigResponse(
                config.getHost(),
                config.getPort(),
                config.getUsername(),
                config.getPassword(),
                config.getSftpPath() == null ? "/" : config.getSftpPath(),
                config.getBandwidthLimitMbps(),
                config.getParallelCount() == null ? 1 : config.getParallelCount(),
                config.getTargetType() == null ? "LOCAL" : config.getTargetType(),
                config.getSyncPath() == null ? "" : config.getSyncPath(),
                config.getTargetHost() == null ? "" : config.getTargetHost(),
                config.getTargetPort() == null ? 22 : config.getTargetPort(),
                config.getTargetUsername() == null ? "" : config.getTargetUsername(),
                config.getTargetPassword() == null ? "" : config.getTargetPassword(),
                config.getTargetPath() == null ? "" : config.getTargetPath(),
                config.getTargetS3Endpoint() == null ? "" : config.getTargetS3Endpoint(),
                config.getTargetS3AccessKey() == null ? "" : config.getTargetS3AccessKey(),
                config.getTargetS3SecretKey() == null ? "" : config.getTargetS3SecretKey(),
                config.getTargetS3Bucket() == null ? "" : config.getTargetS3Bucket(),
                config.getTargetS3Prefix() == null ? "" : config.getTargetS3Prefix(),
                config.getTargetS3Region() == null ? "us-east-1" : config.getTargetS3Region(),
                config.getTargetS3PathStyleAccess() == null || config.getTargetS3PathStyleAccess(),
                config.getTargetSmbHost() == null ? "" : config.getTargetSmbHost(),
                config.getTargetSmbShare() == null ? "" : config.getTargetSmbShare(),
                config.getTargetSmbDomain() == null ? "" : config.getTargetSmbDomain(),
                config.getTargetSmbUsername() == null ? "" : config.getTargetSmbUsername(),
                config.getTargetSmbPassword() == null ? "" : config.getTargetSmbPassword(),
                config.getTargetSmbPath() == null ? "" : config.getTargetSmbPath(),
                config.getTargetWebdavBaseUrl() == null ? "" : config.getTargetWebdavBaseUrl(),
                config.getTargetWebdavUsername() == null ? "" : config.getTargetWebdavUsername(),
                config.getTargetWebdavPassword() == null ? "" : config.getTargetWebdavPassword(),
                config.getTargetWebdavPath() == null ? "" : config.getTargetWebdavPath(),
                config.getTargetHttpUrl() == null ? "" : config.getTargetHttpUrl(),
                config.getTargetHttpMethod() == null ? "POST" : config.getTargetHttpMethod(),
                config.getTargetHttpUsername() == null ? "" : config.getTargetHttpUsername(),
                config.getTargetHttpPassword() == null ? "" : config.getTargetHttpPassword(),
                config.getTargetHttpFileField() == null ? "file" : config.getTargetHttpFileField(),
                config.getTargetHttpPathParam() == null ? "path" : config.getTargetHttpPathParam()
        );
    }

    /**
     * 校验字符串是否有实际内容。
     *
     * @param value 待校验字符串
     * @param message 为空时抛出的错误消息
     */
    private void requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 将字符串转换为去除首尾空白后的非空字符串。
     *
     * @param value 原始字符串
     * @return 去除首尾空白后的字符串；原值为 null 时返回空字符串
     */
    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
