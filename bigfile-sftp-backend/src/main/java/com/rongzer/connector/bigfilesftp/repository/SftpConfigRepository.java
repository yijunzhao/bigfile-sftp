package com.rongzer.connector.bigfilesftp.repository;

import com.rongzer.connector.bigfilesftp.entity.SftpConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * SFTP 配置数据访问接口。
 */
public interface SftpConfigRepository extends JpaRepository<SftpConfig, Long> {
}
