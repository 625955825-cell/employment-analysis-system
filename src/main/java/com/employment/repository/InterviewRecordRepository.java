package com.employment.repository;

import com.employment.model.entity.InterviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewRecordRepository extends JpaRepository<InterviewRecord, Long> {
    List<InterviewRecord> findByStudentId(Long studentId);
    List<InterviewRecord> findByCompanyId(Long companyId);
    List<InterviewRecord> findByInvitationId(Long invitationId);
}
