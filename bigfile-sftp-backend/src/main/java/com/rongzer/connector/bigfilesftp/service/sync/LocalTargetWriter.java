package com.rongzer.connector.bigfilesftp.service.sync;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * 本地文件系统目标端操作器。
 */
public class LocalTargetWriter implements TargetWriter {

    private final Path rootDirectory;

    public LocalTargetWriter(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public String temporaryFileName(String fileName) {
        return fileName + ".part";
    }

    @Override
    public long finalSize(String fileName) throws IOException {
        return size(resolve(fileName));
    }

    @Override
    public long temporarySize(String fileName) throws IOException {
        return size(resolve(temporaryFileName(fileName)));
    }

    @Override
    public void moveFinalToTemporary(String fileName) throws IOException {
        Path finalPath = resolve(fileName);
        Path temporaryPath = resolve(temporaryFileName(fileName));
        Files.createDirectories(temporaryPath.getParent());
        Files.move(finalPath, temporaryPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void promoteTemporaryToFinal(String fileName) throws IOException {
        Path finalPath = resolve(fileName);
        Path temporaryPath = resolve(temporaryFileName(fileName));
        Files.createDirectories(finalPath.getParent());
        Files.move(temporaryPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void appendToTemporary(String fileName, InputStream inputStream) throws IOException {
        Path temporaryPath = resolve(temporaryFileName(fileName));
        Files.createDirectories(temporaryPath.getParent());
        try (var outputStream = Files.newOutputStream(temporaryPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            inputStream.transferTo(outputStream);
        }
    }

    private long size(Path path) throws IOException {
        return Files.exists(path) ? Files.size(path) : 0L;
    }

    private Path resolve(String fileName) {
        Path resolvedPath = rootDirectory.resolve(fileName).normalize();
        if (!resolvedPath.startsWith(rootDirectory)) {
            throw new IllegalArgumentException("非法目标文件路径: " + fileName);
        }
        return resolvedPath;
    }
}
