package com.rongzer.connector.bigfilesftp.service.sync;

import java.io.InputStream;

/**
 * 目标端文件操作接口。
 */
public interface TargetWriter {

    /**
     * 根据正式文件名生成临时文件名。
     */
    String temporaryFileName(String fileName);

    /**
     * 查询正式文件大小。
     */
    long finalSize(String fileName) throws Exception;

    /**
     * 查询临时文件大小。
     */
    long temporarySize(String fileName) throws Exception;

    /**
     * 将未完成的正式文件移动为临时文件。
     */
    void moveFinalToTemporary(String fileName) throws Exception;

    /**
     * 将完整临时文件提升为正式文件。
     */
    void promoteTemporaryToFinal(String fileName) throws Exception;

    /**
     * 将输入流内容追加或写入临时文件。
     */
    void appendToTemporary(String fileName, InputStream inputStream) throws Exception;

    /**
     * 是否支持基于临时文件大小的追加续传。
     */
    default boolean supportsAppendResume() {
        return true;
    }
}
