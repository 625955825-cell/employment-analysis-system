package com.employment.repository;

import com.employment.model.entity.ConversationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRecordRepository extends JpaRepository<ConversationRecord, Long> {
    List<ConversationRecord> findByTeacherId(Long teacherId);
    List<ConversationRecord> findByStudentId(Long studentId);
}
