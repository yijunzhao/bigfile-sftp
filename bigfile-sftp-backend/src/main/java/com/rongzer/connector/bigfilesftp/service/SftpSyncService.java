package com.rongzer.connector.bigfilesftp.service;

import com.rongzer.connector.bigfilesftp.dto.SftpConfigRequest;
import com.rongzer.connector.bigfilesftp.dto.SyncResultResponse;
import com.rongzer.connector.bigfilesftp.entity.S3MultipartPart;
import com.rongzer.connector.bigfilesftp.entity.S3MultipartUpload;
import com.rongzer.connector.bigfilesftp.repository.S3MultipartPartRepository;
import com.rongzer.connector.bigfilesftp.repository.S3MultipartUploadRepository;
import com.rongzer.connector.bigfilesftp.service.sync.*;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

import java.net.URI;

@Service
/**
 * SFTP 文件同步服务。
 *
 * <p>负责源目录扫描、文件级并发任务调度、全局限速、临时文件写入、断点续传和同步完成后的重命名。</p>
 */
public class SftpSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SftpSyncService.class);
    private static final long ROUTE_IDLE_TIMEOUT_MILLIS = 30_000L;
    private static final long MIN_ROUTE_COMPLETION_TIMEOUT_MILLIS = 7_200_000L;
    private static final long MAX_ROUTE_COMPLETION_TIMEOUT_MILLIS = 86_400_000L;
    private static final long ROUTE_TIMEOUT_BUFFER_MILLIS = 1_800_000L;
    private static final long DEFAULT_TIMEOUT_BYTES_PER_SECOND = 1024L * 1024L;
    private static final double ROUTE_TIMEOUT_SAFETY_FACTOR = 3D;

    private final CamelContext camelContext;
    private final S3MultipartUploadRepository s3MultipartUploadRepository;
    private final S3MultipartPartRepository s3MultipartPartRepository;

    public SftpSyncService(CamelContext camelContext, S3MultipartUploadRepository s3MultipartUploadRepository, S3MultipartPartRepository s3MultipartPartRepository) {
        this.camelContext = camelContext;
        this.s3MultipartUploadRepository = s3MultipartUploadRepository;
        this.s3MultipartPartRepository = s3MultipartPartRepository;
    }

    /**
     * 执行一次文件同步。
     *
     * @param request 页面提交的同步配置
     * @return 同步结果
     * @throws Exception 同步过程中发生的异常
     */
    public SyncResultResponse sync(SftpConfigRequest request) throws Exception {
        RemoteDirectoryScan remoteScan = scanRemoteDirectory(request);
        long bandwidthLimitBytesPerSecond = toBytesPerSecond(request.bandwidthLimitMbps());
        BandwidthLimiter bandwidthLimiter = new BandwidthLimiter(bandwidthLimitBytesPerSecond);
        String bandwidthLimit = formatBandwidthLimit(request.bandwidthLimitMbps(), bandwidthLimitBytesPerSecond);
        SyncTarget syncTarget = buildTarget(request);
        LOGGER.info("Camel sync target URI: {}", maskPassword(syncTarget.uri()));
        LOGGER.info("SFTP login home={}, absolute path={}, camel directory={}", remoteScan.homeDirectory(), request.sftpPath().trim(), remoteScan.camelDirectory());
        LOGGER.info("SFTP remote directory scan path={}, result={}", request.sftpPath().trim(), remoteScan.entries());
        LOGGER.info("SFTP bandwidth limit: {}", bandwidthLimit);

        SyncExecutionResult syncExecutionResult = runSyncTasks(request, remoteScan, syncTarget, bandwidthLimiter, resolveParallelCount(request.parallelCount()));

        return new SyncResultResponse(
                syncExecutionResult.files().size(),
                syncTarget.displayPath(),
                maskPassword(buildSftpUri(request, remoteScan.camelDirectory(), "<按文件动态指定>")),
                request.sftpPath().trim(),
                bandwidthLimit,
                remoteScan.entries(),
                syncExecutionResult.files(),
                syncExecutionResult.failedFiles(),
                buildMessage(remoteScan.fileCount(), syncExecutionResult.files().size(), syncExecutionResult.failedFiles().size())
        );
    }

    /**
     * 根据扫描到的文件列表创建文件级同步任务，并按配置并发执行。
     *
     * @param request 同步配置
     * @param remoteScan 源 SFTP 扫描结果
     * @param syncTarget 同步目标信息
     * @param bandwidthLimiter 全局限速器
     * @param parallelCount 并发文件数
     * @return 文件同步执行结果
     * @throws Exception 任务执行异常
     */
    private SyncExecutionResult runSyncTasks(SftpConfigRequest request, RemoteDirectoryScan remoteScan, SyncTarget syncTarget, BandwidthLimiter bandwidthLimiter, int parallelCount) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(parallelCount);
        List<String> files = Collections.synchronizedList(new ArrayList<>());
        List<String> failedFiles = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> futures = new ArrayList<>();

        for (SourceFile sourceFile : remoteScan.files()) {
            if (syncTarget.writer() != null && skipOrPromoteCompletedFile(syncTarget.writer(), sourceFile, files)) {
                continue;
            }
            futures.add(executorService.submit(() -> {
                try {
                    if (syncTarget.s3Target() != null) {
                        syncS3MultipartFile(request, syncTarget.s3Target(), sourceFile, bandwidthLimiter);
                        files.add(sourceFile.relativePath());
                        return;
                    }
                    if (resumeDirectlyFromSourceOffset(request, syncTarget.writer(), sourceFile, bandwidthLimiter)) {
                        files.add(sourceFile.relativePath());
                        return;
                    }
                    String sourceUri = buildSftpUri(request, remoteScan.camelDirectory(), sourceFile.relativePath());
                    long timeoutMillis = resolveFileTimeoutMillis(sourceFile.size(), bandwidthLimiter.bytesPerSecond());
                    LOGGER.info("Camel SFTP source URI: {}, timeout={} ms", maskPassword(sourceUri), timeoutMillis);
                    List<String> syncedFiles = runSyncRoute(sourceUri, syncTarget, bandwidthLimiter, timeoutMillis);
                    if (syncedFiles.isEmpty() && promoteTemporaryIfComplete(syncTarget.writer(), sourceFile)) {
                        files.add(sourceFile.relativePath());
                    } else {
                        files.addAll(syncedFiles);
                    }
                } catch (Exception exception) {
                    try {
                        if (promoteTemporaryIfComplete(syncTarget.writer(), sourceFile)) {
                            files.add(sourceFile.relativePath());
                            return;
                        }
                    } catch (Exception promoteException) {
                        LOGGER.warn("SFTP completed part promote failed: file={}, error={}", sourceFile.relativePath(), promoteException.getMessage());
                    }
                    String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
                    LOGGER.warn("SFTP file sync failed: file={}, error={}", sourceFile.relativePath(), message);
                    failedFiles.add(sourceFile.relativePath() + ": " + message);
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }
        executorService.shutdown();
        return new SyncExecutionResult(List.copyOf(files), List.copyOf(failedFiles));
    }

    /**
     * 在任务入队前处理已完成文件或完整临时文件。
     *
     * @param targetWriter 目标端文件操作器
     * @param sourceFile 源文件信息
     * @param files 已完成文件列表
     * @return true 表示文件无需进入同步队列
     * @throws Exception 文件检查或重命名异常
     */
    private boolean skipOrPromoteCompletedFile(TargetWriter targetWriter, SourceFile sourceFile, List<String> files) throws Exception {
        long finalSize = targetWriter.finalSize(sourceFile.relativePath());
        if (sourceFile.size() > 0 && finalSize == sourceFile.size()) {
            files.add(sourceFile.relativePath() + "（已存在，跳过）");
            return true;
        }
        if (promoteTemporaryIfComplete(targetWriter, sourceFile)) {
            files.add(sourceFile.relativePath());
            return true;
        }
        return false;
    }

    /**
     * 如果临时文件已经完整，则提升为正式文件。
     *
     * @param targetWriter 目标端文件操作器
     * @param sourceFile 源文件信息
     * @return true 表示已成功提升为正式文件
     * @throws Exception 重命名异常
     */
    private boolean promoteTemporaryIfComplete(TargetWriter targetWriter, SourceFile sourceFile) throws Exception {
        if (sourceFile.size() <= 0) {
            return false;
        }
        long temporarySize = targetWriter.temporarySize(sourceFile.relativePath());
        LOGGER.info("Check temporary file: file={}, temporarySize={}, sourceSize={}", sourceFile.relativePath(), temporarySize, sourceFile.size());
        if (temporarySize < sourceFile.size()) {
            return false;
        }
        targetWriter.promoteTemporaryToFinal(sourceFile.relativePath());
        LOGGER.info("Promoted completed temporary file: file={}, temporarySize={}, sourceSize={}", sourceFile.relativePath(), temporarySize, sourceFile.size());
        return true;
    }

    /**
     * 对已有临时文件执行源端 offset 续传。
     *
     * @param request 同步配置
     * @param targetWriter 目标端文件操作器
     * @param sourceFile 源文件信息
     * @param bandwidthLimiter 全局限速器
     * @return true 表示已通过 offset 续传完成该文件
     * @throws Exception 续传异常
     */
    private boolean resumeDirectlyFromSourceOffset(SftpConfigRequest request, TargetWriter targetWriter, SourceFile sourceFile, BandwidthLimiter bandwidthLimiter) throws Exception {
        if (!targetWriter.supportsAppendResume()) {
            return false;
        }
        if (sourceFile.size() <= 0) {
            return false;
        }
        long temporarySize = targetWriter.temporarySize(sourceFile.relativePath());
        long finalSize = targetWriter.finalSize(sourceFile.relativePath());
        if (finalSize > 0 && finalSize < sourceFile.size() && temporarySize == 0) {
            targetWriter.moveFinalToTemporary(sourceFile.relativePath());
            temporarySize = finalSize;
        }
        if (temporarySize <= 0 || temporarySize >= sourceFile.size()) {
            return false;
        }

        String sourceFilePath = request.sftpPath().trim().replaceAll("/+$", "") + "/" + sourceFile.relativePath();
        LOGGER.info("Resume file from source offset: file={}, offset={}, sourceSize={}", sourceFile.relativePath(), temporarySize, sourceFile.size());
        try (SftpConnection sourceConnection = connectSftp(request.host().trim(), request.port(), request.username().trim(), request.password());
             InputStream sourceInputStream = sourceConnection.channel().get(sourceFilePath, null, temporarySize);
             InputStream transferInputStream = bandwidthLimiter.enabled() ? new RateLimitedInputStream(sourceInputStream, bandwidthLimiter) : sourceInputStream) {
            targetWriter.appendToTemporary(sourceFile.relativePath(), transferInputStream);
        }

        return promoteTemporaryIfComplete(targetWriter, sourceFile);
    }

    /**
     * 将单个源 SFTP 文件同步到 S3/MinIO，使用 Multipart Upload 支持断点续传。
     */
    private void syncS3MultipartFile(SftpConfigRequest request, S3Target s3Target, SourceFile sourceFile, BandwidthLimiter bandwidthLimiter) throws Exception {
        String objectKey = s3Target.prefix() + sourceFile.relativePath();
        if (s3ObjectSize(s3Target, objectKey) == sourceFile.size()) {
            LOGGER.info("S3 object already exists, skip: bucket={}, key={}", s3Target.bucket(), objectKey);
            return;
        }

        long partSize = resolveS3PartSize(sourceFile.size());
        S3MultipartUpload upload = findOrCreateS3Upload(request, s3Target, sourceFile, objectKey, partSize);
        List<S3MultipartPart> uploadedParts = s3MultipartPartRepository.findByUploadIdOrderByPartNumberAsc(upload.getUploadId());
        List<Integer> completedPartNumbers = uploadedParts.stream().map(S3MultipartPart::getPartNumber).toList();
        int totalParts = (int) Math.ceil(sourceFile.size() * 1D / partSize);

        try (S3Client s3Client = createS3Client(s3Target);
             SftpConnection sourceConnection = connectSftp(request.host().trim(), request.port(), request.username().trim(), request.password())) {
            for (int partNumber = 1; partNumber <= totalParts; partNumber++) {
                if (completedPartNumbers.contains(partNumber)) {
                    continue;
                }
                long offset = (long) (partNumber - 1) * partSize;
                long currentPartSize = Math.min(partSize, sourceFile.size() - offset);
                uploadS3Part(request, s3Target, sourceFile, s3Client, sourceConnection.channel(), upload.getUploadId(), objectKey, partNumber, offset, currentPartSize, bandwidthLimiter);
            }

            List<CompletedPart> completedParts = s3MultipartPartRepository.findByUploadIdOrderByPartNumberAsc(upload.getUploadId()).stream()
                    .map(part -> CompletedPart.builder().partNumber(part.getPartNumber()).eTag(part.geteTag()).build())
                    .toList();
            s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(s3Target.bucket())
                    .key(objectKey)
                    .uploadId(upload.getUploadId())
                    .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                    .build());
            upload.setStatus("COMPLETED");
            s3MultipartUploadRepository.save(upload);
            LOGGER.info("S3 multipart upload completed: bucket={}, key={}", s3Target.bucket(), objectKey);
        }
    }

    /**
     * 上传一个 S3 Multipart 分片。
     */
    private void uploadS3Part(SftpConfigRequest request, S3Target s3Target, SourceFile sourceFile, S3Client s3Client, ChannelSftp sourceChannel, String uploadId, String objectKey, int partNumber, long offset, long size, BandwidthLimiter bandwidthLimiter) throws Exception {
        String sourceFilePath = request.sftpPath().trim().replaceAll("/+$", "") + "/" + sourceFile.relativePath();
        try (InputStream sourceInputStream = sourceChannel.get(sourceFilePath, null, offset);
             InputStream limitedInputStream = bandwidthLimiter.enabled() ? new RateLimitedInputStream(sourceInputStream, bandwidthLimiter) : sourceInputStream;
             InputStream boundedInputStream = new BoundedInputStream(limitedInputStream, size)) {
            String eTag = s3Client.uploadPart(UploadPartRequest.builder()
                            .bucket(s3Target.bucket())
                            .key(objectKey)
                            .uploadId(uploadId)
                            .partNumber(partNumber)
                            .contentLength(size)
                            .build(),
                    RequestBody.fromInputStream(boundedInputStream, size)).eTag();
            S3MultipartPart part = new S3MultipartPart();
            part.setUploadId(uploadId);
            part.setPartNumber(partNumber);
            part.setOffset(offset);
            part.setSize(size);
            part.seteTag(eTag);
            s3MultipartPartRepository.save(part);
            LOGGER.info("S3 multipart part uploaded: file={}, partNumber={}, size={}", sourceFile.relativePath(), partNumber, size);
        }
    }

    /**
     * 查找未完成上传任务，不存在时创建新的 Multipart Upload。
     */
    private S3MultipartUpload findOrCreateS3Upload(SftpConfigRequest request, S3Target s3Target, SourceFile sourceFile, String objectKey, long partSize) {
        String sourcePath = request.sftpPath().trim().replaceAll("/+$", "") + "/" + sourceFile.relativePath();
        return s3MultipartUploadRepository.findFirstBySourcePathAndBucketAndObjectKeyAndFileSizeAndStatusOrderByIdDesc(sourcePath, s3Target.bucket(), objectKey, sourceFile.size(), "UPLOADING")
                .orElseGet(() -> {
                    try (S3Client s3Client = createS3Client(s3Target)) {
                        String uploadId = s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                                .bucket(s3Target.bucket())
                                .key(objectKey)
                                .build()).uploadId();
                        S3MultipartUpload upload = new S3MultipartUpload();
                        upload.setSourcePath(sourcePath);
                        upload.setBucket(s3Target.bucket());
                        upload.setObjectKey(objectKey);
                        upload.setUploadId(uploadId);
                        upload.setFileSize(sourceFile.size());
                        upload.setPartSize(partSize);
                        upload.setStatus("UPLOADING");
                        return s3MultipartUploadRepository.save(upload);
                    }
                });
    }

    /**
     * 创建 S3 客户端。
     */
    private S3Client createS3Client(S3Target s3Target) {
        return S3Client.builder()
                .endpointOverride(URI.create(s3Target.endpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(s3Target.accessKey(), s3Target.secretKey())))
                .region(Region.of(s3Target.region()))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(s3Target.pathStyleAccess()).build())
                .build();
    }

    /**
     * 查询 S3 对象大小，不存在时返回 -1。
     */
    private long s3ObjectSize(S3Target s3Target, String objectKey) {
        try (S3Client s3Client = createS3Client(s3Target)) {
            return s3Client.headObject(HeadObjectRequest.builder().bucket(s3Target.bucket()).key(objectKey).build()).contentLength();
        } catch (NoSuchKeyException exception) {
            return -1L;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return -1L;
            }
            throw exception;
        }
    }

    /**
     * 根据文件大小选择 S3 分片大小。
     */
    private long resolveS3PartSize(long fileSize) {
        long defaultPartSize = 64L * 1024L * 1024L;
        long minPartSizeForPartLimit = (long) Math.ceil(fileSize / 10_000D);
        long partSize = Math.max(defaultPartSize, minPartSizeForPartLimit);
        long remainder = partSize % (1024L * 1024L);
        return remainder == 0 ? partSize : partSize + (1024L * 1024L - remainder);
    }

    /**
     * 为单个文件创建临时 Camel Route 并执行同步。
     *
     * @param sourceUri 源 SFTP Camel URI
     * @param syncTarget 同步目标
     * @param bandwidthLimiter 全局限速器
     * @param timeoutMillis 单文件等待超时时间
     * @return 当前 Route 完成的文件列表
     * @throws Exception Route 创建、执行或停止异常
     */
    private List<String> runSyncRoute(String sourceUri, SyncTarget syncTarget, BandwidthLimiter bandwidthLimiter, long timeoutMillis) throws Exception {
        String routeId = "sftp-sync-" + UUID.randomUUID();
        List<String> files = Collections.synchronizedList(new ArrayList<>());

        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from(sourceUri)
                        .routeId(routeId)
                        .noStreamCaching()
                        .autoStartup(false)
                        .process(exchange -> prepareResumableTransfer(exchange, syncTarget.writer(), bandwidthLimiter, files))
                        .process(exchange -> writeExchangeToTarget(exchange, syncTarget.writer()))
                        .process(exchange -> completeResumableTransfer(exchange, syncTarget.writer(), files));
            }
        });

        NotifyBuilder notifyBuilder = new NotifyBuilder(camelContext)
                .fromRoute(routeId)
                .whenDone(1)
                .create();

        try {
            camelContext.getRouteController().startRoute(routeId);
            boolean completed = notifyBuilder.matches(timeoutMillis, TimeUnit.MILLISECONDS);
            if (!completed) {
                LOGGER.warn("Camel SFTP sync route timeout: routeId={}, transferredFiles={}", routeId, files);
            }
            return List.copyOf(files);
        } finally {
            camelContext.getRouteController().stopRoute(routeId, 10, TimeUnit.SECONDS);
            camelContext.removeRoute(routeId);
        }
    }

    /**
     * 将 Camel 获取到的源文件流写入目标端临时文件。
     *
     * @param exchange 当前 Camel 交换对象
     * @param targetWriter 目标端文件操作器
     * @throws Exception 读取或写入异常
     */
    private void writeExchangeToTarget(Exchange exchange, TargetWriter targetWriter) throws Exception {
        String fileName = exchange.getProperty("finalFileName", String.class);
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        InputStream inputStream = exchange.getMessage().getBody(InputStream.class);
        if (inputStream == null) {
            throw new IOException("源文件流为空: " + fileName);
        }
        targetWriter.appendToTemporary(fileName, inputStream);
    }

    /**
     * 构建同步结果消息。
     */
    private String buildMessage(int remoteFileCount, int transferredCount, int failedCount) {
        if (failedCount > 0) {
            return "文件同步完成，存在失败文件";
        }
        if (transferredCount > 0) {
            return "文件同步完成";
        }
        if (remoteFileCount > 0) {
            return "远端目录扫描到文件，但Camel未完成同步，请查看后端日志";
        }
        return "未发现需要同步的文件";
    }

    /**
     * 根据文件大小和限速估算单文件同步超时时间。
     */
    private long resolveFileTimeoutMillis(long fileSize, long bandwidthLimitBytesPerSecond) {
        if (fileSize <= 0) {
            return ROUTE_IDLE_TIMEOUT_MILLIS;
        }
        long bytesPerSecond = bandwidthLimitBytesPerSecond > 0 ? bandwidthLimitBytesPerSecond : DEFAULT_TIMEOUT_BYTES_PER_SECOND;
        long estimatedTransferMillis = (long) Math.ceil(fileSize * 1000D / bytesPerSecond * ROUTE_TIMEOUT_SAFETY_FACTOR);
        long timeoutMillis = estimatedTransferMillis + ROUTE_TIMEOUT_BUFFER_MILLIS;
        long boundedTimeoutMillis = Math.max(MIN_ROUTE_COMPLETION_TIMEOUT_MILLIS, timeoutMillis);
        return Math.min(MAX_ROUTE_COMPLETION_TIMEOUT_MILLIS, boundedTimeoutMillis);
    }

    /**
     * 构建单文件源 SFTP Camel URI。
     */
    private String buildSftpUri(SftpConfigRequest request, String camelDirectory, String fileName) {
        String parentDirectory = parentDirectory(fileName);
        String baseFileName = baseFileName(fileName);
        String fileDirectory = parentDirectory.isBlank() ? camelDirectory : camelDirectory + "/" + parentDirectory;
        return "sftp://" + request.host().trim() + ":" + request.port() + "/" + encodePath(fileDirectory)
                + "?username=" + encode(request.username().trim())
                + "&password=" + encode(request.password())
                + "&binary=true"
                + "&recursive=false"
                + "&noop=true"
                + "&idempotent=false"
                + "&maxMessagesPerPoll=1"
                + "&eagerMaxMessagesPerPoll=false"
                + "&include=" + encode(Pattern.quote(baseFileName))
                + "&disconnect=true"
                + "&stepwise=false"
                + "&streamDownload=true"
                + "&initialDelay=0"
                + "&delay=500"
                + "&throwExceptionOnConnectFailed=true";
    }

    /**
     * 将页面配置的并发数限制在允许范围内。
     */
    private int resolveParallelCount(Integer parallelCount) {
        if (parallelCount == null) {
            return 1;
        }
        return Math.max(1, Math.min(16, parallelCount));
    }

    /**
     * 获取相对文件路径的父目录。
     */
    private String parentDirectory(String fileName) {
        int lastSeparator = fileName.lastIndexOf('/');
        return lastSeparator < 0 ? "" : fileName.substring(0, lastSeparator);
    }

    /**
     * 获取相对文件路径的文件名部分。
     */
    private String baseFileName(String fileName) {
        int lastSeparator = fileName.lastIndexOf('/');
        return lastSeparator < 0 ? fileName : fileName.substring(lastSeparator + 1);
    }

    /**
     * 构建本地文件目标 URI。
     */
    private String buildFileUri(Path targetDirectory) {
        return targetDirectory.toUri() + "?autoCreate=true&fileExist=Append";
    }

    /**
     * 根据目标类型构建同步目标。
     */
    private SyncTarget buildTarget(SftpConfigRequest request) throws Exception {
        if ("SFTP".equals(request.targetType())) {
            String homeDirectory = getSftpHomeDirectory(
                    request.targetHost().trim(),
                    request.targetPort(),
                    request.targetUsername().trim(),
                    request.targetPassword()
            );
            String camelDirectory = toCamelDirectory(homeDirectory, request.targetPath().trim());
            LOGGER.info("Target SFTP login home={}, absolute path={}, camel directory={}", homeDirectory, request.targetPath().trim(), camelDirectory);
            return new SyncTarget(
                    buildTargetSftpUri(request, camelDirectory),
                    request.targetPath().trim(),
                    new SftpTargetWriter(request.targetHost().trim(), request.targetPort(), request.targetUsername().trim(), request.targetPassword(), request.targetPath().trim()),
                    null
            );
        }

        if ("S3".equals(request.targetType())) {
            S3Target s3Target = new S3Target(
                    request.targetS3Endpoint().trim(),
                    request.targetS3AccessKey().trim(),
                    request.targetS3SecretKey(),
                    request.targetS3Bucket().trim(),
                    normalizeS3Prefix(request.targetS3Prefix()),
                    request.targetS3Region() == null || request.targetS3Region().isBlank() ? "us-east-1" : request.targetS3Region().trim(),
                    request.targetS3PathStyleAccess() == null || request.targetS3PathStyleAccess()
            );
            return new SyncTarget("s3://" + s3Target.bucket() + "/" + s3Target.prefix(), s3Target.bucket() + "/" + s3Target.prefix(), null, s3Target);
        }

        if ("SMB".equals(request.targetType())) {
            String rootPath = normalizeRemoteRelativePath(request.targetSmbPath());
            String displayPath = "smb://" + request.targetSmbHost().trim() + "/" + request.targetSmbShare().trim() + (rootPath.isBlank() ? "" : "/" + rootPath);
            return new SyncTarget(
                    displayPath,
                    displayPath,
                    new SmbTargetWriter(
                            request.targetSmbHost().trim(),
                            request.targetSmbShare().trim(),
                            trimToEmpty(request.targetSmbDomain()),
                            request.targetSmbUsername().trim(),
                            request.targetSmbPassword(),
                            rootPath
                    ),
                    null
            );
        }

        if ("WEBDAV".equals(request.targetType())) {
            WebdavTargetWriter writer = new WebdavTargetWriter(
                    request.targetWebdavBaseUrl().trim(),
                    trimToEmpty(request.targetWebdavUsername()),
                    request.targetWebdavPassword(),
                    normalizeRemoteRelativePath(request.targetWebdavPath())
            );
            return new SyncTarget(writer.displayUri(), writer.displayUri(), writer, null);
        }

        if ("HTTP".equals(request.targetType())) {
            HttpUploadTargetWriter writer = new HttpUploadTargetWriter(
                    request.targetHttpUrl().trim(),
                    trimToEmpty(request.targetHttpMethod()).isBlank() ? "POST" : request.targetHttpMethod().trim().toUpperCase(),
                    trimToEmpty(request.targetHttpUsername()),
                    request.targetHttpPassword(),
                    trimToEmpty(request.targetHttpFileField()).isBlank() ? "file" : request.targetHttpFileField().trim(),
                    trimToEmpty(request.targetHttpPathParam()).isBlank() ? "path" : request.targetHttpPathParam().trim()
            );
            return new SyncTarget(writer.displayUri(), writer.displayUri(), writer, null);
        }

        Path targetDirectory = Path.of(request.syncPath().trim()).toAbsolutePath().normalize();
        Files.createDirectories(targetDirectory);
        return new SyncTarget(buildFileUri(targetDirectory), targetDirectory.toString(), new LocalTargetWriter(targetDirectory), null);
    }

    /**
     * 将可选远程目录规范化为不带开头斜杠的相对路径。
     */
    private String normalizeRemoteRelativePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        return path.trim().replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
    }

    /**
     * 将可选字符串转换为非空字符串。
     */
    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 规范化 S3 对象前缀。
     */
    private String normalizeS3Prefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        return prefix.trim().replaceAll("^/+", "").replaceAll("/+$", "") + "/";
    }

    /**
     * 构建目标 SFTP Camel URI。
     */
    private String buildTargetSftpUri(SftpConfigRequest request, String camelDirectory) {
        return "sftp://" + request.targetHost().trim() + ":" + request.targetPort() + "/" + encodePath(camelDirectory)
                + "?username=" + encode(request.targetUsername().trim())
                + "&password=" + encode(request.targetPassword())
                + "&binary=true"
                + "&autoCreate=true"
                + "&fileExist=Append"
                + "&disconnect=true"
                + "&stepwise=false"
                + "&throwExceptionOnConnectFailed=true";
    }

    /**
     * 从 Camel Exchange 中解析当前文件名。
     */
    private String resolveFileName(Exchange exchange) {
        String relativePath = exchange.getMessage().getHeader("CamelFileRelativePath", String.class);
        if (relativePath != null && !relativePath.isBlank()) {
            return relativePath.replace('\\', '/');
        }
        return Objects.requireNonNullElse(
                exchange.getMessage().getHeader(Exchange.FILE_NAME, String.class),
                "sftp-file"
        );
    }

    /**
     * 准备临时文件写入和续传上下文。
     */
    private void prepareResumableTransfer(Exchange exchange, TargetWriter targetWriter, BandwidthLimiter bandwidthLimiter, List<String> files) throws Exception {
        String fileName = sanitizeFileName(resolveFileName(exchange));
        long sourceSize = resolveSourceSize(exchange);
        long finalSize = targetWriter.finalSize(fileName);
        long temporarySize = targetWriter.temporarySize(fileName);

        if (sourceSize > 0 && finalSize == sourceSize) {
            files.add(fileName + "（已存在，跳过）");
            exchange.setProperty("CamelRouteStop", Boolean.TRUE);
            return;
        }

        if (sourceSize > 0 && targetWriter.supportsAppendResume() && finalSize > 0 && finalSize < sourceSize && temporarySize == 0) {
            targetWriter.moveFinalToTemporary(fileName);
            temporarySize = finalSize;
        }

        if (sourceSize > 0 && temporarySize >= sourceSize) {
            targetWriter.promoteTemporaryToFinal(fileName);
            files.add(fileName);
            exchange.setProperty("CamelRouteStop", Boolean.TRUE);
            return;
        }

        InputStream inputStream = exchange.getMessage().getBody(InputStream.class);
        if (inputStream != null && temporarySize > 0 && targetWriter.supportsAppendResume()) {
            skipFully(inputStream, temporarySize);
        } else if (!targetWriter.supportsAppendResume()) {
            temporarySize = 0L;
        }
        if (inputStream != null) {
            exchange.getMessage().setBody(inputStream);
        }

        if (!bandwidthLimiter.enabled()) {
            exchange.getMessage().setHeader(Exchange.FILE_NAME, targetWriter.temporaryFileName(fileName));
            exchange.setProperty("finalFileName", fileName);
            return;
        }
        if (inputStream != null) {
            exchange.getMessage().setBody(new RateLimitedInputStream(inputStream, bandwidthLimiter));
        }
        exchange.getMessage().setHeader(Exchange.FILE_NAME, targetWriter.temporaryFileName(fileName));
        exchange.setProperty("finalFileName", fileName);
    }

    /**
     * 完成临时文件提升并记录同步成功文件。
     */
    private void completeResumableTransfer(Exchange exchange, TargetWriter targetWriter, List<String> files) throws Exception {
        String fileName = exchange.getProperty("finalFileName", String.class);
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        targetWriter.promoteTemporaryToFinal(fileName);
        files.add(fileName);
    }

    /**
     * 解析源文件大小。
     */
    private long resolveSourceSize(Exchange exchange) {
        Long fileLength = exchange.getMessage().getHeader("CamelFileLength", Long.class);
        return fileLength == null ? -1L : fileLength;
    }

    /**
     * 清理并校验相对文件名，防止路径穿越。
     */
    private String sanitizeFileName(String fileName) {
        String normalizedFileName = fileName.replace('\\', '/').replaceAll("^/+", "");
        if (normalizedFileName.isBlank() || List.of(normalizedFileName.split("/")).contains("..")) {
            throw new IllegalArgumentException("非法文件名: " + fileName);
        }
        return normalizedFileName;
    }

    /**
     * 从输入流中完整跳过指定字节数。
     */
    private void skipFully(InputStream inputStream, long bytesToSkip) throws IOException {
        long remainingBytes = bytesToSkip;
        byte[] buffer = new byte[8192];
        while (remainingBytes > 0) {
            long skippedBytes = inputStream.skip(remainingBytes);
            if (skippedBytes > 0) {
                remainingBytes -= skippedBytes;
                continue;
            }
            int readBytes = inputStream.read(buffer, 0, (int) Math.min(buffer.length, remainingBytes));
            if (readBytes == -1) {
                throw new IOException("源文件长度小于续传偏移量");
            }
            remainingBytes -= readBytes;
        }
    }

    /**
     * 将 MB/s 限速转换为 B/s。
     */
    private long toBytesPerSecond(BigDecimal bandwidthLimitMbps) {
        if (bandwidthLimitMbps == null || bandwidthLimitMbps.signum() <= 0) {
            return 0L;
        }
        return bandwidthLimitMbps
                .multiply(BigDecimal.valueOf(1024L * 1024L))
                .setScale(0, RoundingMode.HALF_UP)
                .max(BigDecimal.ONE)
                .longValue();
    }

    /**
     * 格式化限速显示文本。
     */
    private String formatBandwidthLimit(BigDecimal bandwidthLimitMbps, long bandwidthLimitBytesPerSecond) {
        if (bandwidthLimitMbps == null || bandwidthLimitBytesPerSecond <= 0) {
            return "不限速";
        }
        BigDecimal bandwidthLimitKbps = bandwidthLimitMbps.multiply(BigDecimal.valueOf(1024));
        return bandwidthLimitMbps.stripTrailingZeros().toPlainString()
                + " MB/s ≈ "
                + bandwidthLimitKbps.stripTrailingZeros().toPlainString()
                + " KB/s ≈ "
                + bandwidthLimitBytesPerSecond
                + " B/s";
    }

    /**
     * URL 编码查询参数。
     */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 扫描源 SFTP 目录并转换为文件清单。
     */
    private RemoteDirectoryScan scanRemoteDirectory(SftpConfigRequest request) {
        Session session = null;
        ChannelSftp channel = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(request.username().trim(), request.host().trim(), request.port());
            session.setPassword(request.password());
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(15_000);

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(15_000);
            String homeDirectory = channel.pwd();
            String camelDirectory = toCamelDirectory(homeDirectory, request.sftpPath().trim());

            List<SourceFile> sourceFiles = scanSourceFiles(channel, request.sftpPath().trim());
            List<String> formattedEntries = sourceFiles.stream()
                    .map(file -> "文件 " + file.relativePath() + " size=" + file.size())
                    .toList();
            long totalBytes = sourceFiles.stream()
                    .mapToLong(SourceFile::size)
                    .sum();
            return new RemoteDirectoryScan(formattedEntries, sourceFiles.size(), totalBytes, homeDirectory, camelDirectory, sourceFiles);
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            LOGGER.warn("SFTP remote directory scan failed: path={}, error={}", request.sftpPath().trim(), message);
            return new RemoteDirectoryScan(List.of("扫描失败: " + message), 0, 0L, "/", trimLeadingSlash(request.sftpPath().trim()), List.of());
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 递归扫描源目录下的文件。
     */
    private List<SourceFile> scanSourceFiles(ChannelSftp channel, String rootPath) throws com.jcraft.jsch.SftpException {
        List<SourceFile> sourceFiles = new ArrayList<>();
        scanSourceFiles(channel, rootPath.replaceAll("/+$", ""), "", sourceFiles);
        return sourceFiles;
    }

    /**
     * 递归扫描当前目录。
     */
    private void scanSourceFiles(ChannelSftp channel, String currentPath, String relativePath, List<SourceFile> sourceFiles) throws com.jcraft.jsch.SftpException {
        Vector<ChannelSftp.LsEntry> entries = channel.ls(currentPath);
        for (ChannelSftp.LsEntry entry : entries) {
            String filename = entry.getFilename();
            if (filename.equals(".") || filename.equals("..")) {
                continue;
            }
            String childAbsolutePath = currentPath + "/" + filename;
            String childRelativePath = relativePath.isBlank() ? filename : relativePath + "/" + filename;
            if (entry.getAttrs().isDir()) {
                scanSourceFiles(channel, childAbsolutePath, childRelativePath, sourceFiles);
            } else {
                sourceFiles.add(new SourceFile(childRelativePath, entry.getAttrs().getSize()));
            }
        }
    }

    /**
     * 查询 SFTP 登录后的默认目录。
     */
    private String getSftpHomeDirectory(String host, int port, String username, String password) throws Exception {
        try (SftpConnection connection = connectSftp(host, port, username, password)) {
            return connection.channel().pwd();
        }
    }

    /**
     * 创建 SFTP 连接。
     */
    private SftpConnection connectSftp(String host, int port, String username, String password) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect(15_000);
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect(15_000);
        return new SftpConnection(session, channel);
    }

    /**
     * 将绝对路径转换为相对登录目录的 Camel 路径。
     */
    private String toCamelDirectory(String homeDirectory, String absolutePath) {
        List<String> homeSegments = splitUnixPath(homeDirectory);
        List<String> targetSegments = splitUnixPath(absolutePath);
        int commonLength = 0;
        while (commonLength < homeSegments.size()
                && commonLength < targetSegments.size()
                && homeSegments.get(commonLength).equals(targetSegments.get(commonLength))) {
            commonLength++;
        }

        List<String> relativeSegments = new ArrayList<>();
        for (int index = commonLength; index < homeSegments.size(); index++) {
            relativeSegments.add("..");
        }
        relativeSegments.addAll(targetSegments.subList(commonLength, targetSegments.size()));
        return relativeSegments.isEmpty() ? "." : String.join("/", relativeSegments);
    }

    /**
     * 按 Unix 路径规则拆分路径片段。
     */
    private List<String> splitUnixPath(String path) {
        String normalizedPath = path == null || path.isBlank() ? "/" : path.trim();
        String withoutLeadingSlash = trimLeadingSlash(normalizedPath);
        String withoutTrailingSlash = withoutLeadingSlash.replaceAll("/+$", "");
        if (withoutTrailingSlash.isBlank()) {
            return List.of();
        }
        return List.of(withoutTrailingSlash.split("/+"));
    }

    /**
     * 去除路径开头的斜杠。
     */
    private String trimLeadingSlash(String path) {
        return path.replaceAll("^/+", "");
    }

    /**
     * 对日志和页面展示用 URI 进行密码脱敏。
     */
    private String maskPassword(String uri) {
        return uri.replaceAll("password=[^&]*", "password=******");
    }

    /**
     * 编码 URI 路径部分，保留路径分隔符。
     */
    private String encodePath(String path) {
        String[] segments = path.split("/", -1);
        StringBuilder encodedPath = new StringBuilder();
        for (int index = 0; index < segments.length; index++) {
            if (index > 0) {
                encodedPath.append('/');
            }
            if (!segments[index].isEmpty()) {
                encodedPath.append(encode(segments[index]).replace("+", "%20"));
            }
        }
        return encodedPath.toString();
    }

}
