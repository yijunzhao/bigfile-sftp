package com.rongzer.connector.bigfilesftp.service.sync;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 限制最多读取指定字节数的输入流包装器。
 */
public class BoundedInputStream extends FilterInputStream {

    private long remainingBytes;

    public BoundedInputStream(InputStream inputStream, long limit) {
        super(inputStream);
        this.remainingBytes = limit;
    }

    @Override
    public int read() throws IOException {
        if (remainingBytes <= 0) {
            return -1;
        }
        int value = super.read();
        if (value != -1) {
            remainingBytes--;
        }
        return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (remainingBytes <= 0) {
            return -1;
        }
        int readLength = (int) Math.min(length, remainingBytes);
        int count = super.read(buffer, offset, readLength);
        if (count > 0) {
            remainingBytes -= count;
        }
        return count;
    }
}
