package com.employment.repository;

import com.employment.model.entity.DataPermissionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataPermissionRequestRepository extends JpaRepository<DataPermissionRequest, Long> {
    List<DataPermissionRequest> findByStudentId(Long studentId);
    List<DataPermissionRequest> findByStatus(String status);
    List<DataPermissionRequest> findByAuditUserId(Long auditUserId);
}
