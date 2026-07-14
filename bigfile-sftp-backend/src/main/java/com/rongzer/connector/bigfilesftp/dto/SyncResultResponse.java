package com.rongzer.connector.bigfilesftp.dto;

import java.util.List;

/**
 * 文件同步结果响应。
 *
 * @param transferredCount 已完成或跳过的文件数量
 * @param targetPath 目标路径
 * @param sourceUri 脱敏后的源 Camel URI
 * @param remotePath 源 SFTP 绝对路径
 * @param bandwidthLimit 本次同步使用的限速描述
 * @param remoteScan 远端目录扫描结果
 * @param files 已同步、已跳过或已提升完成的文件列表
 * @param failedFiles 同步失败的文件列表
 * @param message 同步结果消息
 */
public record SyncResultResponse(
        int transferredCount,
        String targetPath,
        String sourceUri,
        String remotePath,
        String bandwidthLimit,
        List<String> remoteScan,
        List<String> files,
        List<String> failedFiles,
        String message
) {
}
