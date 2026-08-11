package com.employment.repository;

import com.employment.model.entity.ConsultationBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultationBookingRepository extends JpaRepository<ConsultationBooking, Long> {
    List<ConsultationBooking> findByStudentId(Long studentId);
    List<ConsultationBooking> findByStatus(String status);
    List<ConsultationBooking> findByConsultantId(Long consultantId);
}
