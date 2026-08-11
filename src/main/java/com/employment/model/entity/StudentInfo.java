package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "student_info")
public class StudentInfo extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "student_no", nullable = false, length = 30)
    private String studentNo;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "birth_date")
    private String birthDate;

    @Column(name = "id_card", length = 20)
    private String idCard;

    @Column(name = "nation", length = 50)
    private String nation;

    @Column(name = "politics_status", length = 50)
    private String politicsStatus;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "province", length = 50)
    private String province;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "dept_name", length = 100)
    private String deptName;

    @Column(name = "major_id")
    private Long majorId;

    @Column(name = "major_name", length = 100)
    private String majorName;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(name = "study_type", length = 20)
    private String studyType;

    @Column(name = "dormitory", length = 100)
    private String dormitory;

    @Column(name = "emergency_contact", length = 50)
    private String emergencyContact;

    @Column(name = "emergency_phone", length = 20)
    private String emergencyPhone;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "status", length = 20)
    private String status = "studying";
}
