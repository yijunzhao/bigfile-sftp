package com.rongzer.connector.bigfilesftp.service.sync;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 带限速能力的输入流包装器。
 */
public class RateLimitedInputStream extends FilterInputStream {

    private final BandwidthLimiter bandwidthLimiter;

    public RateLimitedInputStream(InputStream inputStream, BandwidthLimiter bandwidthLimiter) {
        super(inputStream);
        this.bandwidthLimiter = bandwidthLimiter;
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value != -1) {
            bandwidthLimiter.acquire(1);
        }
        return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int count = super.read(buffer, offset, length);
        if (count > 0) {
            bandwidthLimiter.acquire(count);
        }
        return count;
    }
}
