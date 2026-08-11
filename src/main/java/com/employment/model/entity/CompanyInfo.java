package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "company_info")
public class CompanyInfo extends BaseEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "company_code", unique = true, length = 50)
    private String companyCode;

    @Column(name = "unified_credit_code", length = 50)
    private String unifiedCreditCode;

    @Column(name = "legal_person", length = 50)
    private String legalPerson;

    @Column(name = "contact_person", length = 50)
    private String contactPerson;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "province", length = 50)
    private String province;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "district", length = 50)
    private String district;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "latitude", length = 50)
    private String latitude;

    @Column(name = "longitude", length = 50)
    private String longitude;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "scale", length = 50)
    private String scale;

    @Column(name = "nature", length = 50)
    private String nature;

    @Column(name = "website", length = 200)
    private String website;

    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "logo", length = 500)
    private String logo;

    @Column(name = "business_license", length = 500)
    private String businessLicense;

    @Column(name = "auth_status", length = 20)
    private String authStatus = "pending";

    @Column(name = "status", length = 10)
    private String status = "0";

    @Column(name = "dept_id")
    private Long deptId;
}
