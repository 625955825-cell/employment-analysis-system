package com.employment.repository;

import com.employment.model.entity.SysLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SysLogRepository extends JpaRepository<SysLog, Long> {
    Page<SysLog> findByLogType(String logType, Pageable pageable);
    Page<SysLog> findByUsername(String username, Pageable pageable);
    Page<SysLog> findByLogTypeAndUsername(String logType, String username, Pageable pageable);
}
