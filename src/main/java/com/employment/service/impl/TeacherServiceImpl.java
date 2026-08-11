package com.employment.service.impl;

import com.employment.common.Constants;
import com.employment.model.entity.ClassEmploymentReminder;
import com.employment.model.entity.CompanyAuth;
import com.employment.model.entity.CompanyInfo;
import com.employment.model.entity.ConversationRecord;
import com.employment.model.entity.DataPermissionRequest;
import com.employment.model.entity.EmploymentRecord;
import com.employment.model.entity.JobApplication;
import com.employment.model.entity.StudentInfo;
import com.employment.model.entity.SysClass;
import com.employment.model.entity.SysDept;
import com.employment.model.entity.SysRole;
import com.employment.model.entity.SysUser;
import com.employment.model.entity.TripartiteAgreement;
import com.employment.repository.*;
import com.employment.security.SpringContextHolder;
import com.employment.service.NotificationService;
import com.employment.service.StatsCacheService;
import com.employment.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final SysUserRepository sysUserRepository;
    private final SysClassRepository sysClassRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final EmploymentRecordRepository employmentRecordRepository;
    private final SysDeptRepository sysDeptRepository;
    private final ConversationRecordRepository conversationRecordRepository;
    private final DataPermissionRequestRepository dataPermissionRequestRepository;
    private final ClassEmploymentReminderRepository classEmploymentReminderRepository;
    private final CompanyAuthRepository companyAuthRepository;
    private final CompanyInfoRepository companyInfoRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final TripartiteAgreementRepository tripartiteAgreementRepository;
    private final NotificationService notificationService;
    private final JobApplicationRepository jobApplicationRepository;
    private final StatsCacheService statsCacheService;

    private static final String STATS_CACHE_PREFIX = "teacher:stats:";

    @Override
    public Map<String, Object> getMyClass(Long userId) {
        Map<String, Object> result = new HashMap<>();
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) {
            return result;
        }
        Long targetClassId = user.getClassId();
        if (targetClassId == null) {
            List<SysClass> advisorClasses = sysClassRepository.findByAdvisorId(userId);
            if (!advisorClasses.isEmpty()) {
                targetClassId = advisorClasses.get(0).getId();
            }
        }
        if (targetClassId != null) {
            SysClass cls = sysClassRepository.findById(targetClassId).orElse(null);
            if (cls != null) {
                result.put("classId", cls.getId());
                result.put("className", cls.getClassName());
                result.put("grade", cls.getGrade());
                result.put("advisor", cls.getAdvisor());
                result.put("studentCount", cls.getStudentCount());
                result.put("deptId", cls.getDeptId());
                result.put("deptName", getDeptName(cls.getDeptId()));
                return result;
            }
        }
        result.put("deptId", user.getDeptId());
        result.put("deptName", getDeptName(user.getDeptId()));
        return result;
    }

    @Override
    public List<Map<String, Object>> getStudents(Long userId, String role, String keyword) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) getStudents(userId, role, keyword, 1, 10);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.get("records");
        return records;
    }

    @Override
    public Object getStudents(Long userId, String role, String keyword, Integer page, Integer size) {
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("records", Collections.emptyList());
            result.put("total", 0L);
            return result;
        }

        List<StudentInfo> students;
        if (Constants.ROLE_CLASS_TEACHER.equals(role)) {
            Long classId = user.getClassId();
            if (classId == null) {
                List<SysClass> advisorClasses = sysClassRepository.findByAdvisorId(userId);
                if (!advisorClasses.isEmpty()) {
                    classId = advisorClasses.get(0).getId();
                }
            }
            students = classId != null ? studentInfoRepository.findByClassId(classId) : Collections.emptyList();
        } else if (Constants.ROLE_DEPT_TEACHER.equals(role)) {
            Long deptId = user.getDeptId();
            students = deptId != null ? studentInfoRepository.findByDeptId(deptId) : Collections.emptyList();
        } else {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("records", Collections.emptyList());
            result.put("total", 0L);
            return result;
        }

        if (StringUtils.hasText(keyword)) {
            students = students.stream()
                    .filter(s -> (s.getRealName() != null && s.getRealName().contains(keyword))
                            || (s.getStudentNo() != null && s.getStudentNo().contains(keyword)))
                    .collect(Collectors.toList());
        }

        long total = students.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, (int) total);
        if (fromIndex >= students.size()) {
            students = Collections.emptyList();
        } else {
            students = students.subList(fromIndex, toIndex);
        }

        List<Map<String, Object>> records = students.stream()
                .map(this::toStudentMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    @Override
    public List<Map<String, Object>> getClasses(Long userId, String role) {
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) {
            return Collections.emptyList();
        }

        List<SysClass> classes;
        if (Constants.ROLE_CLASS_TEACHER.equals(role)) {
            Long classId = user.getClassId();
            if (classId == null) {
                List<SysClass> advisorClasses = sysClassRepository.findByAdvisorId(userId);
                if (!advisorClasses.isEmpty()) {
                    classId = advisorClasses.get(0).getId();
                }
            }
            if (classId != null) {
                SysClass cls = sysClassRepository.findById(classId).orElse(null);
                classes = cls != null ? Collections.singletonList(cls) : Collections.emptyList();
            } else {
                classes = Collections.emptyList();
            }
        } else if (Constants.ROLE_DEPT_TEACHER.equals(role)) {
            Long deptId = user.getDeptId();
            classes = deptId != null ? sysClassRepository.findByDeptId(deptId) : Collections.emptyList();
        } else {
            return Collections.emptyList();
        }

        Map<Long, Long> countMap = sysClassRepository.countStudentsByClassId().stream()
                .collect(Collectors.toMap(
                        m -> (Long) m.get("classId"),
                        m -> (Long) m.get("studentCount")
                ));

        return classes.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("className", c.getClassName());
            map.put("grade", c.getGrade());
            map.put("advisor", c.getAdvisor());
            map.put("advisorUserId", c.getAdvisorId());
            map.put("studentCount", countMap.getOrDefault(c.getId(), 0L).intValue());
            map.put("deptId", c.getDeptId());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getEmploymentStats(Long userId, String role) {
        return getEmploymentStats(userId, role, null);
    }

    @Override
    public Map<String, Object> getEmploymentStats(Long userId, String role, Integer graduationYear) {
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) {
            return Collections.emptyMap();
        }

        String cacheKey = buildStatsCacheKey(role, user, graduationYear);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> cached = (Map<String, Object>) statsCacheService.getFromCache(cacheKey);
            if (cached != null) {
                log.debug("就业统计命中缓存: {}", cacheKey);
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis 读取缓存失败，降级到数据库查询: {}", e.getMessage());
        }

        Map<String, Object> result;

        // 有 graduationYear 筛选时，走原有内存计算（含 detailList，用于统计详情页）
        if (graduationYear != null) {
            List<StudentInfo> students;
            if (Constants.ROLE_CLASS_TEACHER.equals(role)) {
                Long classId = user.getClassId();
                if (classId == null) {
                    List<SysClass> advisorClasses = sysClassRepository.findByAdvisorId(userId);
                    if (!advisorClasses.isEmpty()) classId = advisorClasses.get(0).getId();
                }
                students = classId != null ? studentInfoRepository.findByClassIdAndGraduationYear(classId, graduationYear) : Collections.emptyList();
            } else if (Constants.ROLE_DEPT_TEACHER.equals(role)) {
                Long deptId = user.getDeptId();
                students = deptId != null ? studentInfoRepository.findByDeptIdAndGraduationYear(deptId, graduationYear) : Collections.emptyList();
            } else {
                return Collections.emptyMap();
            }
            result = calculateEmploymentStats(students);
        } else {
            // 无 graduationYear 筛选时，走 SQL 聚合路径（极速，用于 Dashboard 首页）
            if (Constants.ROLE_CLASS_TEACHER.equals(role)) {
                Long classId = user.getClassId();
                if (classId == null) {
                    List<SysClass> advisorClasses = sysClassRepository.findByAdvisorId(userId);
                    if (!advisorClasses.isEmpty()) classId = advisorClasses.get(0).getId();
                }
                if (classId == null) {
                    result = Collections.emptyMap();
                } else {
                    result = calculateEmploymentStatsForDashboard(
                            studentInfoRepository.findByClassId(classId).stream().map(StudentInfo::getId).collect(Collectors.toList()),
                            false,
                            Collections.singletonList(classId)
                    );
                }
            } else if (Constants.ROLE_DEPT_TEACHER.equals(role)) {
                Long deptId = user.getDeptId();
                if (deptId == null) {
                    result = Collections.emptyMap();
                } else {
                    result = calculateEmploymentStatsForDashboard(
                            studentInfoRepository.findByDeptId(deptId).stream().map(StudentInfo::getId).collect(Collectors.toList()),
                            true,
                            Collections.singletonList(deptId)
                    );
                }
            } else {
                result = Collections.emptyMap();
            }
        }

        try {
            statsCacheService.putIntoCache(cacheKey, result);
            log.debug("就业统计写入缓存: {}", cacheKey);
        } catch (Exception e) {
            log.warn("Redis 写入缓存失败: {}", e.getMessage());
        }

        return result;
    }

    private String buildStatsCacheKey(String role, SysUser user, Integer graduationYear) {
        if (Constants.ROLE_CLASS_TEACHER.equals(role)) {
            Long classId = user.getClassId();
            if (classId == null) {
                List<SysClass> advisorClasses = sysClassRepository.findByAdvisorId(user.getId());
                if (!advisorClasses.isEmpty()) classId = advisorClasses.get(0).getId();
            }
            return STATS_CACHE_PREFIX + "class:" + (classId != null ? classId : "none") + ":" + (graduationYear != null ? graduationYear : "all");
        } else {
            return STATS_CACHE_PREFIX + "dept:" + (user.getDeptId() != null ? user.getDeptId() : "none") + ":" + (graduationYear != null ? graduationYear : "all");
        }
    }

    @Override
    public Map<String, Object> getClassEmploymentStats(Long classId) {
        return getClassEmploymentStats(classId, null);
    }

    @Override
    public Map<String, Object> getClassEmploymentStats(Long classId, Integer graduationYear) {
        if (classId == null) {
            return Collections.emptyMap();
        }

        List<StudentInfo> students;
        if (graduationYear != null) {
            students = studentInfoRepository.findByClassIdAndGraduationYear(classId, graduationYear);
        } else {
            students = studentInfoRepository.findByClassId(classId);
        }
        return calculateEmploymentStats(students);
    }

    /**
     * Dashboard 专用统计：全部用 SQL 聚合，不加载明细数据，极速响应。
     * 不再把每个学生和每条记录加载到 Java 内存中排序。
     */
    private Map<String, Object> calculateEmploymentStatsForDashboard(List<Long> studentIds, boolean byDept, List<Long> scopeIds) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", (long) studentIds.size());

        // 取每个学生最新一条 approved 记录的人数（SQL COUNT，DB 层完成）
        long employed = byDept
                ? employmentRecordRepository.countEmployedByDeptIds(scopeIds)
                : employmentRecordRepository.countEmployedByClassIds(scopeIds);
        long pending = byDept
                ? employmentRecordRepository.countPendingByDeptIds(scopeIds)
                : employmentRecordRepository.countPendingByClassIds(scopeIds);
        long total = studentIds.size();
        long unemployed = total - employed - pending;

        stats.put("employed", employed);
        stats.put("unemployed", Math.max(0, unemployed));
        stats.put("pending", pending);
        stats.put("employmentRate", total > 0 ? String.format("%.1f", (double) employed / total * 100) : "0.0");

        // 特殊就业类型（SQL GROUP BY）
        List<Object[]> specialRows = byDept
                ? employmentRecordRepository.countSpecialTypeByDeptIds(scopeIds)
                : employmentRecordRepository.countSpecialTypeByClassIds(scopeIds);
        long graduateSchool = 0, military = 0, selfEmployed = 0, abroad = 0;
        for (Object[] row : specialRows) {
            String type = (String) row[0];
            long cnt = ((Number) row[1]).longValue();
            if ("继续深造".equals(type) || "升学".equals(type)) graduateSchool += cnt;
            else if ("应征入伍".equals(type) || "入伍".equals(type) || "服兵役".equals(type)) military += cnt;
            else if ("自主创业".equals(type) || "创业".equals(type)) selfEmployed += cnt;
            else abroad += cnt;
        }
        Map<String, Object> specialTypes = new LinkedHashMap<>();
        specialTypes.put("继续深造", graduateSchool);
        specialTypes.put("应征入伍", military);
        specialTypes.put("自主创业", selfEmployed);
        specialTypes.put("出国出境", abroad);
        stats.put("specialTypes", specialTypes);

        // 省份分布
        List<Object[]> provinceRows = byDept
                ? employmentRecordRepository.countProvinceByDeptIds(scopeIds)
                : employmentRecordRepository.countProvinceByClassIds(scopeIds);
        Map<String, Long> provinceCount = new LinkedHashMap<>();
        for (Object[] row : provinceRows) {
            String p = (String) row[0];
            long cnt = ((Number) row[1]).longValue();
            if (p != null && !p.isEmpty()) provinceCount.put(p, cnt);
        }
        stats.put("provinceDistribution", provinceCount);

        // 就业类型分布
        List<Object[]> typeRows = byDept
                ? employmentRecordRepository.countEmploymentTypeByDeptIds(scopeIds)
                : employmentRecordRepository.countEmploymentTypeByClassIds(scopeIds);
        Map<String, Long> typeCount = new LinkedHashMap<>();
        for (Object[] row : typeRows) {
            String t = (String) row[0];
            long cnt = ((Number) row[1]).longValue();
            if (t != null && !t.isEmpty()) typeCount.put(t, cnt);
        }
        stats.put("employmentTypeDistribution", typeCount);

        // 行业分布
        List<Object[]> industryRows = byDept
                ? employmentRecordRepository.countIndustryByDeptIds(scopeIds)
                : employmentRecordRepository.countIndustryByClassIds(scopeIds);
        Map<String, Long> industryCount = new LinkedHashMap<>();
        for (Object[] row : industryRows) {
            String i = (String) row[0];
            long cnt = ((Number) row[1]).longValue();
            if (i != null && !i.isEmpty()) industryCount.put(i, cnt);
        }
        stats.put("industryDistribution", industryCount);

        // 薪资分布（只取 salary 字段，在 DB 层只返回有薪资的记录）
        List<Object[]> salaryRows = byDept
                ? employmentRecordRepository.salaryDataByDeptIds(scopeIds)
                : employmentRecordRepository.salaryDataByClassIds(scopeIds);
        Map<String, Long> salaryCount = new LinkedHashMap<>();
        salaryCount.put("5k以下", 0L);
        salaryCount.put("5k-8k", 0L);
        salaryCount.put("8k-12k", 0L);
        salaryCount.put("12k-20k", 0L);
        salaryCount.put("20k以上", 0L);
        for (Object[] row : salaryRows) {
            String sal = (String) row[0];
            if (sal == null || sal.isEmpty()) continue;
            try {
                double salary = Double.parseDouble(sal.replaceAll("[^\\d.]", ""));
                if (salary < 5000) salaryCount.put("5k以下", salaryCount.get("5k以下") + 1);
                else if (salary < 8000) salaryCount.put("5k-8k", salaryCount.get("5k-8k") + 1);
                else if (salary < 12000) salaryCount.put("8k-12k", salaryCount.get("8k-12k") + 1);
                else if (salary < 20000) salaryCount.put("12k-20k", salaryCount.get("12k-20k") + 1);
                else salaryCount.put("20k以上", salaryCount.get("20k以上") + 1);
            } catch (NumberFormatException ignored) {}
        }
        stats.put("salaryDistribution", salaryCount);

        // 城市分布
        List<Object[]> cityRows = byDept
                ? employmentRecordRepository.countCityByDeptIds(scopeIds)
                : employmentRecordRepository.countCityByClassIds(scopeIds);
        Map<String, Long> cityCount = new LinkedHashMap<>();
        for (Object[] row : cityRows) {
            String c = (String) row[0];
            long cnt = ((Number) row[1]).longValue();
            if (c != null && !c.isEmpty()) cityCount.put(c, cnt);
        }
        stats.put("cityDistribution", cityCount);

        // Dashboard 不需要 detailList，减少响应体体积
        return stats;
    }

    /**
     * 就业统计（原有逻辑，用于统计详情页，不在此方法内计算明细）
     */
    private Map<String, Object> calculateEmploymentStats(List<StudentInfo> students) {
        long total = students.size();
        long employed = 0;
        long unemployed = 0;
        long pending = 0;
        long graduateSchool = 0, military = 0, selfEmployed = 0, abroad = 0;

        Map<Long, EmploymentRecord> latestRecords = new LinkedHashMap<>();
        List<EmploymentRecord> approvedRecords = new ArrayList<>();
        if (students.isEmpty()) {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("total", 0L);
            stats.put("employed", 0L);
            stats.put("unemployed", 0L);
            stats.put("pending", 0L);
            stats.put("employmentRate", "0.0");
            stats.put("specialTypes", Collections.emptyMap());
            stats.put("provinceDistribution", Collections.emptyMap());
            stats.put("employmentTypeDistribution", Collections.emptyMap());
            stats.put("industryDistribution", Collections.emptyMap());
            stats.put("salaryDistribution", Collections.emptyMap());
            stats.put("cityDistribution", Collections.emptyMap());
            stats.put("detailList", Collections.emptyList());
            return stats;
        }

        List<Long> studentIds = students.stream().map(StudentInfo::getId).collect(Collectors.toList());
        List<EmploymentRecord> allRecords = employmentRecordRepository.findByStudentIdIn(studentIds);
        Map<Long, List<EmploymentRecord>> grouped = allRecords.stream()
                .collect(Collectors.groupingBy(EmploymentRecord::getStudentId));

        for (StudentInfo s : students) {
            List<EmploymentRecord> records = grouped.getOrDefault(s.getId(), Collections.emptyList());
            if (records == null || records.isEmpty()) {
                unemployed++;
            } else {
                records.sort((a, b) -> {
                    if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
                    if (a.getCreateTime() == null) return 1;
                    if (b.getCreateTime() == null) return -1;
                    return b.getCreateTime().compareTo(a.getCreateTime());
                });
                EmploymentRecord latest = records.get(0);
                String status = latest.getAuditStatus();
                String empType = latest.getEmploymentType();
                boolean isSpecial = "继续深造".equals(empType) || "升学".equals(empType)
                        || "应征入伍".equals(empType) || "入伍".equals(empType) || "服兵役".equals(empType)
                        || "自主创业".equals(empType) || "创业".equals(empType)
                        || "出国出境".equals(empType) || "出国".equals(empType) || "境外".equals(empType);
                if ("approved".equals(status)) {
                    if (isSpecial) {
                        latestRecords.put(s.getId(), latest);
                        approvedRecords.add(latest);
                        if ("继续深造".equals(empType) || "升学".equals(empType)) graduateSchool++;
                        else if ("应征入伍".equals(empType) || "入伍".equals(empType) || "服兵役".equals(empType)) military++;
                        else if ("自主创业".equals(empType) || "创业".equals(empType)) selfEmployed++;
                        else abroad++;
                    } else {
                        employed++;
                        latestRecords.put(s.getId(), latest);
                        approvedRecords.add(latest);
                    }
                } else if ("pending".equals(status)) {
                    pending++;
                } else {
                    unemployed++;
                }
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("employed", employed);
        stats.put("unemployed", unemployed);
        stats.put("pending", pending);
        stats.put("employmentRate", total > 0 ? String.format("%.1f", (double) employed / total * 100) : "0.0");

        Map<String, Object> specialTypes = new LinkedHashMap<>();
        specialTypes.put("继续深造", graduateSchool);
        specialTypes.put("应征入伍", military);
        specialTypes.put("自主创业", selfEmployed);
        specialTypes.put("出国出境", abroad);
        stats.put("specialTypes", specialTypes);

        Map<String, Long> provinceCount = new LinkedHashMap<>();
        for (EmploymentRecord r : approvedRecords) {
            String p = r.getWorkProvince();
            if (p != null && !p.isEmpty()) {
                provinceCount.put(p, provinceCount.getOrDefault(p, 0L) + 1);
            }
        }
        stats.put("provinceDistribution", provinceCount);

        Map<String, Long> typeCount = new LinkedHashMap<>();
        for (EmploymentRecord r : approvedRecords) {
            String t = r.getEmploymentType();
            if (t != null && !t.isEmpty()) {
                typeCount.put(t, typeCount.getOrDefault(t, 0L) + 1);
            }
        }
        stats.put("employmentTypeDistribution", typeCount);

        Map<String, Long> industryCount = new LinkedHashMap<>();
        for (EmploymentRecord r : approvedRecords) {
            String i = r.getCompanyIndustry();
            if (i != null && !i.isEmpty()) {
                industryCount.put(i, industryCount.getOrDefault(i, 0L) + 1);
            }
        }
        stats.put("industryDistribution", industryCount);

        Map<String, Long> salaryCount = new LinkedHashMap<>();
        salaryCount.put("5k以下", 0L);
        salaryCount.put("5k-8k", 0L);
        salaryCount.put("8k-12k", 0L);
        salaryCount.put("12k-20k", 0L);
        salaryCount.put("20k以上", 0L);
        for (EmploymentRecord r : approvedRecords) {
            String sal = r.getSalary();
            if (sal != null && !sal.isEmpty()) {
                try {
                    double salary = Double.parseDouble(sal.replaceAll("[^\\d.]", ""));
                    if (salary < 5000) salaryCount.put("5k以下", salaryCount.get("5k以下") + 1);
                    else if (salary < 8000) salaryCount.put("5k-8k", salaryCount.get("5k-8k") + 1);
                    else if (salary < 12000) salaryCount.put("8k-12k", salaryCount.get("8k-12k") + 1);
                    else if (salary < 20000) salaryCount.put("12k-20k", salaryCount.get("12k-20k") + 1);
                    else salaryCount.put("20k以上", salaryCount.get("20k以上") + 1);
                } catch (NumberFormatException ignored) {}
            }
        }
        stats.put("salaryDistribution", salaryCount);

        Map<String, Long> cityCount = new LinkedHashMap<>();
        for (EmploymentRecord r : approvedRecords) {
            String c = r.getWorkCity();
            if (c != null && !c.isEmpty()) {
                cityCount.put(c, cityCount.getOrDefault(c, 0L) + 1);
            }
        }
        stats.put("cityDistribution", cityCount);

        List<Map<String, Object>> detailList = new ArrayList<>();
        for (StudentInfo s : students) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", s.getId());
            row.put("studentNo", s.getStudentNo());
            row.put("realName", s.getRealName());
            row.put("gender", s.getGender());
            EmploymentRecord rec = latestRecords.get(s.getId());
            if (rec != null) {
                row.put("employmentType", rec.getEmploymentType());
                row.put("companyName", rec.getCompanyName());
                row.put("workProvince", rec.getWorkProvince());
                row.put("workCity", rec.getWorkCity());
                row.put("positionName", rec.getPositionName());
                row.put("salary", rec.getSalary());
                row.put("industry", rec.getCompanyIndustry());
                row.put("auditStatus", rec.getAuditStatus());
            } else {
                row.put("employmentType", null);
                row.put("companyName", null);
                row.put("workProvince", null);
                row.put("workCity", null);
                row.put("positionName", null);
                row.put("salary", null);
                row.put("industry", null);
                row.put("auditStatus", "unemployed");
            }
            detailList.add(row);
        }
        stats.put("detailList", detailList);

        return stats;
    }

    @Override
    public List<Map<String, Object>> getPendingEmployments(Long userId, String role) {
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) return Collections.emptyList();

        List<EmploymentRecord> pending;
        if (Constants.ROLE_CLASS_TEACHER.equals(role)) {
            Long classId = user.getClassId();
            if (classId == null) {
                List<SysClass> advisorClasses = sysClassRepository.findByAdvisorId(userId);
                if (!advisorClasses.isEmpty()) classId = advisorClasses.get(0).getId();
            }
            pending = classId != null
                    ? employmentRecordRepository.findPendingByClassIds(Collections.singletonList(classId))
                    : Collections.emptyList();
        } else if (Constants.ROLE_DEPT_TEACHER.equals(role)) {
            Long deptId = user.getDeptId();
            pending = deptId != null
                    ? employmentRecordRepository.findPendingByDeptIds(Collections.singletonList(deptId))
                    : Collections.emptyList();
        } else {
            return Collections.emptyList();
        }

        Map<Long, StudentInfo> studentMap = studentInfoRepository.findAllById(
                pending.stream().map(EmploymentRecord::getStudentId).collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(StudentInfo::getId, s -> s));

        return pending.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("studentId", r.getStudentId());
            StudentInfo s = studentMap.get(r.getStudentId());
            if (s != null) {
                map.put("studentNo", s.getStudentNo());
                map.put("realName", s.getRealName());
                map.put("className", s.getClassName());
            }
            map.put("employmentType", r.getEmploymentType());
            map.put("companyName", r.getCompanyName());
            map.put("companyScale", r.getCompanyScale());
            map.put("companyIndustry", r.getCompanyIndustry());
            map.put("positionName", r.getPositionName());
            map.put("workProvince", r.getWorkProvince());
            map.put("workCity", r.getWorkCity());
            map.put("salary", r.getSalary());
            map.put("isThreePartySigned", r.getIsThreePartySigned());
            map.put("threePartyNo", r.getThreePartyNo());
            map.put("contractStartDate", r.getContractStartDate());
            map.put("contractEndDate", r.getContractEndDate());
            map.put("probationSalary", r.getProbationSalary());
            map.put("auditStatus", r.getAuditStatus());
            map.put("auditRemark", r.getAuditRemark());
            map.put("remark", r.getRemark());
            map.put("createTime", r.getCreateTime());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getEmploymentHistory(Long userId, String role) {
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) return Collections.emptyList();

        List<EmploymentRecord> allRecords;
        if (Constants.ROLE_CLASS_TEACHER.equals(role)) {
            Long classId = user.getClassId();
            if (classId == null) {
                List<SysClass> advisorClasses = sysClassRepository.findByAdvisorId(userId);
                if (!advisorClasses.isEmpty()) classId = advisorClasses.get(0).getId();
            }
            allRecords = classId != null
                    ? employmentRecordRepository.findApprovedByClassIds(Collections.singletonList(classId))
                    : Collections.emptyList();
        } else if (Constants.ROLE_DEPT_TEACHER.equals(role)) {
            Long deptId = user.getDeptId();
            allRecords = deptId != null
                    ? employmentRecordRepository.findApprovedByDeptIds(Collections.singletonList(deptId))
                    : Collections.emptyList();
        } else {
            return Collections.emptyList();
        }

        Map<Long, StudentInfo> studentMap = studentInfoRepository.findAllById(
                allRecords.stream().map(EmploymentRecord::getStudentId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(StudentInfo::getId, s -> s));

        return allRecords.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("studentId", r.getStudentId());
            StudentInfo s = studentMap.get(r.getStudentId());
            if (s != null) {
                map.put("studentNo", s.getStudentNo());
                map.put("realName", s.getRealName());
                map.put("className", s.getClassName());
            }
            map.put("employmentType", r.getEmploymentType());
            map.put("companyName", r.getCompanyName());
            map.put("companyScale", r.getCompanyScale());
            map.put("companyIndustry", r.getCompanyIndustry());
            map.put("positionName", r.getPositionName());
            map.put("workProvince", r.getWorkProvince());
            map.put("workCity", r.getWorkCity());
            map.put("salary", r.getSalary());
            map.put("isThreePartySigned", r.getIsThreePartySigned());
            map.put("threePartyNo", r.getThreePartyNo());
            map.put("contractStartDate", r.getContractStartDate());
            map.put("contractEndDate", r.getContractEndDate());
            map.put("probationSalary", r.getProbationSalary());
            map.put("auditStatus", r.getAuditStatus());
            map.put("auditRemark", r.getAuditRemark());
            map.put("auditTime", r.getAuditTime());
            map.put("remark", r.getRemark());
            map.put("createTime", r.getCreateTime());
            return map;
        }).sorted((a, b) -> {
            String t1 = (String) a.get("auditTime");
            String t2 = (String) b.get("auditTime");
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getConversations(Long userId) {
        return conversationRecordRepository.findByTeacherId(userId).stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getId());
            map.put("teacherId", c.getTeacherId());
            map.put("studentId", c.getStudentId());
            map.put("conversationTime", c.getConversationTime());
            map.put("conversationType", c.getConversationType());
            map.put("conversationPlace", c.getConversationPlace());
            map.put("topic", c.getTopic());
            map.put("content", c.getContent());
            map.put("result", c.getResult());
            map.put("nextPlan", c.getNextPlan());
            map.put("createTime", c.getCreateTime());
            StudentInfo s = studentInfoRepository.findById(c.getStudentId()).orElse(null);
            if (s != null) {
                map.put("studentNo", s.getStudentNo());
                map.put("studentName", s.getRealName());
                map.put("className", s.getClassName());
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public void createConversation(Long userId, Map<String, Object> data) {
        ConversationRecord record = new ConversationRecord();
        record.setTeacherId(userId);
        record.setStudentId(Long.valueOf(data.get("studentId").toString()));
        if (data.get("conversationTime") != null) record.setConversationTime(data.get("conversationTime").toString());
        if (data.get("conversationType") != null) record.setConversationType(data.get("conversationType").toString());
        if (data.get("conversationPlace") != null) record.setConversationPlace(data.get("conversationPlace").toString());
        if (data.get("topic") != null) record.setTopic(data.get("topic").toString());
        if (data.get("content") != null) record.setContent(data.get("content").toString());
        if (data.get("result") != null) record.setResult(data.get("result").toString());
        if (data.get("nextPlan") != null) record.setNextPlan(data.get("nextPlan").toString());
        if (data.get("attachmentPath") != null) record.setAttachmentPath(data.get("attachmentPath").toString());
        conversationRecordRepository.save(record);
    }

    @Override
    public void deleteConversation(Long id) {
        conversationRecordRepository.deleteById(id);
    }

    @Override
    public Map<String, Object> getConversation(Long id) {
        ConversationRecord c = conversationRecordRepository.findById(id).orElse(null);
        if (c == null) return Collections.emptyMap();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("teacherId", c.getTeacherId());
        map.put("studentId", c.getStudentId());
        map.put("conversationTime", c.getConversationTime());
        map.put("conversationType", c.getConversationType());
        map.put("conversationPlace", c.getConversationPlace());
        map.put("topic", c.getTopic());
        map.put("content", c.getContent());
        map.put("result", c.getResult());
        map.put("nextPlan", c.getNextPlan());
        map.put("attachmentPath", c.getAttachmentPath());
        map.put("createTime", c.getCreateTime());
        StudentInfo s = studentInfoRepository.findById(c.getStudentId()).orElse(null);
        if (s != null) {
            map.put("studentNo", s.getStudentNo());
            map.put("studentName", s.getRealName());
            map.put("className", s.getClassName());
        }
        return map;
    }

    @Override
    public void updateConversation(Long id, Map<String, Object> data) {
        ConversationRecord record = conversationRecordRepository.findById(id)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "记录不存在"));
        if (data.get("studentId") != null) record.setStudentId(Long.valueOf(data.get("studentId").toString()));
        if (data.get("conversationTime") != null) record.setConversationTime(data.get("conversationTime").toString());
        if (data.get("conversationType") != null) record.setConversationType(data.get("conversationType").toString());
        if (data.get("conversationPlace") != null) record.setConversationPlace(data.get("conversationPlace").toString());
        if (data.get("topic") != null) record.setTopic(data.get("topic").toString());
        if (data.get("content") != null) record.setContent(data.get("content").toString());
        if (data.get("result") != null) record.setResult(data.get("result").toString());
        if (data.get("nextPlan") != null) record.setNextPlan(data.get("nextPlan").toString());
        if (data.get("attachmentPath") != null) record.setAttachmentPath(data.get("attachmentPath").toString());
        conversationRecordRepository.save(record);
    }

    @Override
    public List<Map<String, Object>> getPermissionRequests(Long userId, String role) {
        List<DataPermissionRequest> allRequests = dataPermissionRequestRepository.findByStatus("pending");
        if (allRequests.isEmpty()) {
            return Collections.emptyList();
        }

        Long scopeClassId = null;
        Long scopeDeptId = null;
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user != null) {
            if (Constants.ROLE_CLASS_TEACHER.equals(role)) {
                scopeClassId = user.getClassId();
                if (scopeClassId == null) {
                    List<SysClass> advisorClasses = sysClassRepository.findByAdvisorId(userId);
                    if (!advisorClasses.isEmpty()) {
                        scopeClassId = advisorClasses.get(0).getId();
                    }
                }
            } else if (Constants.ROLE_DEPT_TEACHER.equals(role)) {
                scopeDeptId = user.getDeptId();
            }
        }

        Map<Long, StudentInfo> studentMap = studentInfoRepository.findAllById(
                allRequests.stream().map(DataPermissionRequest::getStudentId).collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(StudentInfo::getId, s -> s));

        Set<Long> classIds = studentMap.values().stream()
                .map(StudentInfo::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysClass> classMap = classIds.isEmpty() ? Collections.emptyMap() :
                sysClassRepository.findAllById(classIds).stream()
                        .collect(Collectors.toMap(SysClass::getId, c -> c));

        final Long _scopeClassId = scopeClassId;
        final Long _scopeDeptId = scopeDeptId;

        return allRequests.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("studentId", r.getStudentId());
            map.put("requestType", r.getRequestType());
            map.put("yearFrom", r.getYearFrom());
            map.put("yearTo", r.getYearTo());
            map.put("reason", r.getReason());
            map.put("status", r.getStatus());
            map.put("createTime", r.getCreateTime());
            StudentInfo s = studentMap.get(r.getStudentId());
            if (s != null) {
                map.put("studentNo", s.getStudentNo());
                map.put("studentName", s.getRealName());
                map.put("className", s.getClassName());
                map.put("deptName", s.getDeptName());
                Long studentClassId = s.getClassId();
                SysClass studentClass = studentClassId != null ? classMap.get(studentClassId) : null;
                Long studentDeptId = studentClass != null ? studentClass.getDeptId() : null;
                if (_scopeClassId != null && !_scopeClassId.equals(studentClassId)) {
                    return null;
                }
                if (_scopeDeptId != null && !_scopeDeptId.equals(studentDeptId)) {
                    return null;
                }
            }
            return map;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void auditPermissionRequest(Long id, String action, String remark, Long userId, String role) {
        DataPermissionRequest request = dataPermissionRequestRepository.findById(id)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "申请记录不存在"));
        if ("approve".equals(action)) {
            request.setStatus("approved");
        } else if ("reject".equals(action)) {
            request.setStatus("rejected");
        } else {
            throw new com.employment.exception.BusinessException(400, "无效的审核操作");
        }
        request.setAuditUserId(userId);
        request.setAuditTime(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        if (StringUtils.hasText(remark)) {
            request.setAuditRemark(remark);
        }
        dataPermissionRequestRepository.save(request);

        StudentInfo student = studentInfoRepository.findById(request.getStudentId()).orElse(null);
        if (student != null) {
            String title = "approve".equals(action) ? "数据查看申请已通过" : "数据查看申请被驳回";
            String content = "approve".equals(action)
                    ? "您的数据查看申请已通过审批，您现在可以查看相关就业数据。"
                    : ("您的数据查看申请已被驳回。" + (StringUtils.hasText(remark) ? "原因：" + remark : "如有疑问请联系辅导员。"));
            notificationService.sendNotification(student.getUserId(), title, content, "system", "system");
        }
    }

    @Override
    public List<Map<String, Object>> getPermissionHistory(Long userId, String role) {
        List<DataPermissionRequest> approved = dataPermissionRequestRepository.findByStatus("approved");
        List<DataPermissionRequest> rejected = dataPermissionRequestRepository.findByStatus("rejected");
        List<DataPermissionRequest> allRequests = new ArrayList<>();
        allRequests.addAll(approved);
        allRequests.addAll(rejected);
        if (allRequests.isEmpty()) {
            return Collections.emptyList();
        }

        Long scopeClassId = null;
        Long scopeDeptId = null;
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user != null) {
            if (Constants.ROLE_CLASS_TEACHER.equals(role)) {
                scopeClassId = user.getClassId();
                if (scopeClassId == null) {
                    List<SysClass> advisorClasses = sysClassRepository.findByAdvisorId(userId);
                    if (!advisorClasses.isEmpty()) {
                        scopeClassId = advisorClasses.get(0).getId();
                    }
                }
            } else if (Constants.ROLE_DEPT_TEACHER.equals(role)) {
                scopeDeptId = user.getDeptId();
            }
        }

        Map<Long, StudentInfo> studentMap = studentInfoRepository.findAllById(
                allRequests.stream().map(DataPermissionRequest::getStudentId).collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(StudentInfo::getId, s -> s));

        Set<Long> classIds = studentMap.values().stream()
                .map(StudentInfo::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysClass> classMap = classIds.isEmpty() ? Collections.emptyMap() :
                sysClassRepository.findAllById(classIds).stream()
                        .collect(Collectors.toMap(SysClass::getId, c -> c));

        final Long _scopeClassId = scopeClassId;
        final Long _scopeDeptId = scopeDeptId;

        return allRequests.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("studentId", r.getStudentId());
            map.put("requestType", r.getRequestType());
            map.put("yearFrom", r.getYearFrom());
            map.put("yearTo", r.getYearTo());
            map.put("reason", r.getReason());
            map.put("status", r.getStatus());
            map.put("auditRemark", r.getAuditRemark());
            map.put("auditTime", r.getAuditTime());
            map.put("createTime", r.getCreateTime());
            StudentInfo s = studentMap.get(r.getStudentId());
            if (s != null) {
                map.put("studentNo", s.getStudentNo());
                map.put("studentName", s.getRealName());
                map.put("className", s.getClassName());
                map.put("deptName", s.getDeptName());
                Long studentClassId = s.getClassId();
                SysClass studentClass = studentClassId != null ? classMap.get(studentClassId) : null;
                Long studentDeptId = studentClass != null ? studentClass.getDeptId() : null;
                if (_scopeClassId != null && !_scopeClassId.equals(studentClassId)) return null;
                if (_scopeDeptId != null && !_scopeDeptId.equals(studentDeptId)) return null;
            }
            return map;
        }).filter(Objects::nonNull)
          .sorted((a, b) -> {
              LocalDateTime t1 = (LocalDateTime) b.getOrDefault("createTime", null);
              LocalDateTime t2 = (LocalDateTime) a.getOrDefault("createTime", null);
              if (t1 == null && t2 == null) return 0;
              if (t1 == null) return 1;
              if (t2 == null) return -1;
              return t2.compareTo(t1);
          })
          .collect(Collectors.toList());
    }

    private Map<String, Object> toStudentMap(StudentInfo s) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", s.getId());
        map.put("userId", s.getUserId());
        map.put("studentNo", s.getStudentNo());
        map.put("realName", s.getRealName());
        map.put("gender", s.getGender());
        map.put("phone", s.getPhone());
        map.put("email", s.getEmail());
        map.put("className", s.getClassName());
        map.put("classId", s.getClassId());
        map.put("deptName", s.getDeptName());
        map.put("majorName", s.getMajorName());
        map.put("graduationYear", s.getGraduationYear());
        map.put("status", s.getStatus());
        map.put("employmentStatus", resolveEmploymentStatus(s.getId()));
        return map;
    }

    private String resolveEmploymentStatus(Long studentId) {
        List<EmploymentRecord> records = employmentRecordRepository.findByStudentId(studentId);
        if (records == null || records.isEmpty()) {
            return "unemployed";
        }
        records.sort((a, b) -> {
            if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
            if (a.getCreateTime() == null) return 1;
            if (b.getCreateTime() == null) return -1;
            return b.getCreateTime().compareTo(a.getCreateTime());
        });
        EmploymentRecord latest = records.get(0);
        String status = latest.getAuditStatus();
        if ("approved".equals(status)) {
            return "employed";
        } else if ("pending".equals(status)) {
            return "pending";
        }
        return "unemployed";
    }

    private String getDeptName(Long deptId) {
        if (deptId == null) return null;
        return sysDeptRepository.findById(deptId).map(d -> d.getDeptName()).orElse(null);
    }

    @Override
    public List<Map<String, Object>> getEmploymentReminders(Long userId) {
        return classEmploymentReminderRepository.findByReceiverIdOrderByCreateTimeDesc(userId).stream()
                .map(r -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", r.getId());
                    map.put("senderId", r.getSenderId());
                    map.put("senderName", r.getSenderName());
                    map.put("classId", r.getClassId());
                    map.put("className", r.getClassName());
                    map.put("title", r.getTitle());
                    map.put("content", r.getContent());
                    map.put("employmentRate", r.getEmploymentRate());
                    map.put("totalStudents", r.getTotalStudents());
                    map.put("employedStudents", r.getEmployedStudents());
                    map.put("unemployedStudents", r.getUnemployedStudents());
                    map.put("status", r.getStatus());
                    map.put("isRead", r.getIsRead());
                    map.put("createTime", r.getCreateTime());
                    return map;
                }).collect(Collectors.toList());
    }

    @Override
    public void sendEmploymentReminder(Long userId, Map<String, Object> data) {
        SysUser sender = sysUserRepository.findById(userId)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "发送人不存在"));

        Long classId = Long.valueOf(data.get("classId").toString());
        Long receiverId = Long.valueOf(data.get("receiverId").toString());
        SysUser receiver = sysUserRepository.findById(receiverId)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "接收人不存在"));
        SysClass cls = sysClassRepository.findById(classId)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "班级不存在"));

        ClassEmploymentReminder reminder = new ClassEmploymentReminder();
        reminder.setSenderId(userId);
        reminder.setSenderName(sender.getRealName());
        reminder.setReceiverId(receiverId);
        reminder.setReceiverName(receiver.getRealName());
        reminder.setClassId(classId);
        reminder.setClassName(cls.getClassName());
        reminder.setTitle((String) data.getOrDefault("title", "就业情况提醒"));
        reminder.setContent((String) data.getOrDefault("content", ""));
        reminder.setEmploymentRate((String) data.getOrDefault("employmentRate", ""));
        if (data.get("totalStudents") != null) {
            reminder.setTotalStudents(Integer.valueOf(data.get("totalStudents").toString()));
        }
        if (data.get("employedStudents") != null) {
            reminder.setEmployedStudents(Integer.valueOf(data.get("employedStudents").toString()));
        }
        if (data.get("unemployedStudents") != null) {
            reminder.setUnemployedStudents(Integer.valueOf(data.get("unemployedStudents").toString()));
        }
        reminder.setStatus("unread");
        reminder.setIsRead("0");

        classEmploymentReminderRepository.save(reminder);
    }

    @Override
    public void markReminderAsRead(Long reminderId, Long userId) {
        ClassEmploymentReminder reminder = classEmploymentReminderRepository.findById(reminderId)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "提醒不存在"));
        if (!reminder.getReceiverId().equals(userId)) {
            throw new com.employment.exception.BusinessException(403, "无权操作");
        }
        reminder.setIsRead("1");
        classEmploymentReminderRepository.save(reminder);
    }

    @Override
    public int getUnreadReminderCount(Long userId) {
        return classEmploymentReminderRepository.countByReceiverIdAndIsRead(userId, "0");
    }

    @Override
    public Map<String, Object> batchNotifyAdvisors(Long userId, Integer graduationYear) {
        Map<String, Object> result = new LinkedHashMap<>();
        SysUser sender = sysUserRepository.findById(userId)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "用户不存在"));

        // 获取当前学年学生
        List<StudentInfo> targetStudents = new ArrayList<>();
        List<StudentInfo> allStudents = studentInfoRepository.findAll();
        for (StudentInfo s : allStudents) {
            if (graduationYear == null || (s.getGraduationYear() != null && s.getGraduationYear().equals(graduationYear))) {
                targetStudents.add(s);
            }
        }

        // 获取所有未就业学生及其班级信息
        List<Object[]> unemployedRows = employmentRecordRepository.findUnemployedStudents(graduationYear);
        Set<Long> unemployedStudentIds = unemployedRows.stream()
                .map(row -> ((Number) row[0]).longValue())
                .collect(Collectors.toSet());

        // 按班级分组统计
        Map<Long, List<StudentInfo>> byClass = new HashMap<>();
        for (StudentInfo s : targetStudents) {
            if (unemployedStudentIds.contains(s.getId()) && s.getClassId() != null) {
                byClass.computeIfAbsent(s.getClassId(), k -> new ArrayList<>()).add(s);
            }
        }

        // 获取班级信息（只有有未就业学生的班级才发通知）
        Map<Long, SysClass> classMap = new HashMap<>();
        List<Long> targetClassIds = new ArrayList<>(byClass.keySet());
        for (SysClass cls : sysClassRepository.findAllById(targetClassIds)) {
            if (cls.getAdvisorId() != null) {
                classMap.put(cls.getId(), cls);
            }
        }

        int sentCount = 0;
        int skipCount = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (Map.Entry<Long, List<StudentInfo>> entry : byClass.entrySet()) {
            Long classId = entry.getKey();
            List<StudentInfo> students = entry.getValue();
            SysClass cls = classMap.get(classId);

            if (cls == null || cls.getAdvisorId() == null) {
                skipCount += students.size();
                continue;
            }

            SysUser advisor = sysUserRepository.findById(cls.getAdvisorId()).orElse(null);
            if (advisor == null) {
                skipCount += students.size();
                continue;
            }

            // 计算班级就业数据
            long totalClassStudents = 0;
            long employedClassStudents = 0;
            for (StudentInfo s : targetStudents) {
                if (s.getClassId() != null && s.getClassId().equals(classId)) {
                    totalClassStudents++;
                    if (!unemployedStudentIds.contains(s.getId())) {
                        employedClassStudents++;
                    }
                }
            }
            double empRate = totalClassStudents > 0 ? (double) employedClassStudents / totalClassStudents * 100 : 0;

            // 构建通知内容
            String title = String.format("【就业帮扶提醒】%s 尚有 %d 名学生未登记就业", cls.getClassName(), students.size());
            String content = String.format("数据分析员 %s 发起的帮扶提醒：截止今日，%s 仍有 %d 名学生尚未完成就业登记，请重点关注并及时跟进帮扶。",
                    sender.getRealName(), cls.getClassName(), students.size());

            // 创建提醒记录
            ClassEmploymentReminder reminder = new ClassEmploymentReminder();
            reminder.setSenderId(userId);
            reminder.setSenderName(sender.getRealName());
            reminder.setReceiverId(advisor.getId());
            reminder.setReceiverName(advisor.getRealName());
            reminder.setClassId(classId);
            reminder.setClassName(cls.getClassName());
            reminder.setTitle(title);
            reminder.setContent(content);
            reminder.setEmploymentRate(String.format("%.1f", empRate) + "%");
            reminder.setTotalStudents((int) totalClassStudents);
            reminder.setEmployedStudents((int) employedClassStudents);
            reminder.setUnemployedStudents(students.size());
            reminder.setStatus("unread");
            reminder.setIsRead("0");
            classEmploymentReminderRepository.save(reminder);

            // 发送 WebSocket 实时通知
            try {
                NotificationService notificationService = SpringContextHolder.getBean(NotificationService.class);
                notificationService.sendNotification(advisor.getId(), title, content, "system", "system");
            } catch (Exception e) {
                // WebSocket 通知失败不影响主流程
            }

            sentCount += students.size();
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("className", cls.getClassName());
            detail.put("advisorName", advisor.getRealName());
            detail.put("unemployedCount", students.size());
            details.add(detail);
        }

        result.put("sentCount", sentCount);
        result.put("skipCount", skipCount);
        result.put("details", details);
        return result;
    }

    @Override
    public List<Map<String, Object>> getPendingCompanyAuths(Long userId, String role) {
        if (!Constants.ROLE_DEPT_TEACHER.equals(role)) {
            return Collections.emptyList();
        }

        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null || user.getDeptId() == null) {
            return Collections.emptyList();
        }

        List<CompanyAuth> allPending = companyAuthRepository.findByAuditStatus("pending");

        Long companyRoleId = sysRoleRepository.findByRoleKey("company")
                .map(SysRole::getId).orElse(null);
        if (companyRoleId == null) {
            return Collections.emptyList();
        }
        List<Long> companyUserIds = sysUserRoleRepository.findUserIdsByRoleId(companyRoleId);

        Map<Long, SysUser> companyUserMap = sysUserRepository.findAllById(companyUserIds).stream()
                .filter(u -> u.getDeptId() != null && u.getDeptId().equals(user.getDeptId()))
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        Map<Long, CompanyInfo> companyMap = companyInfoRepository.findAll().stream()
                .filter(c -> c.getUserId() != null && companyUserMap.containsKey(c.getUserId()))
                .collect(Collectors.toMap(CompanyInfo::getId, c -> c));

        Set<Long> targetCompanyIds = companyMap.keySet();

        return allPending.stream()
                .filter(auth -> targetCompanyIds.contains(auth.getCompanyId()))
                .map(auth -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", auth.getId());
                    map.put("companyId", auth.getCompanyId());
                    CompanyInfo company = companyMap.get(auth.getCompanyId());
                    if (company != null) {
                        map.put("companyName", company.getCompanyName());
                        map.put("contactPerson", company.getContactPerson());
                        map.put("contactPhone", company.getContactPhone());
                        if (company.getDeptId() != null) {
                            String deptName = sysDeptRepository.findById(company.getDeptId())
                                    .map(SysDept::getDeptName).orElse(null);
                            map.put("deptName", deptName);
                        }
                    }
                    map.put("authType", auth.getAuthType());
                    map.put("authName", auth.getAuthName());
                    map.put("filePath", auth.getFilePath());
                    map.put("auditStatus", auth.getAuditStatus());
                    map.put("createTime", auth.getCreateTime());
                    return map;
                }).collect(Collectors.toList());
    }

    @Override
    public void auditCompanyAuth(Long authId, String action, String remark, Long userId, String role) {
        if (!Constants.ROLE_DEPT_TEACHER.equals(role)) {
            throw new com.employment.exception.BusinessException(403, "只有院级老师可以审核企业入驻");
        }

        CompanyAuth auth = companyAuthRepository.findById(authId)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "认证记录不存在"));

        if ("approve".equals(action)) {
            auth.setAuditStatus("approved");
        } else if ("reject".equals(action)) {
            auth.setAuditStatus("rejected");
        } else {
            throw new com.employment.exception.BusinessException(400, "无效的审核操作");
        }

        auth.setAuditUserId(userId);
        auth.setAuditTime(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        if (StringUtils.hasText(remark)) {
            auth.setAuditRemark(remark);
        }

        companyAuthRepository.save(auth);

        if ("approve".equals(action)) {
            companyInfoRepository.findById(auth.getCompanyId()).ifPresent(company -> {
                company.setAuthStatus("approved");
                companyInfoRepository.save(company);
            });
        } else if ("reject".equals(action)) {
            companyInfoRepository.findById(auth.getCompanyId()).ifPresent(company -> {
                company.setAuthStatus("rejected");
                companyInfoRepository.save(company);
            });
        }
    }

    @Override
    public List<Map<String, Object>> getAgreements(Long userId, String role) {
        if (!Constants.ROLE_CLASS_TEACHER.equals(role) && !Constants.ROLE_DEPT_TEACHER.equals(role)) {
            return Collections.emptyList();
        }

        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) return Collections.emptyList();

        Set<Long> targetStudentIds = new HashSet<>();
        if (Constants.ROLE_CLASS_TEACHER.equals(role)) {
            Long classId = user.getClassId();
            if (classId == null) {
                List<SysClass> advisorClasses = sysClassRepository.findByAdvisorId(userId);
                if (!advisorClasses.isEmpty()) {
                    classId = advisorClasses.get(0).getId();
                }
            }
            if (classId != null) {
                targetStudentIds.addAll(
                    studentInfoRepository.findByClassId(classId).stream()
                        .map(StudentInfo::getId).collect(Collectors.toSet())
                );
            }
        } else if (Constants.ROLE_DEPT_TEACHER.equals(role)) {
            Long deptId = user.getDeptId();
            if (deptId != null) {
                targetStudentIds.addAll(
                    studentInfoRepository.findByDeptId(deptId).stream()
                        .map(StudentInfo::getId).collect(Collectors.toSet())
                );
            }
        }

        if (targetStudentIds.isEmpty()) return Collections.emptyList();

        List<TripartiteAgreement> agreements = tripartiteAgreementRepository.findAllByStudentIds(
                new ArrayList<>(targetStudentIds)
        );

        Map<Long, StudentInfo> studentMap = studentInfoRepository.findAllById(
            agreements.stream().map(TripartiteAgreement::getStudentId).collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(StudentInfo::getId, s -> s));

        Map<Long, CompanyInfo> companyMap = companyInfoRepository.findAll().stream()
            .collect(Collectors.toMap(CompanyInfo::getId, c -> c));

        return agreements.stream().map(a -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("studentId", a.getStudentId());
            map.put("companyId", a.getCompanyId());
            map.put("agreementNo", a.getAgreementNo());
            map.put("studentSignTime", a.getStudentSignTime());
            map.put("companySignTime", a.getCompanySignTime());
            map.put("schoolSignTime", a.getSchoolSignTime());
            map.put("status", a.getStatus());
            map.put("createTime", a.getCreateTime());
            StudentInfo s = studentMap.get(a.getStudentId());
            if (s != null) {
                map.put("studentName", s.getRealName());
                map.put("studentNo", s.getStudentNo());
                map.put("className", s.getClassName());
            }
            CompanyInfo c = companyMap.get(a.getCompanyId());
            if (c != null) {
                map.put("companyName", c.getCompanyName());
                map.put("companyScale", c.getScale());
                map.put("companyIndustry", c.getIndustry());
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public void signAgreement(Long id, Long userId, String role) {
        if (!Constants.ROLE_CLASS_TEACHER.equals(role) && !Constants.ROLE_DEPT_TEACHER.equals(role)) {
            throw new com.employment.exception.BusinessException(403, "无权签署协议");
        }

        TripartiteAgreement agreement = tripartiteAgreementRepository.findById(id)
            .orElseThrow(() -> new com.employment.exception.BusinessException(404, "协议不存在"));

        if (!"company_signed".equals(agreement.getStatus())) {
            throw new com.employment.exception.BusinessException(400, "当前状态下学校无法签署，请确认学生和企业已签署");
        }

        String now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        agreement.setSchoolSignTime(now);
        agreement.setStatus("completed");
        tripartiteAgreementRepository.save(agreement);

        List<JobApplication> applications = jobApplicationRepository.findByStudentId(agreement.getStudentId());
        for (JobApplication app : applications) {
            if (app.getCompanyId().equals(agreement.getCompanyId())) {
                app.setStatus("accepted");
                app.setOfferStatus("accepted");
                jobApplicationRepository.save(app);
                log.info("三方协议签署完成，自动更新申请 {} 状态为 accepted（已入职）", app.getId());
                break;
            }
        }

        EmploymentRecord empRecord = null;
        if (agreement.getEmploymentRecordId() != null) {
            empRecord = employmentRecordRepository.findById(agreement.getEmploymentRecordId()).orElse(null);
        }
        if (empRecord == null) {
            List<EmploymentRecord> empRecords = employmentRecordRepository.findByStudentId(agreement.getStudentId());
            for (EmploymentRecord record : empRecords) {
                if (agreement.getAgreementNo().equals(record.getThreePartyNo())
                        || (record.getThreePartyNo() != null && record.getThreePartyNo().contains(agreement.getAgreementNo()))) {
                    empRecord = record;
                    break;
                }
            }
        }
        if (empRecord != null && "pending".equals(empRecord.getAuditStatus())) {
            empRecord.setAuditStatus("approved");
            empRecord.setAuditUserId(userId);
            empRecord.setAuditTime(now);
            empRecord.setAuditRemark("三方协议签署完成，系统自动审核通过");
            employmentRecordRepository.save(empRecord);
            log.info("三方协议签署完成，自动审核通过就业记录 {}", empRecord.getId());
            StudentInfo empStu = studentInfoRepository.findById(agreement.getStudentId()).orElse(null);
            if (empStu != null) {
                statsCacheService.evictByStudent(empStu.getClassId(), empStu.getDeptId());
            }
        }

        StudentInfo s = studentInfoRepository.findById(agreement.getStudentId()).orElse(null);
        if (s != null) {
            notificationService.sendNotification(s.getUserId(),
                "三方协议已全部签署完成",
                "您与" + (companyInfoRepository.findById(agreement.getCompanyId())
                    .map(CompanyInfo::getCompanyName).orElse("企业")) + "的三方协议已由学校签署完毕，您已正式入职！",
                "agreement", "agreement");
        }
    }

    @Override
    public List<Map<String, Object>> getAgreementStats(Long userId, String role) {
        List<Map<String, Object>> agreements = getAgreements(userId, role);
        long total = agreements.size();
        long pending = agreements.stream().filter(a -> "pending".equals(a.get("status")) || "student_signed".equals(a.get("status"))).count();
        long companySigned = agreements.stream().filter(a -> "company_signed".equals(a.get("status"))).count();
        long completed = agreements.stream().filter(a -> "completed".equals(a.get("status"))).count();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("companySigned", companySigned);
        stats.put("completed", completed);
        return Collections.singletonList(stats);
    }

    @Override
    public Map<String, Object> getDashboard(Long userId, String role) {
        Map<String, Object> dashboard = new LinkedHashMap<>();

        Map<String, Object> myClass = getMyClass(userId);
        dashboard.put("myClass", myClass);

        Long totalStudents = 0L;
        if (Constants.ROLE_CLASS_TEACHER.equals(role)) {
            Long classId = resolveClassId(userId);
            totalStudents = classId != null ? studentInfoRepository.countByClassId(classId) : 0L;
        } else if (Constants.ROLE_DEPT_TEACHER.equals(role)) {
            SysUser user = sysUserRepository.findById(userId).orElse(null);
            totalStudents = user != null && user.getDeptId() != null
                    ? studentInfoRepository.countByDeptId(user.getDeptId()) : 0L;
        }
        dashboard.put("totalStudents", totalStudents);

        Map<String, Object> employmentStats = getEmploymentStats(userId, role);
        dashboard.put("employmentStats", employmentStats);

        long pendingCount = 0L;
        if (Constants.ROLE_CLASS_TEACHER.equals(role)) {
            Long classId = resolveClassId(userId);
            if (classId != null) {
                pendingCount = employmentRecordRepository.countPendingByClassIds(Collections.singletonList(classId));
            }
        } else if (Constants.ROLE_DEPT_TEACHER.equals(role)) {
            SysUser user = sysUserRepository.findById(userId).orElse(null);
            if (user != null && user.getDeptId() != null) {
                pendingCount = employmentRecordRepository.countPendingByDeptIds(Collections.singletonList(user.getDeptId()));
            }
        }
        dashboard.put("pendingEmploymentCount", pendingCount);

        int unreadReminders = getUnreadReminderCount(userId);
        dashboard.put("unreadReminderCount", unreadReminders);

        List<Map<String, Object>> agreementStats = getAgreementStats(userId, role);
        if (!agreementStats.isEmpty()) {
            dashboard.put("agreementStats", agreementStats.get(0));
        }

        if (Constants.ROLE_DEPT_TEACHER.equals(role)) {
            List<Map<String, Object>> classes = getClasses(userId, role);
            dashboard.put("classes", classes);
            dashboard.put("classCount", classes.size());
        }

        return dashboard;
    }

    private Long resolveClassId(Long userId) {
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) return null;
        Long classId = user.getClassId();
        if (classId == null) {
            List<SysClass> advisorClasses = sysClassRepository.findByAdvisorId(userId);
            if (!advisorClasses.isEmpty()) classId = advisorClasses.get(0).getId();
        }
        return classId;
    }

    /**
     * 就业明细（分页），用 SQL JOIN 替代全量内存加载。
     * 先分页查出学生 ID，再批量查就业记录，避免 N+1。
     */
    @Override
    public Map<String, Object> getEmploymentDetail(Long userId, String role, Long classId, Integer page, Integer size) {
        SysUser user = sysUserRepository.findById(userId).orElse(null);
        if (user == null) return Collections.emptyMap();

        List<StudentInfo> students;
        long total;
        if (classId != null) {
            // 指定班级
            students = studentInfoRepository.findByClassId(classId);
            total = students.size();
        } else if (Constants.ROLE_CLASS_TEACHER.equals(role)) {
            Long myClassId = resolveClassId(userId);
            students = myClassId != null ? studentInfoRepository.findByClassId(myClassId) : Collections.emptyList();
            total = students.size();
        } else if (Constants.ROLE_DEPT_TEACHER.equals(role)) {
            Long deptId = user.getDeptId();
            students = deptId != null ? studentInfoRepository.findByDeptId(deptId) : Collections.emptyList();
            total = students.size();
        } else {
            return Collections.emptyMap();
        }

        // 分页
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, (int) total);
        List<StudentInfo> pagedStudents = (fromIndex >= students.size()) ? Collections.emptyList() : students.subList(fromIndex, toIndex);
        List<Long> pagedIds = pagedStudents.stream().map(StudentInfo::getId).collect(Collectors.toList());

        // 批量查就业记录（每个学生只取最新一条 approved）
        Map<Long, EmploymentRecord> latestMap = new LinkedHashMap<>();
        if (!pagedIds.isEmpty()) {
            List<EmploymentRecord> allRecords = employmentRecordRepository.findByStudentIdIn(pagedIds);
            Map<Long, List<EmploymentRecord>> grouped = allRecords.stream()
                    .collect(Collectors.groupingBy(EmploymentRecord::getStudentId));
            for (Long sid : pagedIds) {
                List<EmploymentRecord> recs = grouped.get(sid);
                if (recs == null || recs.isEmpty()) continue;
                recs.sort((a, b) -> {
                    if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
                    if (a.getCreateTime() == null) return 1;
                    if (b.getCreateTime() == null) return -1;
                    return b.getCreateTime().compareTo(a.getCreateTime());
                });
                EmploymentRecord latest = recs.get(0);
                if ("approved".equals(latest.getAuditStatus())) {
                    latestMap.put(sid, latest);
                }
            }
        }

        List<Map<String, Object>> detailList = pagedStudents.stream().map(s -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", s.getId());
            row.put("studentNo", s.getStudentNo());
            row.put("realName", s.getRealName());
            row.put("gender", s.getGender());
            EmploymentRecord rec = latestMap.get(s.getId());
            if (rec != null) {
                row.put("employmentType", rec.getEmploymentType());
                row.put("companyName", rec.getCompanyName());
                row.put("workProvince", rec.getWorkProvince());
                row.put("workCity", rec.getWorkCity());
                row.put("positionName", rec.getPositionName());
                row.put("salary", rec.getSalary());
                row.put("industry", rec.getCompanyIndustry());
                row.put("auditStatus", rec.getAuditStatus());
            } else {
                row.put("employmentType", null);
                row.put("companyName", null);
                row.put("workProvince", null);
                row.put("workCity", null);
                row.put("positionName", null);
                row.put("salary", null);
                row.put("industry", null);
                row.put("auditStatus", "unemployed");
            }
            return row;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", detailList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * 就业明细（全量），用于导出。限制最多 10000 条，避免内存溢出。
     */
    @Override
    public Map<String, Object> getEmploymentDetailAll(Long userId, String role, Long classId) {
        Map<String, Object> result = getEmploymentDetail(userId, role, classId, 1, 10000);
        result.put("page", 1);
        result.put("size", 10000);
        return result;
    }
}
