package com.rongzer.connector.bigfilesftp.service.sync;

import java.util.List;

/**
 * 源 SFTP 目录扫描结果。
 *
 * @param entries 页面展示用扫描明细
 * @param fileCount 文件数量
 * @param totalBytes 文件总字节数
 * @param homeDirectory 源 SFTP 登录默认目录
 * @param camelDirectory Apache Camel 可消费的相对目录
 * @param files 待同步源文件列表
 */
public record RemoteDirectoryScan(List<String> entries, int fileCount, long totalBytes, String homeDirectory, String camelDirectory, List<SourceFile> files) {
}
