package com.employment.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobDetailVO {

    private Long id;
    private Long companyId;
    private String companyName;
    private String companyLogo;
    private String companyScale;
    private String companyIndustry;
    private String companyNature;
    private String companyCity;
    private String companyAddress;
    private String companyIntroduction;

    private String jobName;
    private String jobCategory;
    private String jobType;
    private String workCity;
    private String workAddress;
    private String salaryMin;
    private String salaryMax;
    private String salaryMonths;
    private Integer recruitNumber;
    private String requirement;
    private String responsibility;
    private String benefits;
    private String educationRequired;
    private String experienceRequired;
    private String skillRequired;
    private String isRemote;
    private String isHighSalary;
    private Integer viewCount;
    private Integer applyCount;
    private String status;
    private String publishTime;
    private String deadline;

    private Boolean hasApplied;
    private Boolean hasFavorite;
}
