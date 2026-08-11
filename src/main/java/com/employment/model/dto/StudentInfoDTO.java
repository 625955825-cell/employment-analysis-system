package com.employment.model.dto;

import lombok.Data;

@Data
public class StudentInfoDTO {

    private Long id;
    private Long userId;
    private String studentNo;
    private String realName;
    private String gender;
    private String birthDate;
    private String idCard;
    private String nation;
    private String politicsStatus;
    private String phone;
    private String email;
    private String province;
    private String city;
    private String address;
    private Long deptId;
    private String deptName;
    private Long majorId;
    private String majorName;
    private String className;
    private Integer graduationYear;
    private String studyType;
    private String dormitory;
    private String emergencyContact;
    private String emergencyPhone;
    private String avatar;
    private String status;
}
