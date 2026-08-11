package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_permission")
public class SysPermission extends BaseEntity {

    @Column(name = "permission_name", nullable = false, length = 100)
    private String permissionName;

    @Column(name = "permission_key", nullable = false, length = 100)
    private String permissionKey;

    @Column(name = "permission_type", length = 20)
    private String permissionType;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "path", length = 200)
    private String path;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "component", length = 200)
    private String component;

    @Column(name = "sort")
    private Integer sort;

    @Column(name = "status", length = 10)
    private String status = "0";
}
