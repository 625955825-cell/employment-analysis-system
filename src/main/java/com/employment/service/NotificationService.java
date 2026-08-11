package com.employment.service;

import com.employment.config.NotificationWebSocketHandler;
import com.employment.model.entity.Notification;
import com.employment.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationWebSocketHandler webSocketHandler;

    @Transactional
    public void sendNotification(Long userId, String title, String content, String type, String category) {
        if (userId == null) return;
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setType(type);
            notification.setCategory(category);
            notification.setIsRead("0");
            notification = notificationRepository.save(notification);

            // 通过 WebSocket 实时推送
            Map<String, Object> pushData = new HashMap<>();
            pushData.put("type", "notification");
            pushData.put("id", notification.getId());
            pushData.put("title", title);
            pushData.put("content", content);
            pushData.put("category", category);
            pushData.put("createTime", notification.getCreateTime());
            webSocketHandler.sendToUser(userId, pushData);

            log.info("通知发送成功: userId={}, title={}", userId, title);
        } catch (Exception e) {
            log.error("发送通知失败: {}", e.getMessage());
        }
    }

    @Transactional
    public void sendToRole(String roleKey, String title, String content, String type) {
        log.info("Role-based notification (not implemented): role={}, title={}", roleKey, title);
    }
}
