package com.employment.controller;

import com.employment.common.Result;
import com.employment.repository.SysClassRepository;
import com.employment.repository.SysDeptRepository;
import com.employment.repository.SysMajorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 已禁用：自动初始化接口已移除。
 * 所有数据由 init.sql 导入，不再通过后端初始化。
 */
@RestController
@RequestMapping("/api/admin/auto-init")
@RequiredArgsConstructor
public class AutoInitController {

    private final SysDeptRepository deptRepository;
    private final SysMajorRepository majorRepository;
    private final SysClassRepository sysClassRepository;

    /**
     * @deprecated 已禁用，请使用 init.sql 导入数据
     */
    @Deprecated
    @PostMapping
    public Result<Map<String, Object>> autoInit() {
        return Result.error("自动初始化已禁用，请使用 init.sql 导入数据");
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> getInitStatus() {
        long deptCount = deptRepository.count();
        long majorCount = majorRepository.count();
        long classCount = sysClassRepository.count();
        boolean hasData = deptCount > 0 || majorCount > 0 || classCount > 0;

        Map<String, Object> data = new HashMap<>();
        data.put("deptCount", deptCount);
        data.put("majorCount", majorCount);
        data.put("classCount", classCount);
        data.put("hasData", hasData);
        data.put("message", "自动初始化已禁用，数据由 init.sql 导入");
        return Result.success(data);
    }

    /**
     * @deprecated 已禁用，请使用 init.sql 导入数据
     */
    @Deprecated
    @PostMapping("/reset")
    public Result<Map<String, Object>> resetAndInit() {
        return Result.error("自动初始化已禁用，请使用 init.sql 导入数据");
    }
}
