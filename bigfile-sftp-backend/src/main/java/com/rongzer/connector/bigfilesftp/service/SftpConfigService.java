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
                .orElse(new SftpConfigResponse("", 22, "", "", "", null, 1, "LOCAL", "", "", 22, "", "", ""));
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
                config.getTargetPath() == null ? "" : config.getTargetPath()
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
