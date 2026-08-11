package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_role")
public class SysRole extends BaseEntity {

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "role_key", nullable = false, unique = true, length = 50)
    private String roleKey;

    @Column(name = "role_sort")
    private Integer roleSort;

    @Column(name = "status", length = 10)
    private String status = "0";

    @Column(name = "remark", length = 500)
    private String remark;
}
