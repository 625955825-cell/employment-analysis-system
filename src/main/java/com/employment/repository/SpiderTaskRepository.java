package com.employment.repository;

import com.employment.model.entity.SpiderTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SpiderTaskRepository extends JpaRepository<SpiderTask, Long> {

    Page<SpiderTask> findAllByOrderByCreateTimeDesc(Pageable pageable);

    Page<SpiderTask> findByStatusOrderByCreateTimeDesc(String status, Pageable pageable);

    Page<SpiderTask> findBySourceCodeOrderByCreateTimeDesc(String sourceCode, Pageable pageable);

    @Query("SELECT s FROM SpiderTask s WHERE " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:keyword IS NULL OR s.taskName LIKE %:keyword%) " +
           "ORDER BY s.createTime DESC")
    Page<SpiderTask> searchTasks(@Param("status") String status, @Param("keyword") String keyword, Pageable pageable);

    List<SpiderTask> findByIsScheduled(String isScheduled);

    @Modifying
    @Transactional
    @Query("UPDATE SpiderTask s SET s.progress = 0, s.collectedCount = 0, s.status = 'failed' WHERE s.progress > 100 OR s.progress < 0")
    int fixInvalidProgress();

    @Modifying
    @Transactional
    @Query("UPDATE SpiderTask s SET s.progress = 100, s.status = 'failed' WHERE s.progress > 100")
    int clampProgress();
}
