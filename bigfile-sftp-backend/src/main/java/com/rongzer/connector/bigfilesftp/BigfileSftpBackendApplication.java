package com.rongzer.connector.bigfilesftp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
/**
 * Bigfile SFTP 后端启动入口。
 *
 * <p>启动前会根据当前工作目录自动设置 H2 文件数据库路径，确保默认落到后端模块的 db 目录。</p>
 */
public class BigfileSftpBackendApplication {

    private static final String DB_PATH_PROPERTY = "BIGFILE_SFTP_DB_PATH";
    private static final String BACKEND_MODULE_NAME = "bigfile-sftp-backend";

    /**
     * 应用主入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        configureDatabasePath();
        SpringApplication.run(BigfileSftpBackendApplication.class, args);
    }

    /**
     * 配置 H2 数据库默认文件路径。
     */
    private static void configureDatabasePath() {
        if (System.getProperty(DB_PATH_PROPERTY) != null || System.getenv(DB_PATH_PROPERTY) != null) {
            return;
        }

        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path backendDirectory = workingDirectory.getFileName().toString().equals(BACKEND_MODULE_NAME)
                ? workingDirectory
                : workingDirectory.resolve(BACKEND_MODULE_NAME);
        if (!Files.isDirectory(backendDirectory)) {
            backendDirectory = workingDirectory;
        }

        System.setProperty(DB_PATH_PROPERTY, backendDirectory.resolve("db").resolve("bigfile-sftp").toString());
    }
}
