package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "crawler_job_record")
public class CrawlerJobRecord extends BaseEntity {

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "source_code", length = 50)
    private String sourceCode;

    @Column(name = "major_name", length = 100)
    private String majorName;

    @Column(name = "industry_keyword", length = 100)
    private String industryKeyword;

    @Column(name = "search_url", length = 500)
    private String searchUrl;

    @Column(name = "page_num")
    private Integer pageNum;

    @Column(name = "status", length = 20)
    private String status = "pending";

    @Column(name = "parsed_count")
    private Integer parsedCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_time")
    private String startedTime;

    @Column(name = "finished_time")
    private String finishedTime;

    @Column(name = "is_complete", length = 10)
    private String isComplete = "0";

    @Column(name = "raw_html", columnDefinition = "LONGTEXT")
    private String rawHtml;
}
