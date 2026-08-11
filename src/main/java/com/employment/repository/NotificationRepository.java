package com.employment.repository;

import com.employment.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreateTimeDesc(Long userId);
    List<Notification> findByUserIdAndIsReadOrderByCreateTimeDesc(Long userId, String isRead);
    long countByUserIdAndIsRead(Long userId, String isRead);
}
