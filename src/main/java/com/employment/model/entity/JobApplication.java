package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "job_application")
public class JobApplication extends BaseEntity {

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "resume_id")
    private Long resumeId;

    @Column(name = "status", length = 20)
    private String status = "pending";

    @Column(name = "read_status", length = 10)
    private String readStatus = "0";

    @Column(name = "apply_letter", columnDefinition = "TEXT")
    private String applyLetter;

    @Column(name = "company_remark", length = 500)
    private String companyRemark;

    @Column(name = "interview_status", length = 20)
    private String interviewStatus;

    @Column(name = "offer_status", length = 20)
    private String offerStatus;
}
