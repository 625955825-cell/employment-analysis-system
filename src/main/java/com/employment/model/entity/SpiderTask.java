package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "spider_task")
public class SpiderTask extends BaseEntity {

    @Column(name = "task_name", nullable = false, length = 200)
    private String taskName;

    @Column(name = "source_code", length = 50)
    private String sourceCode;

    @Column(name = "source_name", length = 100)
    private String sourceName;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    @Column(name = "data_types", length = 200)
    private String dataTypes;

    @Column(name = "depth")
    private Integer depth = 1;

    @Column(name = "status", length = 20)
    private String status = "pending";

    @Column(name = "progress")
    private Integer progress = 0;

    @Column(name = "collected_count")
    private Integer collectedCount = 0;

    @Column(name = "success_rate")
    private Integer successRate = 0;

    @Column(name = "is_scheduled", length = 10)
    private String isScheduled = "0";

    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(name = "last_run_time")
    private String lastRunTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "selected_majors", columnDefinition = "TEXT")
    private String selectedMajors;

    @Column(name = "created_by")
    private Long createdBy;
}
