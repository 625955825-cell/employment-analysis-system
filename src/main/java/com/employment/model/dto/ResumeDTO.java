package com.employment.model.dto;

import lombok.Data;

@Data
public class ResumeDTO {
    private Long id;
    private Long studentId;
    private String resumeName;
    private String isDefault;
    private String personalSummary;
    private String educationExperience;
    private String projectExperience;
    private String workExperience;
    private String skillCertificates;
    private String awardsHonors;
    private String selfEvaluation;
    private Integer expectedSalaryMin;
    private Integer expectedSalaryMax;
    private String expectedCity;
    private String expectedPosition;
    private String expectedIndustry;
    private String filePath;
    private String createTime;
    private String updateTime;
}
