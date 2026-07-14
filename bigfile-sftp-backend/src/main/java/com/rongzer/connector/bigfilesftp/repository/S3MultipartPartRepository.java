package com.rongzer.connector.bigfilesftp.repository;

import com.rongzer.connector.bigfilesftp.entity.S3MultipartPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * S3 Multipart 分片数据访问接口。
 */
public interface S3MultipartPartRepository extends JpaRepository<S3MultipartPart, Long> {

    List<S3MultipartPart> findByUploadIdOrderByPartNumberAsc(String uploadId);

    Optional<S3MultipartPart> findByUploadIdAndPartNumber(String uploadId, Integer partNumber);
}
