package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "activity_registration")
public class ActivityRegistration extends BaseEntity {

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "registration_time")
    private String registrationTime;

    @Column(name = "status", length = 20)
    private String status = "registered";

    @Column(name = "check_in_time")
    private String checkInTime;
}
