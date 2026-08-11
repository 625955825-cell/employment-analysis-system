package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_dept")
public class SysDept extends BaseEntity {

    @Column(name = "dept_name", nullable = false, length = 100)
    private String deptName;

    @Column(name = "dept_code", length = 50)
    private String deptCode;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "is_top_level", length = 10)
    private String isTopLevel;

    @Column(name = "sort")
    private Integer sort;

    @Column(name = "status", length = 10)
    private String status = "0";

    @Column(name = "remark", length = 500)
    private String remark;
}
