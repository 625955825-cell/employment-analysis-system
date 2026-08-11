package com.employment.repository;

import com.employment.model.entity.NoticeReadRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeReadRecordRepository extends JpaRepository<NoticeReadRecord, Long> {
    Optional<NoticeReadRecord> findByNoticeIdAndUserId(Long noticeId, Long userId);
    List<NoticeReadRecord> findByUserId(Long userId);
    long countByUserId(Long userId);
}
