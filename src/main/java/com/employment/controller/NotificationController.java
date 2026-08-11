package com.employment.controller;

import com.employment.common.PageResult;
import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.entity.Notification;
import com.employment.repository.NotificationRepository;
import com.employment.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final SecurityUtils securityUtils;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = securityUtils.getCurrentUserId();
        List<Notification> all = notificationRepository.findByUserIdOrderByCreateTimeDesc(userId);
        long total = all.size();
        int start = Math.min((page - 1) * size, (int) total);
        int end = Math.min(start + size, (int) total);
        List<Notification> records = start < total ? all.subList(start, end) : Collections.emptyList();
        long unreadCount = notificationRepository.countByUserIdAndIsRead(userId, "0");

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("unreadCount", unreadCount);
        return Result.success(result);
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount() {
        Long userId = securityUtils.getCurrentUserId();
        long count = notificationRepository.countByUserIdAndIsRead(userId, "0");
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return Result.success(result);
    }

    @PutMapping("/{id}/read")
    @Transactional
    @OperationLog(module = "消息通知", content = "标记已读")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setIsRead("1");
            notificationRepository.save(n);
        });
        return Result.success();
    }

    @PutMapping("/read-all")
    @Transactional
    @OperationLog(module = "消息通知", content = "全部标记已读")
    public Result<Void> markAllAsRead() {
        Long userId = securityUtils.getCurrentUserId();
        notificationRepository.findByUserIdAndIsReadOrderByCreateTimeDesc(userId, "0")
                .forEach(n -> {
                    n.setIsRead("1");
                    notificationRepository.save(n);
                });
        return Result.success();
    }
}
