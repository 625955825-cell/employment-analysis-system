package com.employment.repository;

import com.employment.model.entity.ClassEmploymentReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassEmploymentReminderRepository extends JpaRepository<ClassEmploymentReminder, Long> {
    List<ClassEmploymentReminder> findByReceiverIdOrderByCreateTimeDesc(Long receiverId);
    List<ClassEmploymentReminder> findBySenderIdOrderByCreateTimeDesc(Long senderId);
    List<ClassEmploymentReminder> findByClassIdOrderByCreateTimeDesc(Long classId);
    int countByReceiverIdAndIsRead(Long receiverId, String isRead);
}
