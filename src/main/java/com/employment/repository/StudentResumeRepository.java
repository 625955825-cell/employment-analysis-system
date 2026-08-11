package com.employment.repository;

import com.employment.model.entity.StudentResume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentResumeRepository extends JpaRepository<StudentResume, Long> {
    List<StudentResume> findByStudentId(Long studentId);
    Optional<StudentResume> findByStudentIdAndIsDefault(Long studentId, String isDefault);
    int countByStudentId(Long studentId);
}
