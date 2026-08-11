package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "company_auth")
public class CompanyAuth extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "auth_type", length = 50)
    private String authType;

    @Column(name = "auth_name", length = 100)
    private String authName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "audit_status", length = 20)
    private String auditStatus = "pending";

    @Column(name = "audit_user_id")
    private Long auditUserId;

    @Column(name = "audit_time")
    private String auditTime;

    @Column(name = "audit_remark", length = 500)
    private String auditRemark;
}
