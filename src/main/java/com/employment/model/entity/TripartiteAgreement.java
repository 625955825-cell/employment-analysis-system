package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tripartite_agreement")
public class TripartiteAgreement extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "employment_record_id")
    private Long employmentRecordId;

    @Column(name = "agreement_no", length = 100)
    private String agreementNo;

    @Column(name = "student_sign_time")
    private String studentSignTime;

    @Column(name = "company_sign_time")
    private String companySignTime;

    @Column(name = "school_sign_time")
    private String schoolSignTime;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "status", length = 20)
    private String status = "pending";
}
