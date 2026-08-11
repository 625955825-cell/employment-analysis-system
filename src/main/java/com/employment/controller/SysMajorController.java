package com.employment.controller;

import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.entity.SysDept;
import com.employment.model.entity.SysMajor;
import com.employment.repository.SysDeptRepository;
import com.employment.repository.SysMajorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/majors")
@RequiredArgsConstructor
public class SysMajorController {

    private final SysMajorRepository sysMajorRepository;
    private final SysDeptRepository sysDeptRepository;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<SysMajor> page = sysMajorRepository.findAll(PageRequest.of(pageNum - 1, pageSize));
        Map<Long, String> deptMap = sysDeptRepository.findAll().stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName));
        for (SysMajor m : page.getContent()) {
            m.setDeptName(deptMap.get(m.getDeptId()));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getContent());
        data.put("total", page.getTotalElements());
        return Result.success(data);
    }

    @GetMapping("/all")
    public Result<List<SysMajor>> getAllMajors() {
        List<SysMajor> majors = sysMajorRepository.findAll();
        Map<Long, String> deptMap = sysDeptRepository.findAll().stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName));
        for (SysMajor m : majors) {
            m.setDeptName(deptMap.get(m.getDeptId()));
        }
        return Result.success(majors);
    }

    @GetMapping("/by-dept")
    public Result<List<SysMajor>> byDept(@RequestParam Long deptId) {
        List<SysMajor> majors = sysMajorRepository.findByDeptId(deptId);
        for (SysMajor m : majors) {
            m.setDeptName(sysDeptRepository.findById(deptId).map(SysDept::getDeptName).orElse(null));
        }
        return Result.success(majors);
    }

    @GetMapping("/{id}")
    public Result<SysMajor> getById(@PathVariable Long id) {
        return Result.success(sysMajorRepository.findById(id).orElse(null));
    }

    @PostMapping
    @Transactional
    @OperationLog(module = "专业管理", content = "添加专业")
    public Result<SysMajor> save(@RequestBody SysMajor major) {
        major.setId(null);
        if (major.getRecommendEnabled() == null) {
            major.setRecommendEnabled("1");
        }
        return Result.success("保存成功", sysMajorRepository.save(major));
    }

    @PutMapping("/{id}")
    @Transactional
    @OperationLog(module = "专业管理", content = "编辑专业")
    public Result<SysMajor> update(@PathVariable Long id, @RequestBody SysMajor major) {
        SysMajor existing = sysMajorRepository.findById(id).orElseThrow(() -> new RuntimeException("专业不存在"));
        existing.setMajorName(major.getMajorName());
        existing.setDeptId(major.getDeptId());
        existing.setIsTopLevel(major.getIsTopLevel());
        existing.setShortName(major.getShortName());
        existing.setRemark(major.getRemark());
        if (major.getRecommendEnabled() != null) {
            existing.setRecommendEnabled(major.getRecommendEnabled());
        }
        return Result.success("更新成功", sysMajorRepository.save(existing));
    }

    @PutMapping("/{id}/recommend-enabled")
    @Transactional
    @OperationLog(module = "专业管理", content = "设置推荐开关")
    public Result<?> setRecommendEnabled(@PathVariable Long id, @RequestBody Map<String, String> body) {
        SysMajor existing = sysMajorRepository.findById(id).orElseThrow(() -> new RuntimeException("专业不存在"));
        existing.setRecommendEnabled(body.get("recommendEnabled"));
        sysMajorRepository.save(existing);
        return Result.success("设置成功", null);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @OperationLog(module = "专业管理", content = "删除专业")
    public Result<?> delete(@PathVariable Long id) {
        sysMajorRepository.deleteById(id);
        return Result.success("删除成功", null);
    }
}
