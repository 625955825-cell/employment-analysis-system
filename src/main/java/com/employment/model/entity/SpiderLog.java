package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "spider_log")
public class SpiderLog extends BaseEntity {

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "task_name", length = 200)
    private String taskName;

    @Column(name = "level", length = 10)
    private String level = "INFO";

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "source_code", length = 50)
    private String sourceCode;

    @Column(name = "log_time")
    private String logTime;

    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;
}
