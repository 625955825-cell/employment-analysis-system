package com.employment.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "consultation_booking")
public class ConsultationBooking extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "consultant_id")
    private Long consultantId;

    @Column(name = "booking_date")
    private String bookingDate;

    @Column(name = "booking_time", length = 50)
    private String bookingTime;

    @Column(name = "consultation_type", length = 50)
    private String consultationType;

    @Column(name = "topic", length = 200)
    private String topic;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", length = 20)
    private String status = "pending";

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;
}
