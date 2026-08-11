package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "offer_letter")
public class OfferLetter extends BaseEntity {

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "position_name", length = 100)
    private String positionName;

    @Column(name = "salary", length = 50)
    private String salary;

    @Column(name = "work_city", length = 50)
    private String workCity;

    @Column(name = "start_date")
    private String startDate;

    @Column(name = "probation_period", length = 50)
    private String probationPeriod;

    @Column(name = "probation_salary", length = 50)
    private String probationSalary;

    @Column(name = "status", length = 20)
    private String status = "pending";

    @Column(name = "response_deadline")
    private String responseDeadline;
}
