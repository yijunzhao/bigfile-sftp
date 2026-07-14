package com.rongzer.connector.bigfilesftp.repository;

import com.rongzer.connector.bigfilesftp.entity.S3MultipartUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * S3 Multipart 上传任务数据访问接口。
 */
public interface S3MultipartUploadRepository extends JpaRepository<S3MultipartUpload, Long> {

    Optional<S3MultipartUpload> findFirstBySourcePathAndBucketAndObjectKeyAndFileSizeAndStatusOrderByIdDesc(
            String sourcePath,
            String bucket,
            String objectKey,
            Long fileSize,
            String status
    );
}
