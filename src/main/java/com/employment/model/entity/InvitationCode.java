package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_invitation_code")
public class InvitationCode extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "role_key", length = 50)
    private String roleKey;

    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "used_by")
    private Long usedBy;

    @Column(name = "used_username", length = 50)
    private String usedUsername;

    @Column(name = "used_time")
    private LocalDateTime usedTime;

    @Column(name = "expires_time")
    private LocalDateTime expiresTime;

    @Column(name = "status", length = 10)
    private String status = "unused";

    @Column(name = "remark", length = 500)
    private String remark;
}
