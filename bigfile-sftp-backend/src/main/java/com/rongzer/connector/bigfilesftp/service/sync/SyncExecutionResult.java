package com.rongzer.connector.bigfilesftp.service.sync;

import java.util.List;

/**
 * 一次同步任务的执行结果。
 *
 * @param files 成功同步或跳过的文件列表
 * @param failedFiles 失败文件列表
 */
public record SyncExecutionResult(List<String> files, List<String> failedFiles) {
}
