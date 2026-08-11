package com.employment.repository;

import com.employment.model.entity.EmploymentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmploymentAttachmentRepository extends JpaRepository<EmploymentAttachment, Long> {
    List<EmploymentAttachment> findByEmploymentId(Long employmentId);
}
