package com.employment.repository;

import com.employment.model.entity.InterviewInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewInvitationRepository extends JpaRepository<InterviewInvitation, Long> {
    List<InterviewInvitation> findByStudentId(Long studentId);
    List<InterviewInvitation> findByCompanyId(Long companyId);
    List<InterviewInvitation> findByApplicationId(Long applicationId);
}
