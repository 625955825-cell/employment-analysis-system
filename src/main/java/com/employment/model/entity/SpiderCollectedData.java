package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "spider_collected_data")
public class SpiderCollectedData extends BaseEntity {

    @Column(name = "data_type", length = 30)
    private String dataType;

    @Column(name = "source_code", length = 50)
    private String sourceCode;

    @Column(name = "major_name", length = 100)
    private String majorName;

    @Column(name = "industry_keyword", length = 100)
    private String industryKeyword;

    @Column(name = "job_name", length = 200)
    private String jobName;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "salary", length = 50)
    private String salary;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "company_scale", length = 50)
    private String companyScale;

    @Column(name = "education", length = 50)
    private String education;

    @Column(name = "experience", length = 50)
    private String experience;

    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    @Column(name = "responsibility", columnDefinition = "TEXT")
    private String responsibility;

    @Column(name = "raw_data", columnDefinition = "LONGTEXT")
    private String rawData;

    @Column(name = "collect_time")
    private String collectTime;

    @Column(name = "detail_url", columnDefinition = "TEXT")
    private String detailUrl;

    @Column(name = "is_valid", length = 10)
    private String isValid = "0"; // "0"=待清洗 "1"=有效 "2"=无效

    @Column(name = "is_synced", length = 10)
    private String isSynced = "0"; // "0"=未同步 "1"=已同步

    @Column(name = "sync_time")
    private String syncTime;

    @Column(name = "synced_record_id")
    private Long syncedRecordId;
}
