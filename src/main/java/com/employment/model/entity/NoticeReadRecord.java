package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "notice_read_record",
       uniqueConstraints = @UniqueConstraint(columnNames = {"notice_id", "user_id"}))
public class NoticeReadRecord extends BaseEntity {

    @Column(name = "notice_id", nullable = false)
    private Long noticeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "read_time")
    private String readTime;
}
