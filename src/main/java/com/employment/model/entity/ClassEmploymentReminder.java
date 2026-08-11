package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "class_employment_reminder")
public class ClassEmploymentReminder extends BaseEntity {

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_name", length = 50)
    private String senderName;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "receiver_name", length = 50)
    private String receiverName;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "content", length = 1000)
    private String content;

    @Column(name = "employment_rate", length = 20)
    private String employmentRate;

    @Column(name = "total_students")
    private Integer totalStudents;

    @Column(name = "employed_students")
    private Integer employedStudents;

    @Column(name = "unemployed_students")
    private Integer unemployedStudents;

    @Column(name = "status", length = 20)
    private String status = "unread";

    @Column(name = "is_read", length = 1)
    private String isRead = "0";
}
