package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "employment_record")
public class EmploymentRecord extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "employment_type", nullable = false, length = 50)
    private String employmentType;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "company_code", length = 50)
    private String companyCode;

    @Column(name = "company_scale", length = 50)
    private String companyScale;

    @Column(name = "company_industry", length = 100)
    private String companyIndustry;

    @Column(name = "position_name", length = 100)
    private String positionName;

    @Column(name = "position_category", length = 100)
    private String positionCategory;

    @Column(name = "work_city", length = 50)
    private String workCity;

    @Column(name = "work_province", length = 50)
    private String workProvince;

    @Column(name = "salary", length = 50)
    private String salary;

    @Column(name = "is_three_party_signed", length = 10)
    private String isThreePartySigned = "0";

    @Column(name = "three_party_no", length = 100)
    private String threePartyNo;

    @Column(name = "contract_start_date")
    private String contractStartDate;

    @Column(name = "contract_end_date")
    private String contractEndDate;

    @Column(name = "probation_salary", length = 50)
    private String probationSalary;

    @Column(name = "audit_status", length = 20)
    private String auditStatus = "pending";

    @Column(name = "audit_remark", length = 500)
    private String auditRemark;

    @Column(name = "audit_user_id")
    private Long auditUserId;

    @Column(name = "audit_time")
    private String auditTime;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;
}
