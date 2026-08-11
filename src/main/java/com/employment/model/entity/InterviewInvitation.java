package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "interview_invitation")
public class InterviewInvitation extends BaseEntity {

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "interview_time")
    private String interviewTime;

    @Column(name = "interview_address", length = 200)
    private String interviewAddress;

    @Column(name = "interview_type", length = 50)
    private String interviewType;

    @Column(name = "contact_person", length = 50)
    private String contactPerson;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "status", length = 20)
    private String status = "pending";
}
