package com.employment.model.dto;

import lombok.Data;

@Data
public class RecommendResultDTO {
    private Long targetId;
    private String targetName;
    private String targetInfo;
    private Integer matchScore;
    private String algorithmType;
    private String industry;
    private String city;
    private String salary;
    private String source;
    private String education;
    private String experience;
    private String companyName;
    private String detailUrl;
    private String responsibility;
    private String matchReason;
    /** 职位来源: hr=系统HR发布的职位, spider=爬虫数据 */
    private String positionSource;
}
