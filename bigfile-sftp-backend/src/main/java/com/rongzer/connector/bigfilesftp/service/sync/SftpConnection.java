package com.rongzer.connector.bigfilesftp.service.sync;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;

/**
 * SFTP 连接封装。
 *
 * @param session JSch 会话
 * @param channel SFTP 通道
 */
public record SftpConnection(Session session, ChannelSftp channel) implements AutoCloseable {

    @Override
    public void close() {
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
