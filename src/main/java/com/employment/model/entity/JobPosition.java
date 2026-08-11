package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "job_position")
public class JobPosition extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "job_category", length = 100)
    private String jobCategory;

    @Column(name = "job_type", length = 50)
    private String jobType;

    @Column(name = "work_city", length = 50)
    private String workCity;

    @Column(name = "work_address", length = 200)
    private String workAddress;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Column(name = "salary_months", length = 20)
    private String salaryMonths;

    @Column(name = "recruit_number")
    private Integer recruitNumber;

    @Column(name = "requirement", columnDefinition = "TEXT")
    private String requirement;

    @Column(name = "responsibility", columnDefinition = "TEXT")
    private String responsibility;

    @Column(name = "benefits", columnDefinition = "TEXT")
    private String benefits;

    @Column(name = "education_required", length = 50)
    private String educationRequired;

    @Column(name = "experience_required", length = 50)
    private String experienceRequired;

    @Column(name = "skill_required", columnDefinition = "TEXT")
    private String skillRequired;

    @Column(name = "is_remote", length = 10)
    private String isRemote = "0";

    @Column(name = "is_high_salary", length = 10)
    private String isHighSalary = "0";

    @Column(name = "is_deleted", length = 10)
    private String isDeleted = "0";

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "apply_count")
    private Integer applyCount = 0;

    @Column(name = "status", length = 20)
    private String status = "published";

    @Column(name = "publish_time")
    private String publishTime;

    @Column(name = "deadline")
    private String deadline;
}
