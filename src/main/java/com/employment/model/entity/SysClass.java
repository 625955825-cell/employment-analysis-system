package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_class")
public class SysClass extends BaseEntity {

    @Column(name = "class_name", nullable = false, length = 50)
    private String className;

    @Column(name = "major_id", nullable = false)
    private Long majorId;

    @Column(name = "dept_id", nullable = false)
    private Long deptId;

    @Column(name = "grade", length = 10)
    private String grade;

    @Column(name = "advisor", length = 50)
    private String advisor;

    @Column(name = "advisor_id")
    private Long advisorId;

    @Column(name = "student_count")
    private Integer studentCount;

    @Column(name = "status", length = 10)
    private String status = "0";

    @Column(name = "remark", length = 500)
    private String remark;

    @Transient
    private String deptName;

    @Transient
    private String majorName;
}
