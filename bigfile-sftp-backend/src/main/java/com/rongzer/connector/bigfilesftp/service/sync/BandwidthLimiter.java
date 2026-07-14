package com.rongzer.connector.bigfilesftp.service.sync;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 多文件共享的全局带宽限速器。
 */
public class BandwidthLimiter {

    private static final double NANOS_PER_SECOND = 1_000_000_000D;

    private final long bytesPerSecond;
    private final long startNanos;
    private long totalBytes;

    public BandwidthLimiter(long bytesPerSecond) {
        this.bytesPerSecond = bytesPerSecond;
        this.startNanos = System.nanoTime();
    }

    /**
     * 判断是否启用限速。
     */
    public boolean enabled() {
        return bytesPerSecond > 0;
    }

    /**
     * 获取限速值，单位 B/s。
     */
    public long bytesPerSecond() {
        return bytesPerSecond;
    }

    /**
     * 按已传输字节数计算需要等待的时间。
     */
    public synchronized void acquire(long count) throws IOException {
        if (!enabled() || count <= 0) {
            return;
        }
        totalBytes += count;
        long expectedNanos = (long) (totalBytes * NANOS_PER_SECOND / bytesPerSecond);
        long actualNanos = System.nanoTime() - startNanos;
        long sleepNanos = expectedNanos - actualNanos;
        if (sleepNanos <= 0) {
            return;
        }
        try {
            TimeUnit.NANOSECONDS.sleep(sleepNanos);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("传输限速等待被中断", exception);
        }
    }
}
