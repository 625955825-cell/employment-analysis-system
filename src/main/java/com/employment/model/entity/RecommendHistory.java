package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "recommend_history")
public class RecommendHistory extends BaseEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_type", length = 20)
    private String userType;

    @Column(name = "recommend_type", length = 20)
    private String recommendType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_name", length = 200)
    private String targetName;

    @Column(name = "target_info", length = 500)
    private String targetInfo;

    @Column(name = "match_score")
    private Integer matchScore;

    @Column(name = "algorithm_type", length = 50)
    private String algorithmType;

    @Column(name = "feedback", length = 20)
    private String feedback;

    @Column(name = "feedback_reason", length = 500)
    private String feedbackReason;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "salary", length = 50)
    private String salary;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "is_viewed", length = 10)
    private String isViewed = "0";
}
