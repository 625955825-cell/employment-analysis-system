package com.employment.repository;

import com.employment.model.entity.CrawlerJobRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrawlerJobRecordRepository extends JpaRepository<CrawlerJobRecord, Long> {

    List<CrawlerJobRecord> findByTaskIdOrderByCreateTimeDesc(Long taskId);

    List<CrawlerJobRecord> findByTaskIdAndStatus(Long taskId, String status);

    List<CrawlerJobRecord> findByTaskIdAndIsComplete(Long taskId, String isComplete);

    long countByTaskId(Long taskId);

    long countByTaskIdAndStatus(Long taskId, String status);

    @Modifying
    @Query(value = "UPDATE crawler_job_record SET status = :status, is_complete = '1', finished_time = NOW() WHERE id = :id", nativeQuery = true)
    void markComplete(@Param("id") Long id, @Param("status") String status);
}
