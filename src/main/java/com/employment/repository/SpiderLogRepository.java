package com.employment.repository;

import com.employment.model.entity.SpiderLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpiderLogRepository extends JpaRepository<SpiderLog, Long> {

    Page<SpiderLog> findAllByOrderByCreateTimeDesc(Pageable pageable);

    Page<SpiderLog> findByLevelOrderByCreateTimeDesc(String level, Pageable pageable);

    @Query("SELECT l FROM SpiderLog l WHERE " +
           "(:level IS NULL OR l.level = :level) AND " +
           "(:taskName IS NULL OR l.taskName LIKE %:taskName%) " +
           "ORDER BY l.createTime DESC")
    Page<SpiderLog> searchLogs(@Param("level") String level, @Param("taskName") String taskName, Pageable pageable);

    List<SpiderLog> findByTaskIdOrderByCreateTimeDesc(Long taskId);
}
