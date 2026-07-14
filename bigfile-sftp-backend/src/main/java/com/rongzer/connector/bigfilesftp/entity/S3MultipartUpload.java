package com.rongzer.connector.bigfilesftp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * S3 Multipart 上传任务实体。
 *
 * <p>用于记录 MinIO/S3 分片上传的 uploadId 和整体状态，支持服务重启后继续上传。</p>
 */
@Entity
@Table(name = "s3_multipart_upload")
public class S3MultipartUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "source_path", nullable = false, length = 500)
    private String sourcePath;
    @Column(name = "bucket", nullable = false, length = 100)
    private String bucket;
    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;
    @Column(name = "upload_id", nullable = false, length = 500)
    private String uploadId;
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    @Column(name = "part_size", nullable = false)
    private Long partSize;
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Long getPartSize() { return partSize; }
    public void setPartSize(Long partSize) { this.partSize = partSize; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
