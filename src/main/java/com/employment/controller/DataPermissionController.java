package com.employment.controller;

import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.entity.DataPermissionRequest;
import com.employment.model.entity.EmploymentRecord;
import com.employment.model.entity.StudentInfo;
import com.employment.repository.DataPermissionRequestRepository;
import com.employment.repository.EmploymentRecordRepository;
import com.employment.repository.StudentInfoRepository;
import com.employment.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/data-permission")
@RequiredArgsConstructor
public class DataPermissionController {

    private final DataPermissionRequestRepository requestRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final EmploymentRecordRepository employmentRecordRepository;
    private final SecurityUtils securityUtils;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId).orElse(null);
        if (studentInfo == null) {
            return Result.success(Collections.emptyList());
        }

        List<DataPermissionRequest> requests = requestRepository.findByStudentId(studentInfo.getId());
        // 预加载所有院系名称用于匹配
        Map<Long, String> deptNameMap = new HashMap<>();
        for (DataPermissionRequest r : requests) {
            if (r.getDeptId() != null && !deptNameMap.containsKey(r.getDeptId())) {
                // 通过 studentInfo 查询其所属院系名称
                studentInfoRepository.findById(r.getStudentId())
                        .map(s -> s.getDeptName()).ifPresent(name -> deptNameMap.put(r.getDeptId(), name));
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (DataPermissionRequest r : requests) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("studentId", r.getStudentId());
            map.put("deptId", r.getDeptId());
            map.put("deptName", deptNameMap.get(r.getDeptId()));
            map.put("majorId", r.getMajorId());
            map.put("requestType", r.getRequestType());
            map.put("yearFrom", r.getYearFrom());
            map.put("yearTo", r.getYearTo());
            map.put("reason", r.getReason());
            map.put("status", r.getStatus());
            map.put("auditRemark", r.getAuditRemark());
            map.put("auditTime", r.getAuditTime());
            map.put("createTime", r.getCreateTime());
            result.add(map);
        }
        return Result.success(result);
    }

    @PostMapping("/apply")
    @Transactional
    @OperationLog(module = "数据权限", content = "提交数据权限申请")
    public Result<Void> apply(@RequestBody Map<String, Object> data) {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "学生信息不存在"));

        DataPermissionRequest request = new DataPermissionRequest();
        request.setStudentId(studentInfo.getId());
        // 存储申请院系和专业（可空，前端展示用）
        if (data.get("deptId") != null) {
            request.setDeptId(Long.valueOf(data.get("deptId").toString()));
        }
        if (data.get("majorId") != null) {
            request.setMajorId(Long.valueOf(data.get("majorId").toString()));
        }
        request.setRequestType((String) data.get("requestType"));
        if (data.get("yearFrom") != null) {
            request.setYearFrom(((Number) data.get("yearFrom")).intValue());
        }
        if (data.get("yearTo") != null) {
            request.setYearTo(((Number) data.get("yearTo")).intValue());
        }
        request.setReason((String) data.get("reason"));
        request.setStatus("pending");
        requestRepository.save(request);
        return Result.success("申请提交成功", null);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        DataPermissionRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "申请记录不存在"));
        Map<String, Object> map = new HashMap<>();
        map.put("id", request.getId());
        map.put("studentId", request.getStudentId());
        map.put("deptId", request.getDeptId());
        map.put("majorId", request.getMajorId());
        map.put("requestType", request.getRequestType());
        map.put("yearFrom", request.getYearFrom());
        map.put("yearTo", request.getYearTo());
        map.put("reason", request.getReason());
        map.put("status", request.getStatus());
        map.put("auditRemark", request.getAuditRemark());
        map.put("auditTime", request.getAuditTime());
        map.put("createTime", request.getCreateTime());
        return Result.success(map);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> cancelRequest(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "学生信息不存在"));

        DataPermissionRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "申请记录不存在"));

        // 只能取消自己的申请
        if (!request.getStudentId().equals(studentInfo.getId())) {
            throw new com.employment.exception.BusinessException(403, "无权操作");
        }
        // 只能取消待审核的申请
        if (!"pending".equals(request.getStatus())) {
            throw new com.employment.exception.BusinessException(400, "只能取消待审核的申请");
        }

        requestRepository.deleteById(id);
        return Result.success("已取消申请", null);
    }

    /**
     * 学生查看已审批通过的就业数据
     * 根据已批准的申请返回对应院系/年份范围的就业数据
     */
    @GetMapping("/data")
    public Result<Map<String, Object>> getApprovedData(@RequestParam Long requestId) {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "学生信息不存在"));

        DataPermissionRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "申请记录不存在"));

        if (!request.getStudentId().equals(studentInfo.getId())) {
            throw new com.employment.exception.BusinessException(403, "无权查看该申请的数据");
        }
        if (!"approved".equals(request.getStatus())) {
            throw new com.employment.exception.BusinessException(400, "该申请尚未通过审批，无法查看数据");
        }

        Integer yearFrom = request.getYearFrom();
        Integer yearTo = request.getYearTo();

        // 1. 精确查询目标学生（避免 findAll 全表扫描）
        List<StudentInfo> targetStudents;
        boolean hasYearFilter = yearFrom != null && yearTo != null;
        if (hasYearFilter) {
            // 两者都指定时走索引查询
            targetStudents = request.getDeptId() != null
                    ? studentInfoRepository.findByDeptIdAndGraduationYear(request.getDeptId(), yearFrom)
                            .stream().filter(s -> s.getGraduationYear() != null && s.getGraduationYear() <= yearTo).collect(Collectors.toList())
                    : studentInfoRepository.findByClassIdAndGraduationYear(null, yearFrom)
                            .stream().filter(s -> s.getGraduationYear() != null && s.getGraduationYear() <= yearTo).collect(Collectors.toList());
            // findByClassIdAndGraduationYear 返回空列表的兜底：直接用 findAll 过滤（仅在年份范围场景）
            if (targetStudents.isEmpty()) {
                targetStudents = studentInfoRepository.findAll().stream()
                        .filter(s -> s.getGraduationYear() != null && s.getGraduationYear() >= yearFrom && s.getGraduationYear() <= yearTo
                                && (request.getDeptId() == null || (s.getDeptId() != null && s.getDeptId().equals(request.getDeptId()))))
                        .collect(Collectors.toList());
            }
        } else {
            // 无年份过滤，或只填了单边
            targetStudents = request.getDeptId() != null
                    ? studentInfoRepository.findByDeptId(request.getDeptId())
                    : studentInfoRepository.findAll();
            if (yearFrom != null) {
                final int yf = yearFrom;
                targetStudents = targetStudents.stream()
                        .filter(s -> s.getGraduationYear() != null && s.getGraduationYear() >= yf)
                        .collect(Collectors.toList());
            } else if (yearTo != null) {
                final int yt = yearTo;
                targetStudents = targetStudents.stream()
                        .filter(s -> s.getGraduationYear() != null && s.getGraduationYear() <= yt)
                        .collect(Collectors.toList());
            }
        }

        Set<Long> targetStudentIds = targetStudents.stream()
                .map(StudentInfo::getId).collect(Collectors.toSet());

        // 2. 按学生ID批量查询，只加载目标学生的就业记录（避免 findAll 全表扫描）
        List<EmploymentRecord> allRecords = targetStudentIds.isEmpty()
                ? List.of()
                : employmentRecordRepository.findByStudentIdIn(new ArrayList<>(targetStudentIds));
        Map<Long, EmploymentRecord> latestByStudent = new LinkedHashMap<>();
        for (EmploymentRecord r : allRecords) {
            if (!targetStudentIds.contains(r.getStudentId())) continue;
            EmploymentRecord existing = latestByStudent.get(r.getStudentId());
            if (existing == null || (r.getCreateTime() != null && existing.getCreateTime() != null
                    && r.getCreateTime().compareTo(existing.getCreateTime()) > 0)) {
                latestByStudent.put(r.getStudentId(), r);
            }
        }

        long totalStudents = targetStudents.size();
        long employed = latestByStudent.values().stream()
                .filter(r -> "approved".equals(r.getAuditStatus())).count();
        long graduateSchool = 0, military = 0, selfEmployed = 0, abroad = 0;

        for (EmploymentRecord r : latestByStudent.values()) {
            if (!"approved".equals(r.getAuditStatus())) continue;
            String empType = r.getEmploymentType();
            if ("继续深造".equals(empType) || "升学".equals(empType)) graduateSchool++;
            else if ("应征入伍".equals(empType) || "入伍".equals(empType) || "服兵役".equals(empType)) military++;
            else if ("自主创业".equals(empType) || "创业".equals(empType)) selfEmployed++;
            else if ("出国出境".equals(empType) || "出国".equals(empType) || "境外".equals(empType)) abroad++;
        }

        // 薪资分布
        long[] salaryRanges = {0, 0, 0, 0, 0};
        String[] salaryLabels = {"5k以下", "5k-8k", "8k-12k", "12k-20k", "20k以上"};
        for (EmploymentRecord r : latestByStudent.values()) {
            if (!"approved".equals(r.getAuditStatus()) || r.getSalary() == null) continue;
            try {
                String sal = r.getSalary().replaceAll("[^\\d.]", "");
                double salary = Double.parseDouble(sal);
                if (salary < 5000) salaryRanges[0]++;
                else if (salary < 8000) salaryRanges[1]++;
                else if (salary < 12000) salaryRanges[2]++;
                else if (salary < 20000) salaryRanges[3]++;
                else salaryRanges[4]++;
            } catch (Exception ignored) {}
        }
        Map<String, Long> salaryDist = new LinkedHashMap<>();
        for (int i = 0; i < salaryLabels.length; i++) {
            salaryDist.put(salaryLabels[i], salaryRanges[i]);
        }

        // 行业分布
        Map<String, Long> industryCount = new LinkedHashMap<>();
        for (EmploymentRecord r : latestByStudent.values()) {
            if (!"approved".equals(r.getAuditStatus()) || r.getCompanyIndustry() == null) continue;
            industryCount.put(r.getCompanyIndustry(), industryCount.getOrDefault(r.getCompanyIndustry(), 0L) + 1);
        }
        List<Map<String, Object>> industryDistribution = industryCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("value", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        // 省份分布
        Map<String, Long> provinceCount = new LinkedHashMap<>();
        for (EmploymentRecord r : latestByStudent.values()) {
            if (!"approved".equals(r.getAuditStatus()) || r.getWorkProvince() == null) continue;
            provinceCount.put(r.getWorkProvince(), provinceCount.getOrDefault(r.getWorkProvince(), 0L) + 1);
        }
        List<Map<String, Object>> provinceDistribution = provinceCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(15)
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("value", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestId", requestId);
        result.put("requestType", request.getRequestType());
        result.put("deptId", request.getDeptId());
        result.put("yearFrom", yearFrom);
        result.put("yearTo", yearTo);
        result.put("totalStudents", totalStudents);
        result.put("employed", employed);
        result.put("unemployed", totalStudents - employed);
        result.put("employmentRate", totalStudents > 0 ? Math.round((double) employed / totalStudents * 10000) / 100.0 : 0.0);

        Map<String, Long> specialTypes = new LinkedHashMap<>();
        specialTypes.put("继续深造", graduateSchool);
        specialTypes.put("应征入伍", military);
        specialTypes.put("自主创业", selfEmployed);
        specialTypes.put("出国出境", abroad);
        result.put("specialTypes", specialTypes);

        result.put("salaryDistribution", salaryDist);
        result.put("industryDistribution", industryDistribution);
        result.put("provinceDistribution", provinceDistribution);

        return Result.success(result);
    }
}
