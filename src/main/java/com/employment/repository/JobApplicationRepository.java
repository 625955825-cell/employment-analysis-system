package com.employment.repository;

import com.employment.model.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByStudentId(Long studentId);
    List<JobApplication> findByCompanyId(Long companyId);
    List<JobApplication> findByJobId(Long jobId);
    Optional<JobApplication> findByJobIdAndStudentId(Long jobId, Long studentId);
    boolean existsByJobIdAndStudentId(Long jobId, Long studentId);
}
