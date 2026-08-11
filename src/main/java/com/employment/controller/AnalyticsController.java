package com.employment.controller;

import com.employment.common.Result;
import com.employment.repository.*;
import com.employment.service.StatsCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final StudentInfoRepository studentInfoRepository;
    private final EmploymentRecordRepository employmentRecordRepository;
    private final SysClassRepository sysClassRepository;
    private final StatsCacheService statsCacheService;

    // ==================== 核心概览（SQL 聚合 + Redis 缓存） ====================

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(
            @RequestParam(required = false) Integer graduationYear) {
        String cacheKey = statsCacheService.analyticsKey("overview:" + (graduationYear == null ? "all" : graduationYear));
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> cached = (Map<String, Object>) statsCacheService.getAnalyticsCache(cacheKey);
            if (cached != null) return Result.success(cached);
        } catch (Exception e) {
            // Redis 降级
        }

        Map<String, Object> result = buildOverview(graduationYear);

        try {
            statsCacheService.putAnalyticsCache(cacheKey, result);
        } catch (Exception e) {
            // Redis 降级
        }
        return Result.success(result);
    }

    private Map<String, Object> buildOverview(Integer year) {
        Map<String, Object> result = new LinkedHashMap<>();
        long totalStudents = studentInfoRepository.countStudents(year);
        long employed = employmentRecordRepository.countGlobalEmployed(year);
        long pending = employmentRecordRepository.countGlobalPending(year);
        long unemployed = totalStudents - employed - pending;
        if (unemployed < 0) unemployed = 0;

        result.put("totalStudents", totalStudents);
        result.put("totalEmploymentRecords", employmentRecordRepository.countGlobalRecords(year));
        result.put("employed", employed);
        result.put("unemployed", unemployed);
        result.put("pending", pending);
        result.put("employmentRate", totalStudents > 0
                ? Math.round((double) employed / totalStudents * 10000) / 100.0 : 0.0);
        result.put("graduationYear", year);

        // 特殊类型（SQL GROUP BY）
        Map<String, Long> specialMap = new LinkedHashMap<>();
        specialMap.put("继续深造", 0L);
        specialMap.put("应征入伍", 0L);
        specialMap.put("自主创业", 0L);
        specialMap.put("出国出境", 0L);
        for (Object[] row : employmentRecordRepository.countGlobalSpecialType(year)) {
            String type = (String) row[0];
            long cnt = ((Number) row[1]).longValue();
            if ("继续深造".equals(type) || "升学".equals(type)) specialMap.put("继续深造", specialMap.get("继续深造") + cnt);
            else if ("应征入伍".equals(type) || "入伍".equals(type) || "服兵役".equals(type)) specialMap.put("应征入伍", specialMap.get("应征入伍") + cnt);
            else if ("自主创业".equals(type) || "创业".equals(type)) specialMap.put("自主创业", specialMap.get("自主创业") + cnt);
            else specialMap.put("出国出境", specialMap.get("出国出境") + cnt);
        }
        result.put("specialTypes", specialMap);

        // 近7天新增就业人数
        long recent7Days = employmentRecordRepository.countRecent7DaysApproved(year);
        result.put("recent7DaysCount", recent7Days);

        return result;
    }

    // ==================== 各院系就业率（SQL 聚合 + Redis 缓存） ====================

    @GetMapping("/dept-stats")
    public Result<List<Map<String, Object>>> getDeptStats(
            @RequestParam(required = false) Integer graduationYear) {
        String cacheKey = statsCacheService.analyticsKey("dept-stats:" + (graduationYear == null ? "all" : graduationYear));
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cached = (List<Map<String, Object>>) statsCacheService.getAnalyticsCache(cacheKey);
            if (cached != null) return Result.success(cached);
        } catch (Exception e) {}

        List<Map<String, Object>> result = buildDeptStats(graduationYear);
        try { statsCacheService.putAnalyticsCache(cacheKey, result); } catch (Exception e) {}
        return Result.success(result);
    }

    private List<Map<String, Object>> buildDeptStats(Integer year) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 班级维度人数
        Map<Long, Long> classStudentCount = new HashMap<>();
        for (Map<String, Object> m : sysClassRepository.countStudentsByClassId()) {
            Number classIdNum = (Number) m.get("classId");
            Number countNum = (Number) m.get("studentCount");
            if (classIdNum != null && countNum != null) {
                classStudentCount.put(classIdNum.longValue(), countNum.longValue());
            }
        }

        // 按院系分组学生人数（SQL）
        List<Object[]> deptRows = studentInfoRepository.countGroupByDept(year);
        Map<Long, Long> deptStudentCount = new LinkedHashMap<>();
        for (Object[] row : deptRows) {
            long deptId = ((Number) row[0]).longValue();
            long cnt = ((Number) row[2]).longValue();
            deptStudentCount.put(deptId, cnt);
        }

        // 按院系统计已就业（SQL）
        Map<Long, Long> deptEmployed = new HashMap<>();
        for (Object[] row : employmentRecordRepository.countEmployedByAllDepts(year)) {
            long deptId = ((Number) row[0]).longValue();
            long cnt = ((Number) row[1]).longValue();
            deptEmployed.put(deptId, cnt);
        }

        for (Object[] row : deptRows) {
            long deptId = ((Number) row[0]).longValue();
            String deptName = row[1] == null ? "未知院系" : (String) row[1];
            long total = ((Number) row[2]).longValue();
            long employed = deptEmployed.getOrDefault(deptId, 0L);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("deptId", deptId);
            item.put("deptName", deptName);
            item.put("totalStudents", total);
            item.put("employed", employed);
            item.put("unemployed", total - employed);
            item.put("employmentRate", total > 0
                    ? Math.round((double) employed / total * 10000) / 100.0 : 0.0);
            item.put("graduationYear", year);
            result.add(item);
        }

        result.sort((a, b) -> {
            Double ra = (Double) a.get("employmentRate");
            Double rb = (Double) b.get("employmentRate");
            return rb.compareTo(ra);
        });
        return result;
    }

    // ==================== 各班级就业率（SQL 聚合 + Redis 缓存） ====================

    @GetMapping("/class-stats")
    public Result<List<Map<String, Object>>> getClassStats(
            @RequestParam(required = false) Integer graduationYear) {
        String cacheKey = statsCacheService.analyticsKey("class-stats:" + (graduationYear == null ? "all" : graduationYear));
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cached = (List<Map<String, Object>>) statsCacheService.getAnalyticsCache(cacheKey);
            if (cached != null) return Result.success(cached);
        } catch (Exception e) {}

        List<Map<String, Object>> result = buildClassStats(graduationYear);
        try { statsCacheService.putAnalyticsCache(cacheKey, result); } catch (Exception e) {}
        return Result.success(result);
    }

    private List<Map<String, Object>> buildClassStats(Integer year) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 班级维度人数（SQL）
        Map<Long, Long> classStudentCount = new HashMap<>();
        for (Map<String, Object> m : sysClassRepository.countStudentsByClassId()) {
            Number classIdNum = (Number) m.get("classId");
            Number countNum = (Number) m.get("studentCount");
            if (classIdNum != null && countNum != null) {
                classStudentCount.put(classIdNum.longValue(), countNum.longValue());
            }
        }

        // 按班级分组学生人数（SQL）
        List<Object[]> classRows = studentInfoRepository.countGroupByClass(year);

        // 按班级统计已就业（SQL）
        Map<Long, Long> classEmployed = new HashMap<>();
        for (Object[] row : employmentRecordRepository.countEmployedByAllClasses(year)) {
            Number classIdNum = (Number) row[0];
            Number cntNum = (Number) row[1];
            if (classIdNum != null && cntNum != null) {
                classEmployed.put(classIdNum.longValue(), cntNum.longValue());
            }
        }

        for (Object[] row : classRows) {
            Number classIdNum = (Number) row[0];
            String className = row[1] == null ? "未知班级" : (String) row[1];
            String deptName = row[2] == null ? "未知院系" : (String) row[2];
            String majorName = row[3] == null ? "未知专业" : (String) row[3];
            Number totalNum = (Number) row[4];
            long total = totalNum == null ? 0 : totalNum.longValue();
            long classId = classIdNum == null ? 0 : classIdNum.longValue();
            long employed = classEmployed.getOrDefault(classId, 0L);
            long studentCount = classStudentCount.getOrDefault(classId, total);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("classId", classId);
            item.put("className", className);
            item.put("deptName", deptName);
            item.put("majorName", majorName);
            item.put("grade", null);
            item.put("totalStudents", total);
            item.put("studentCount", studentCount);
            item.put("employed", employed);
            item.put("unemployed", total - employed);
            item.put("employmentRate", total > 0
                    ? Math.round((double) employed / total * 10000) / 100.0 : 0.0);
            item.put("graduationYear", year);
            result.add(item);
        }

        result.sort((a, b) -> {
            Double ra = (Double) a.get("employmentRate");
            Double rb = (Double) b.get("employmentRate");
            return rb.compareTo(ra);
        });
        return result;
    }

    // ==================== 就业类型分布（SQL 聚合 + Redis 缓存） ====================

    @GetMapping("/employment-trend")
    public Result<Map<String, Object>> getEmploymentTrend(
            @RequestParam(required = false) Integer graduationYear) {
        String cacheKey = statsCacheService.analyticsKey("trend:" + (graduationYear == null ? "all" : graduationYear));
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> cached = (Map<String, Object>) statsCacheService.getAnalyticsCache(cacheKey);
            if (cached != null) return Result.success(cached);
        } catch (Exception e) {}

        Map<String, Object> result = buildTrend(graduationYear);
        try { statsCacheService.putAnalyticsCache(cacheKey, result); } catch (Exception e) {}
        return Result.success(result);
    }

    private Map<String, Object> buildTrend(Integer year) {
        Map<String, Object> result = new LinkedHashMap<>();

        List<Object[]> typeRows = employmentRecordRepository.countGlobalEmploymentType(year);
        Map<String, Long> typeMap = new LinkedHashMap<>();
        for (Object[] row : typeRows) {
            String type = row[0] == null ? "未知" : (String) row[0];
            long cnt = ((Number) row[1]).longValue();
            typeMap.put(type, cnt);
        }

        List<Map.Entry<String, Long>> sortedEntries = typeMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        long totalApproved = sortedEntries.stream().mapToLong(Map.Entry::getValue).sum();

        Map<String, Double> typePercent = new LinkedHashMap<>();
        for (Map.Entry<String, Long> e : sortedEntries) {
            double percent = totalApproved > 0
                    ? Math.round((double) e.getValue() / totalApproved * 10000) / 100.0 : 0.0;
            typePercent.put(e.getKey(), percent);
        }

        LinkedHashMap<String, Long> orderedTypeMap = new LinkedHashMap<>();
        for (Map.Entry<String, Long> e : sortedEntries) {
            orderedTypeMap.put(e.getKey(), e.getValue());
        }

        result.put("typeCount", orderedTypeMap);
        result.put("typePercent", typePercent);
        result.put("totalApproved", totalApproved);
        result.put("graduationYear", year);
        return result;
    }

    // ==================== 薪资分布（SQL 聚合 + Redis 缓存） ====================

    @GetMapping("/salary-dist")
    public Result<Map<String, Object>> getSalaryDist(
            @RequestParam(required = false) Integer graduationYear) {
        String cacheKey = statsCacheService.analyticsKey("salary:" + (graduationYear == null ? "all" : graduationYear));
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> cached = (Map<String, Object>) statsCacheService.getAnalyticsCache(cacheKey);
            if (cached != null) return Result.success(cached);
        } catch (Exception e) {}

        Map<String, Object> result = buildSalary(graduationYear);
        try { statsCacheService.putAnalyticsCache(cacheKey, result); } catch (Exception e) {}
        return Result.success(result);
    }

    private Map<String, Object> buildSalary(Integer year) {
        Map<String, Object> result = new LinkedHashMap<>();
        String[] labels = {"5k以下", "5k-8k", "8k-12k", "12k-20k", "20k以上"};
        long[] ranges = new long[5];
        long totalWithSalary = 0;

        List<Object[]> salaryRows = employmentRecordRepository.getGlobalSalaryData(year);
        for (Object[] row : salaryRows) {
            String sal = (String) row[0];
            if (sal == null || sal.isEmpty()) continue;
            totalWithSalary++;
            try {
                double salary = Double.parseDouble(sal.replaceAll("[^\\d.]", ""));
                if (salary < 5000) ranges[0]++;
                else if (salary < 8000) ranges[1]++;
                else if (salary < 12000) ranges[2]++;
                else if (salary < 20000) ranges[3]++;
                else ranges[4]++;
            } catch (Exception ignored) {}
        }

        Map<String, Long> dist = new LinkedHashMap<>();
        for (int i = 0; i < labels.length; i++) dist.put(labels[i], ranges[i]);

        result.put("distribution", dist);
        result.put("totalWithSalary", totalWithSalary);
        result.put("graduationYear", year);
        return result;
    }

    // ==================== 行业分布（SQL 聚合 + Redis 缓存） ====================

    @GetMapping("/industry-dist")
    public Result<Map<String, Object>> getIndustryDist(
            @RequestParam(required = false) Integer graduationYear) {
        String cacheKey = statsCacheService.analyticsKey("industry:" + (graduationYear == null ? "all" : graduationYear));
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> cached = (Map<String, Object>) statsCacheService.getAnalyticsCache(cacheKey);
            if (cached != null) return Result.success(cached);
        } catch (Exception e) {}

        Map<String, Object> result = buildIndustry(graduationYear);
        try { statsCacheService.putAnalyticsCache(cacheKey, result); } catch (Exception e) {}
        return Result.success(result);
    }

    private Map<String, Object> buildIndustry(Integer year) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> chartData = new ArrayList<>();
        long total = 0;

        List<Object[]> rows = employmentRecordRepository.countGlobalIndustry(year);
        List<Map.Entry<String, Long>> sorted = rows.stream()
                .map(row -> new AbstractMap.SimpleEntry<>((String) row[0], ((Number) row[1]).longValue()))
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        for (Map.Entry<String, Long> e : sorted) {
            total += e.getValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", e.getKey());
            item.put("value", e.getValue());
            chartData.add(item);
        }

        result.put("distribution", chartData);
        result.put("total", total);
        result.put("graduationYear", year);
        return result;
    }

    // ==================== 省份分布（SQL 聚合 + Redis 缓存） ====================

    @GetMapping("/province-dist")
    public Result<Map<String, Object>> getProvinceDist(
            @RequestParam(required = false) Integer graduationYear) {
        String cacheKey = statsCacheService.analyticsKey("province:" + (graduationYear == null ? "all" : graduationYear));
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> cached = (Map<String, Object>) statsCacheService.getAnalyticsCache(cacheKey);
            if (cached != null) return Result.success(cached);
        } catch (Exception e) {}

        Map<String, Object> result = buildProvince(graduationYear);
        try { statsCacheService.putAnalyticsCache(cacheKey, result); } catch (Exception e) {}
        return Result.success(result);
    }

    private Map<String, Object> buildProvince(Integer year) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> chartData = new ArrayList<>();
        long total = 0;

        List<Object[]> rows = employmentRecordRepository.countGlobalProvince(year);
        for (Object[] row : rows) {
            total += ((Number) row[1]).longValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row[0]);
            item.put("value", ((Number) row[1]).longValue());
            chartData.add(item);
        }

        result.put("distribution", chartData);
        result.put("total", total);
        result.put("graduationYear", year);
        return result;
    }

    // ==================== 可选年份 ====================

    @GetMapping("/available-years")
    public Result<List<Integer>> getAvailableYears() {
        List<Object[]> years = studentInfoRepository.countByGraduationYear();
        List<Integer> result = new ArrayList<>();
        for (Object[] row : years) {
            if (row[0] != null) result.add(((Number) row[0]).intValue());
        }
        result.sort(Comparator.reverseOrder());
        return Result.success(result);
    }

    // ==================== 未就业学生列表 ====================

    @GetMapping("/unemployed-students")
    public Result<Map<String, Object>> getUnemployedStudents(
            @RequestParam(required = false) Integer graduationYear,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50000") Integer pageSize) {

        List<Object[]> rows = employmentRecordRepository.findUnemployedStudents(graduationYear);
        long total = employmentRecordRepository.countUnemployedStudents(graduationYear);

        // page=0 时返回全部（用于按院系/班级/专业过滤查看）
        int start;
        int end;
        if (page == 0) {
            start = 0;
            end = rows.size();
        } else {
            start = (page - 1) * pageSize;
            end = Math.min(start + pageSize, rows.size());
        }
        List<Map<String, Object>> pageData = new ArrayList<>();
        for (int i = start; i < end; i++) {
            Object[] row = rows.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("studentId", row[0]);
            item.put("realName", row[1]);
            item.put("studentNo", row[2]);
            item.put("gender", row[3]);
            item.put("graduationYear", row[4]);
            item.put("deptName", row[5]);
            item.put("majorName", row[6]);
            item.put("className", row[7]);
            String recordStatus = row[8] != null ? row[8].toString() : null;
            item.put("recordStatus", recordStatus);
            item.put("employmentType", row[9]);
            // 风险原因判断
            if (recordStatus == null) {
                item.put("riskReason", "未登记任何就业信息");
            } else if ("pending".equals(recordStatus)) {
                item.put("riskReason", "就业信息待审核");
            } else if ("rejected".equals(recordStatus)) {
                item.put("riskReason", "就业信息审核未通过");
            } else {
                item.put("riskReason", "状态异常");
            }
            // 风险等级
            if (recordStatus == null) {
                item.put("riskLevel", "高风险");
            } else if ("rejected".equals(recordStatus)) {
                item.put("riskLevel", "高风险");
            } else if ("pending".equals(recordStatus)) {
                item.put("riskLevel", "中风险");
            } else {
                item.put("riskLevel", "低风险");
            }
            pageData.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", pageData);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return Result.success(result);
    }

    // ==================== 专业预警统计 ====================

    @GetMapping("/major-stats")
    public Result<List<Map<String, Object>>> getMajorStats(
            @RequestParam(required = false) Integer graduationYear) {

        String cacheKey = statsCacheService.analyticsKey("major-stats:" + (graduationYear == null ? "all" : graduationYear));
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cached = (List<Map<String, Object>>) statsCacheService.getAnalyticsCache(cacheKey);
            if (cached != null) return Result.success(cached);
        } catch (Exception e) {}

        List<Map<String, Object>> result = new ArrayList<>();
        double overallRate = 0;
        long totalStudents = studentInfoRepository.countStudents(graduationYear);
        if (totalStudents > 0) {
            long employed = employmentRecordRepository.countGlobalEmployed(graduationYear);
            overallRate = (double) employed / totalStudents * 100;
        }

        for (Object[] row : studentInfoRepository.countGroupByMajor(graduationYear)) {
            Number majorIdNum = (Number) row[0];
            String majorName = row[1] == null ? "未知专业" : (String) row[1];
            String deptName = row[3] == null ? "未知院系" : (String) row[3];
            Number totalNum = (Number) row[4];
            long majorId = majorIdNum == null ? 0 : majorIdNum.longValue();
            long total = totalNum == null ? 0 : totalNum.longValue();

            long employed = studentInfoRepository.countEmployedByMajorId(majorId, graduationYear);

            double rate = total > 0 ? Math.round((double) employed / total * 10000) / 100.0 : 0.0;
            long below = (long) Math.max(0, Math.round((overallRate - rate) * total / 100));
            String level = rate < overallRate - 5 ? "重点关注" : rate < overallRate ? "一般关注" : "正常";

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("majorId", majorId);
            item.put("majorName", majorName);
            item.put("deptName", deptName);
            item.put("totalStudents", total);
            item.put("employed", employed);
            item.put("unemployed", total - employed);
            item.put("employmentRate", rate);
            item.put("belowAvgCount", below);
            item.put("warningLevel", level);
            item.put("graduationYear", graduationYear);
            result.add(item);
        }

        result.sort((a, b) -> {
            Double ra = (Double) a.get("employmentRate");
            Double rb = (Double) b.get("employmentRate");
            return ra.compareTo(rb);
        });

        try { statsCacheService.putAnalyticsCache(cacheKey, result); } catch (Exception e) {}
        return Result.success(result);
    }
}
