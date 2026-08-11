package com.employment.repository;

import com.employment.model.entity.JobFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobFavoriteRepository extends JpaRepository<JobFavorite, Long> {
    List<JobFavorite> findByStudentId(Long studentId);
    Optional<JobFavorite> findByJobIdAndStudentId(Long jobId, Long studentId);
    boolean existsByJobIdAndStudentId(Long jobId, Long studentId);
    void deleteByJobIdAndStudentId(Long jobId, Long studentId);
}
