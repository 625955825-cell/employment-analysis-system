package com.employment.controller;

import com.employment.common.Result;
import com.employment.model.entity.*;
import com.employment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final SysUserRepository sysUserRepository;
    private final CompanyInfoRepository companyInfoRepository;
    private final JobPositionRepository jobPositionRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final EmploymentRecordRepository employmentRecordRepository;
    private final SysDeptRepository sysDeptRepository;
    private final SysMajorRepository sysMajorRepository;
    private final SysClassRepository sysClassRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final SysNoticeRepository sysNoticeRepository;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        long totalUsers = sysUserRepository.count();
        long approvedCompanies = companyInfoRepository.countByAuthStatus("approved");
        long publishedJobs = jobPositionRepository.countByStatusAndIsDeleted("published", "0");

        long totalStudents = studentInfoRepository.count();
        long totalEmployment = employmentRecordRepository.count();

        double employmentRate = 0.0;
        if (totalStudents > 0) {
            employmentRate = Math.round((double) totalEmployment / totalStudents * 10000.0) / 100.0;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("totalCompanies", approvedCompanies);
        data.put("totalJobs", publishedJobs);
        data.put("employmentRate", employmentRate);
        data.put("totalStudents", totalStudents);
        data.put("totalEmployment", totalEmployment);

        return Result.success(data);
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        long deptCount = sysDeptRepository.count();
        long majorCount = sysMajorRepository.count();
        long classCount = sysClassRepository.count();
        long noticeCount = sysNoticeRepository.count();
        long applicationCount = jobApplicationRepository.count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deptCount", deptCount);
        data.put("majorCount", majorCount);
        data.put("classCount", classCount);
        data.put("noticeCount", noticeCount);
        data.put("applicationCount", applicationCount);

        return Result.success(data);
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 各角色用户分布
        List<Object[]> roleCounts = employmentRecordRepository.countByEmploymentType();
        Map<String, Long> employmentTypeMap = new LinkedHashMap<>();
        for (Object[] row : roleCounts) {
            employmentTypeMap.put(row[0] == null ? "未知" : row[0].toString(), (Long) row[1]);
        }
        data.put("employmentTypeDistribution", employmentTypeMap);

        // 各院系学生分布
        List<Object[]> deptCounts = studentInfoRepository.countByDept();
        List<Map<String, Object>> studentDeptChart = new ArrayList<>();
        for (Object[] row : deptCounts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row[0] == null ? "未知" : row[0].toString());
            item.put("value", ((Number) row[1]).longValue());
            studentDeptChart.add(item);
        }
        data.put("studentDeptDistribution", studentDeptChart);

        // 就业省份分布
        List<Object[]> provinceCounts = employmentRecordRepository.countByWorkProvince();
        List<Map<String, Object>> provinceChart = new ArrayList<>();
        for (Object[] row : provinceCounts) {
            if (row[0] != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", row[0].toString());
                item.put("value", ((Number) row[1]).longValue());
                provinceChart.add(item);
            }
        }
        data.put("employmentProvinceDistribution", provinceChart);

        // 行业分布（企业）
        List<Object[]> industryCounts = companyInfoRepository.countApprovedByIndustry();
        List<Map<String, Object>> industryChart = new ArrayList<>();
        for (Object[] row : industryCounts) {
            if (row[0] != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", row[0].toString());
                item.put("value", ((Number) row[1]).longValue());
                industryChart.add(item);
            }
        }
        data.put("companyIndustryDistribution", industryChart);

        // 各专业学生分布
        List<Object[]> majorCounts = studentInfoRepository.countByMajor();
        List<Map<String, Object>> majorChart = new ArrayList<>();
        for (Object[] row : majorCounts) {
            if (row[0] != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", row[0].toString());
                item.put("value", ((Number) row[1]).longValue());
                majorChart.add(item);
            }
        }
        data.put("studentMajorDistribution", majorChart);

        return Result.success(data);
    }
}
