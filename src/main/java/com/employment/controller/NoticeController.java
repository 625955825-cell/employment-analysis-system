package com.employment.controller;

import com.employment.common.PageResult;
import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.entity.NoticeReadRecord;
import com.employment.model.entity.SysNotice;
import com.employment.repository.NoticeReadRecordRepository;
import com.employment.repository.SysNoticeRepository;
import com.employment.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final SysNoticeRepository noticeRepository;
    private final NoticeReadRecordRepository noticeReadRecordRepository;
    private final SecurityUtils securityUtils;

    @GetMapping("/list")
    public Result<PageResult<SysNotice>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<SysNotice> pageData = noticeRepository.findAll(
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishTime")));
        return Result.success(new PageResult<>(pageData.getTotalElements(), pageData.getContent()));
    }

    @GetMapping("/my-notices")
    public Result<List<Map<String, Object>>> getMyNotices() {
        String role = securityUtils.getCurrentRole();
        Long userId = securityUtils.getCurrentUserId();

        List<SysNotice> all = noticeRepository.findAll(
                Sort.by(Sort.Direction.DESC, "publishTime"));

        // 获取当前用户已读公告ID集合
        Set<Long> readNoticeIds = new HashSet<>();
        for (NoticeReadRecord record : noticeReadRecordRepository.findByUserId(userId)) {
            readNoticeIds.add(record.getNoticeId());
        }

        List<Map<String, Object>> notices = new ArrayList<>();
        for (SysNotice n : all) {
            if (!"published".equals(n.getStatus())) continue;
            String roles = n.getTargetRoles();
            if (roles == null || roles.isEmpty() || "all".equalsIgnoreCase(roles)) {
                // 全部可见
            } else {
                String[] roleArr = roles.split(",");
                boolean match = false;
                for (String r : roleArr) {
                    if (r.trim().equals(role)) {
                        match = true;
                        break;
                    }
                }
                if (!match) continue;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", n.getId());
            map.put("title", n.getTitle());
            map.put("content", n.getContent());
            map.put("noticeType", n.getNoticeType());
            map.put("publisherName", n.getPublisherName());
            map.put("publishTime", n.getPublishTime());
            map.put("topStatus", n.getTopStatus());
            map.put("viewCount", n.getViewCount());
            map.put("isRead", readNoticeIds.contains(n.getId()));
            map.put("images", n.getImages());
            notices.add(map);
        }
        return Result.success(notices);
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Object>> getUnreadCount() {
        String role = securityUtils.getCurrentRole();
        Long userId = securityUtils.getCurrentUserId();

        // 获取当前用户已读公告ID集合
        Set<Long> readNoticeIds = new HashSet<>();
        for (NoticeReadRecord record : noticeReadRecordRepository.findByUserId(userId)) {
            readNoticeIds.add(record.getNoticeId());
        }

        List<SysNotice> all = noticeRepository.findAll(
                Sort.by(Sort.Direction.DESC, "publishTime"));

        long totalPublished = 0;
        long unreadCount = 0;
        for (SysNotice n : all) {
            if (!"published".equals(n.getStatus())) continue;
            String roles = n.getTargetRoles();
            boolean visible = (roles == null || roles.isEmpty() || "all".equalsIgnoreCase(roles));
            if (!visible) {
                String[] roleArr = roles.split(",");
                for (String r : roleArr) {
                    if (r.trim().equals(role)) {
                        visible = true;
                        break;
                    }
                }
            }
            if (visible) {
                totalPublished++;
                if (!readNoticeIds.contains(n.getId())) {
                    unreadCount++;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("count", unreadCount);
        result.put("total", totalPublished);
        return Result.success(result);
    }

    @PostMapping
    @Transactional
    @OperationLog(module = "公告管理", content = "发布公告")
    public Result<SysNotice> create(@RequestBody SysNotice notice) {
        Long userId = securityUtils.getCurrentUserId();
        String username = securityUtils.getCurrentUsername();
        notice.setId(null);
        notice.setPublisherId(userId);
        notice.setPublisherName(username);
        notice.setStatus("published");
        notice.setViewCount(0);
        return Result.success(noticeRepository.save(notice));
    }

    @PutMapping("/{id}")
    @Transactional
    @OperationLog(module = "公告管理", content = "编辑公告")
    public Result<SysNotice> update(@PathVariable Long id, @RequestBody SysNotice notice) {
        SysNotice existing = noticeRepository.findById(id)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "公告不存在"));
        if (notice.getTitle() != null) existing.setTitle(notice.getTitle());
        if (notice.getContent() != null) existing.setContent(notice.getContent());
        if (notice.getNoticeType() != null) existing.setNoticeType(notice.getNoticeType());
        if (notice.getTopStatus() != null) existing.setTopStatus(notice.getTopStatus());
        if (notice.getStatus() != null) existing.setStatus(notice.getStatus());
        if (notice.getTargetRoles() != null) existing.setTargetRoles(notice.getTargetRoles());
        if (notice.getImages() != null) existing.setImages(notice.getImages());
        return Result.success(noticeRepository.save(existing));
    }

    @PutMapping("/{id}/top")
    @Transactional
    @OperationLog(module = "公告管理", content = "设置置顶")
    public Result<Void> toggleTop(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String topStatus = body.get("topStatus");
        noticeRepository.findById(id).ifPresent(n -> {
            n.setTopStatus(topStatus);
            noticeRepository.save(n);
        });
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Transactional
    @OperationLog(module = "公告管理", content = "删除公告")
    public Result<Void> delete(@PathVariable Long id) {
        noticeRepository.deleteById(id);
        return Result.success();
    }

    @PutMapping("/{id}/read")
    @Transactional
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        SysNotice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "公告不存在"));

        // 如果已读过，直接返回
        if (noticeReadRecordRepository.findByNoticeIdAndUserId(id, userId).isPresent()) {
            return Result.success();
        }

        // 记录已读
        NoticeReadRecord record = new NoticeReadRecord();
        record.setNoticeId(id);
        record.setUserId(userId);
        record.setReadTime(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        noticeReadRecordRepository.save(record);

        // 更新浏览数
        notice.setViewCount(notice.getViewCount() == null ? 1 : notice.getViewCount() + 1);
        noticeRepository.save(notice);

        return Result.success();
    }
}
