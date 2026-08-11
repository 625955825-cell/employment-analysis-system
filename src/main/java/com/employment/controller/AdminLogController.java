package com.employment.controller;

import com.employment.common.Constants;
import com.employment.common.PageResult;
import com.employment.common.Result;
import com.employment.model.entity.SysLog;
import com.employment.repository.SysLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final SysLogRepository logRepository;

    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String logType) {
        // 按条件过滤后取分页
        List<SysLog> filtered;
        if (logType != null && !logType.isEmpty()) {
            filtered = logRepository.findByLogType(logType,
                    org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE,
                            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createTime"))).getContent();
        } else {
            filtered = logRepository.findAll(
                    org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE,
                            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createTime"))).getContent();
        }
        if (keyword != null && !keyword.isEmpty()) {
            String kw = keyword.toLowerCase();
            filtered = filtered.stream()
                    .filter(l -> (l.getUsername() != null && l.getUsername().toLowerCase().contains(kw)) ||
                            (l.getDescription() != null && l.getDescription().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }
        int total = filtered.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<SysLog> pageData = start < total ? filtered.subList(start, end) : Collections.emptyList();

        List<Map<String, Object>> records = pageData.stream().map(l -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", l.getId());
            map.put("username", l.getUsername());
            map.put("logType", l.getLogType());
            map.put("module", l.getModule());
            map.put("description", l.getDescription());
            map.put("method", l.getMethod());
            map.put("url", l.getUrl());
            map.put("ip", l.getIp());
            map.put("status", l.getStatus());
            map.put("errorMsg", l.getErrorMsg());
            map.put("costTime", l.getCostTime());
            map.put("createTime", l.getCreateTime());
            return map;
        }).collect(Collectors.toList());

        return Result.success(new PageResult<>(Long.valueOf(total), records));
    }

    @GetMapping("/types")
    public Result<List<Map<String, String>>> types() {
        List<Map<String, String>> types = new ArrayList<>();
        Map<String, String> t1 = new HashMap<>();
        t1.put("value", Constants.LOG_TYPE_LOGIN);
        t1.put("label", "登录日志");
        types.add(t1);
        Map<String, String> t2 = new HashMap<>();
        t2.put("value", Constants.LOG_TYPE_LOGOUT);
        t2.put("label", "登出日志");
        types.add(t2);
        Map<String, String> t3 = new HashMap<>();
        t3.put("value", Constants.LOG_TYPE_OPERATION);
        t3.put("label", "操作日志");
        types.add(t3);
        return Result.success(types);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id) {
        logRepository.deleteById(id);
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/clear")
    @Transactional
    public Result<Void> clear() {
        logRepository.deleteAll();
        return Result.success("日志清空成功", null);
    }
}
