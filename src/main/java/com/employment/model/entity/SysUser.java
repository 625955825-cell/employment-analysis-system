package com.employment.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.NaturalId;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_user")
public class SysUser extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
    @NaturalId
    private String username;

    @JsonIgnore
    @Column(name = "password", nullable = false, length = 200)
    private String password;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "id_card", length = 20)
    private String idCard;

    @Column(name = "status", length = 10)
    private String status = "0";

    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "major_id")
    private Long majorId;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(name = "student_no", length = 30)
    private String studentNo;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    @Column(name = "last_login_time")
    private String lastLoginTime;

    @Column(name = "remark", length = 500)
    private String remark;
}
