package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "employment_attachment")
public class EmploymentAttachment extends BaseEntity {

    @Column(name = "employment_id", nullable = false)
    private Long employmentId;

    @Column(name = "attachment_type", length = 50)
    private String attachmentType;

    @Column(name = "attachment_name", length = 200)
    private String attachmentName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "upload_status", length = 20)
    private String uploadStatus = "pending";
}
