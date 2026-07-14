package com.rongzer.connector.bigfilesftp.controller;

import com.rongzer.connector.bigfilesftp.dto.SftpConfigRequest;
import com.rongzer.connector.bigfilesftp.dto.SftpConfigResponse;
import com.rongzer.connector.bigfilesftp.dto.SyncResultResponse;
import com.rongzer.connector.bigfilesftp.service.SftpConfigService;
import com.rongzer.connector.bigfilesftp.service.SftpSyncService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sftp-config")
/**
 * SFTP 配置与同步接口。
 */
public class SftpConfigController {

    private final SftpConfigService configService;
    private final SftpSyncService syncService;

    public SftpConfigController(SftpConfigService configService, SftpSyncService syncService) {
        this.configService = configService;
        this.syncService = syncService;
    }

    /**
     * 查询当前页面保存的 SFTP 同步配置。
     *
     * @return SFTP 同步配置
     */
    @GetMapping
    public SftpConfigResponse getConfig() {
        return configService.getConfig();
    }

    /**
     * 保存页面配置。
     *
     * @param request SFTP 同步配置请求
     * @return 保存后的配置
     */
    @PostMapping
    public SftpConfigResponse saveConfig(@Valid @RequestBody SftpConfigRequest request) {
        return configService.saveConfig(request);
    }

    /**
     * 保存配置并发起一次文件同步。
     *
     * @param request SFTP 同步配置请求
     * @return 同步结果
     * @throws Exception 同步过程中发生的异常
     */
    @PostMapping("/sync")
    public SyncResultResponse sync(@Valid @RequestBody SftpConfigRequest request) throws Exception {
        configService.saveConfig(request);
        return syncService.sync(request);
    }
}
