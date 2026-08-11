package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "data_permission_request")
public class DataPermissionRequest extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "major_id")
    private Long majorId;

    @Column(name = "request_type", length = 50)
    private String requestType;

    @Column(name = "year_from")
    private Integer yearFrom;

    @Column(name = "year_to")
    private Integer yearTo;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "status", length = 20)
    private String status = "pending";

    @Column(name = "audit_user_id")
    private Long auditUserId;

    @Column(name = "audit_time")
    private String auditTime;

    @Column(name = "audit_remark", length = 500)
    private String auditRemark;
}
