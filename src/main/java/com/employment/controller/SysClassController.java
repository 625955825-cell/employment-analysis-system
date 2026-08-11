package com.employment.controller;

import com.employment.common.PageResult;
import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.dto.BatchGenerateClassRequest;
import com.employment.model.entity.SysClass;
import com.employment.repository.SysClassRepository;
import com.employment.service.SysClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/classes")
@RequiredArgsConstructor
public class SysClassController {

    private final SysClassService sysClassService;
    private final SysClassRepository sysClassRepository;

    @GetMapping("/list")
    public Result<PageResult<SysClass>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long majorId) {
        return Result.success(sysClassService.list(keyword, deptId, majorId, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<SysClass> getById(@PathVariable Long id) {
        return Result.success(sysClassService.getById(id));
    }

    @PostMapping
    @OperationLog(module = "班级管理", content = "添加班级")
    public Result<SysClass> save(@RequestBody SysClass sysClass) {
        return Result.success("保存成功", sysClassService.save(sysClass));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "班级管理", content = "编辑班级")
    public Result<SysClass> update(@PathVariable Long id, @RequestBody SysClass sysClass) {
        return Result.success("更新成功", sysClassService.update(id, sysClass));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "班级管理", content = "删除班级")
    public Result<?> delete(@PathVariable Long id) {
        sysClassService.delete(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/by-major")
    public Result<List<Map<String, Object>>> getByMajorId(@RequestParam(required = false) Long majorId) {
        return Result.success(sysClassService.getByMajorId(majorId));
    }

    @GetMapping("/all")
    public Result<List<Map<String, Object>>> getAllClasses() {
        return Result.success(sysClassService.getAllClasses());
    }

    @PostMapping("/batch-generate")
    @OperationLog(module = "班级管理", content = "批量生成班级")
    public Result<?> batchGenerate(@RequestBody BatchGenerateClassRequest request) {
        List<SysClass> generated = sysClassService.batchGenerateClasses(request);
        return Result.success("批量生成成功，共生成 " + generated.size() + " 个班级", generated);
    }

    @DeleteMapping("/batch-by-grade")
    @OperationLog(module = "班级管理", content = "按年级批量删除班级")
    public Result<?> batchDeleteByGrade(@RequestParam String grade) {
        int count = sysClassRepository.findByGrade(grade).size();
        sysClassService.batchDeleteByGrade(grade);
        return Result.success("删除成功，已删除 " + count + " 个班级及其所属学生", null);
    }
}
