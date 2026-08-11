package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "interview_record")
public class InterviewRecord extends BaseEntity {

    @Column(name = "invitation_id", nullable = false)
    private Long invitationId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "interview_result", length = 50)
    private String interviewResult;

    @Column(name = "interview_feedback", columnDefinition = "TEXT")
    private String interviewFeedback;

    @Column(name = "score")
    private Integer score;

    @Column(name = "company_remark", length = 500)
    private String companyRemark;
}
