package com.rongzer.connector.bigfilesftp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * S3 Multipart 已完成分片实体。
 */
@Entity
@Table(name = "s3_multipart_part")
public class S3MultipartPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "upload_id", nullable = false, length = 500)
    private String uploadId;
    @Column(name = "part_number", nullable = false)
    private Integer partNumber;
    @Column(name = "offset_value", nullable = false)
    private Long offset;
    @Column(name = "part_size", nullable = false)
    private Long size;
    @Column(name = "etag", nullable = false, length = 255)
    private String eTag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    public Integer getPartNumber() { return partNumber; }
    public void setPartNumber(Integer partNumber) { this.partNumber = partNumber; }
    public Long getOffset() { return offset; }
    public void setOffset(Long offset) { this.offset = offset; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    public String geteTag() { return eTag; }
    public void seteTag(String eTag) { this.eTag = eTag; }
}
