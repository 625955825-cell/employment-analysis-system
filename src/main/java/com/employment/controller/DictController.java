package com.employment.controller;

import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.service.DataInitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class DictController {

    private final DataInitService dataInitService;

    @PostMapping("/init")
    @OperationLog(module = "系统管理", content = "初始化基础数据")
    public Result<String> initData() {
        dataInitService.initRoles();
        dataInitService.initDepartments();
        dataInitService.initMajors();
        dataInitService.initAdminUser();
        return Result.success("数据初始化完成");
    }

    @GetMapping("/departments")
    public Result<List<Map<String, Object>>> getDepartments() {
        return Result.success(dataInitService.getDeptTree());
    }

    @GetMapping("/majors")
    public Result<List<Map<String, Object>>> getMajors(@RequestParam Long deptId) {
        List<Map<String, Object>> majors = dataInitService.getMajorsByDeptId(deptId);
        log.info("[DictController] getMajors deptId={}, 返回 {} 条专业", deptId, majors.size());
        return Result.success(majors);
    }
}
