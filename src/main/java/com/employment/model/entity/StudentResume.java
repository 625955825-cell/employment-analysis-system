package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "student_resume")
public class StudentResume extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "resume_name", nullable = false, length = 100)
    private String resumeName;

    @Column(name = "is_default", length = 10)
    private String isDefault = "0";

    @Column(name = "personal_summary", columnDefinition = "TEXT")
    private String personalSummary;

    @Column(name = "education_experience", columnDefinition = "TEXT")
    private String educationExperience;

    @Column(name = "project_experience", columnDefinition = "TEXT")
    private String projectExperience;

    @Column(name = "work_experience", columnDefinition = "TEXT")
    private String workExperience;

    @Column(name = "skill_certificates", columnDefinition = "TEXT")
    private String skillCertificates;

    @Column(name = "awards_honors", columnDefinition = "TEXT")
    private String awardsHonors;

    @Column(name = "self_evaluation", columnDefinition = "TEXT")
    private String selfEvaluation;

    @Column(name = "expected_salary_min")
    private Integer expectedSalaryMin;

    @Column(name = "expected_salary_max")
    private Integer expectedSalaryMax;

    @Column(name = "expected_city", length = 100)
    private String expectedCity;

    @Column(name = "expected_position", length = 100)
    private String expectedPosition;

    @Column(name = "expected_industry", length = 100)
    private String expectedIndustry;

    @Column(name = "file_path", length = 500)
    private String filePath;
}
