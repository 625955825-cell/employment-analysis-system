package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "career_activity")
public class CareerActivity extends BaseEntity {

    @Column(name = "activity_name", nullable = false, length = 200)
    private String activityName;

    @Column(name = "activity_type", length = 50)
    private String activityType;

    @Column(name = "organizer", length = 200)
    private String organizer;

    @Column(name = "speaker", length = 100)
    private String speaker;

    @Column(name = "start_time")
    private String startTime;

    @Column(name = "end_time")
    private String endTime;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "current_participants")
    private Integer currentParticipants = 0;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "poster", length = 500)
    private String poster;

    @Column(name = "status", length = 20)
    private String status = "published";

    @Column(name = "publish_time")
    private String publishTime;
}
