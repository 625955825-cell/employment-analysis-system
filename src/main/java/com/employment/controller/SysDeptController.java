package com.employment.controller;

import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.entity.SysDept;
import com.employment.repository.SysDeptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/depts")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptRepository sysDeptRepository;

    @GetMapping("/list")
    public Result<List<SysDept>> list() {
        return Result.success(sysDeptRepository.findAll());
    }

    @GetMapping("/{id}")
    public Result<SysDept> getById(@PathVariable Long id) {
        return Result.success(sysDeptRepository.findById(id).orElse(null));
    }

    @PostMapping
    @Transactional
    @OperationLog(module = "院系管理", content = "添加院系")
    public Result<SysDept> save(@RequestBody SysDept dept) {
        dept.setId(null);
        return Result.success("保存成功", sysDeptRepository.save(dept));
    }

    @PutMapping("/{id}")
    @Transactional
    @OperationLog(module = "院系管理", content = "编辑院系")
    public Result<SysDept> update(@PathVariable Long id, @RequestBody SysDept dept) {
        SysDept existing = sysDeptRepository.findById(id).orElseThrow();
        existing.setDeptName(dept.getDeptName());
        existing.setIsTopLevel(dept.getIsTopLevel());
        existing.setSort(dept.getSort());
        existing.setStatus(dept.getStatus());
        existing.setRemark(dept.getRemark());
        return Result.success("更新成功", sysDeptRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @OperationLog(module = "院系管理", content = "删除院系")
    public Result<?> delete(@PathVariable Long id) {
        sysDeptRepository.deleteById(id);
        return Result.success("删除成功", null);
    }
}
