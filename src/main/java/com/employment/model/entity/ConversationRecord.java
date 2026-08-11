package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "conversation_record")
public class ConversationRecord extends BaseEntity {

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "conversation_time")
    private String conversationTime;

    @Column(name = "conversation_type", length = 50)
    private String conversationType;

    @Column(name = "conversation_place", length = 200)
    private String conversationPlace;

    @Column(name = "topic", length = 200)
    private String topic;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    @Column(name = "next_plan", columnDefinition = "TEXT")
    private String nextPlan;

    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;
}
