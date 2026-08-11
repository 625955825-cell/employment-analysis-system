package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_notice")
public class SysNotice extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "notice_type", length = 50)
    private String noticeType;

    /** 目标角色，逗号分隔：student,class_teacher,dept_teacher,admin,company,employment_staff
     * 为空或"all"表示全部用户可见 */
    @Column(name = "target_roles", length = 200)
    private String targetRoles;

    @Column(name = "publisher_id")
    private Long publisherId;

    @Column(name = "publisher_name", length = 50)
    private String publisherName;

    @Column(name = "publish_time")
    private String publishTime;

    @Column(name = "top_status", length = 10)
    private String topStatus = "0";

    @Column(name = "status", length = 20)
    private String status = "published";

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "images", columnDefinition = "TEXT")
    private String images;
}
