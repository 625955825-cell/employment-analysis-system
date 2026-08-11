package com.employment.controller;

import com.employment.common.PageResult;
import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.entity.CompanyInfo;
import com.employment.model.entity.JobPosition;
import com.employment.repository.CompanyInfoRepository;
import com.employment.repository.JobPositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/job")
@RequiredArgsConstructor
public class AdminJobController {

    private final JobPositionRepository jobPositionRepository;
    private final CompanyInfoRepository companyInfoRepository;

    /**
     * 职位列表（分页）
     */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean exportAll) {

        List<JobPosition> all = jobPositionRepository.findAll();
        all.removeIf(j -> "1".equals(j.getIsDeleted()));

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            all.removeIf(j -> {
                String name = j.getJobName() != null ? j.getJobName().toLowerCase() : "";
                String companyName = j.getCompanyName() != null ? j.getCompanyName().toLowerCase() : "";
                return !name.contains(kw) && !companyName.contains(kw);
            });
        }
        if (companyId != null) {
            all.removeIf(j -> !companyId.equals(j.getCompanyId()));
        }
        if (status != null && !status.isEmpty()) {
            all.removeIf(j -> !status.equals(j.getStatus()));
        }

        all.sort((a, b) -> {
            if (b.getCreateTime() == null && a.getCreateTime() == null) return 0;
            if (b.getCreateTime() == null) return 1;
            if (a.getCreateTime() == null) return -1;
            return b.getCreateTime().compareTo(a.getCreateTime());
        });

        // 导出全部：不分页
        if (Boolean.TRUE.equals(exportAll)) {
            Set<Long> companyIds = new HashSet<>();
            all.forEach(j -> { if (j.getCompanyId() != null) companyIds.add(j.getCompanyId()); });
            Map<Long, CompanyInfo> companyMap = new HashMap<>();
            if (!companyIds.isEmpty()) {
                companyInfoRepository.findAllById(companyIds).forEach(c -> companyMap.put(c.getId(), c));
            }
            List<Map<String, Object>> records = all.stream().map(j -> {
                Map<String, Object> m = toJobMap(j);
                CompanyInfo company = companyMap.get(j.getCompanyId());
                if (company != null) {
                    m.put("companyIndustry", company.getIndustry());
                    m.put("companyScale", company.getScale());
                    m.put("companyCity", company.getCity());
                }
                return m;
            }).collect(Collectors.toList());
            return Result.success(new PageResult<>((long) all.size(), records, 1L, (long) all.size()));
        }

        int total = all.size();
        int start = Math.min((page - 1) * size, total);
        int end = Math.min(start + size, total);

        // 批量加载企业信息
        Set<Long> companyIds = new HashSet<>();
        for (JobPosition j : all) {
            if (j.getCompanyId() != null) companyIds.add(j.getCompanyId());
        }
        Map<Long, CompanyInfo> companyMap = new HashMap<>();
        if (!companyIds.isEmpty()) {
            companyInfoRepository.findAllById(companyIds).forEach(c -> companyMap.put(c.getId(), c));
        }

        List<Map<String, Object>> records = new ArrayList<>();
        if (start < total) {
            for (JobPosition j : all.subList(start, end)) {
                Map<String, Object> m = toJobMap(j);
                CompanyInfo company = companyMap.get(j.getCompanyId());
                if (company != null) {
                    m.put("companyIndustry", company.getIndustry());
                    m.put("companyScale", company.getScale());
                    m.put("companyCity", company.getCity());
                    m.put("companyContact", company.getContactPerson());
                    m.put("companyPhone", company.getContactPhone());
                }
                records.add(m);
            }
        }

        return Result.success(new PageResult<>((long) total, records, (long) page, (long) size));
    }

    /**
     * 获取单个职位详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        JobPosition j = jobPositionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("职位不存在"));
        Map<String, Object> m = toJobMap(j);
        CompanyInfo company = companyInfoRepository.findById(j.getCompanyId()).orElse(null);
        if (company != null) {
            m.put("companyIndustry", company.getIndustry());
            m.put("companyScale", company.getScale());
            m.put("companyCity", company.getCity());
            m.put("companyContact", company.getContactPerson());
            m.put("companyPhone", company.getContactPhone());
            m.put("companyEmail", company.getContactEmail());
            m.put("companyAddress", company.getAddress());
            m.put("companyIntroduction", company.getIntroduction());
        }
        return Result.success(m);
    }

    /**
     * 新增职位（管理员直接录入）
     */
    @PostMapping
    @OperationLog(module = "职位管理", content = "新增职位")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> data) {
        Long companyId = Long.valueOf(data.get("companyId").toString());
        CompanyInfo company = companyInfoRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("企业不存在"));

        JobPosition j = new JobPosition();
        j.setCompanyId(companyId);
        j.setCompanyName(company.getCompanyName());
        applyJobData(j, data);
        j.setIsDeleted("0");
        j.setViewCount(0);
        j.setApplyCount(0);
        j.setStatus(data.get("status") != null ? (String) data.get("status") : "published");

        String now = java.time.LocalDate.now().toString();
        j.setPublishTime(now);
        if (j.getDeadline() == null) {
            j.setDeadline(java.time.LocalDate.now().plusDays(30).toString());
        }

        JobPosition saved = jobPositionRepository.save(j);
        return Result.success(toJobMap(saved));
    }

    /**
     * 更新职位
     */
    @PutMapping("/{id}")
    @OperationLog(module = "职位管理", content = "编辑职位")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        JobPosition j = jobPositionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("职位不存在"));
        applyJobData(j, data);
        JobPosition saved = jobPositionRepository.save(j);
        return Result.success(toJobMap(saved));
    }

    /**
     * 上架职位
     */
    @PutMapping("/{id}/publish")
    @OperationLog(module = "职位管理", content = "上架职位")
    public Result<Void> publish(@PathVariable Long id) {
        JobPosition j = jobPositionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("职位不存在"));
        j.setStatus("published");
        jobPositionRepository.save(j);
        return Result.success();
    }

    /**
     * 下架职位
     */
    @PutMapping("/{id}/pause")
    @OperationLog(module = "职位管理", content = "下架职位")
    public Result<Void> pause(@PathVariable Long id) {
        JobPosition j = jobPositionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("职位不存在"));
        j.setStatus("paused");
        jobPositionRepository.save(j);
        return Result.success();
    }

    /**
     * 删除职位（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @OperationLog(module = "职位管理", content = "删除职位")
    public Result<Void> delete(@PathVariable Long id) {
        JobPosition j = jobPositionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("职位不存在"));
        j.setIsDeleted("1");
        jobPositionRepository.save(j);
        return Result.success();
    }

    /**
     * 统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        List<JobPosition> all = jobPositionRepository.findAll();
        all.removeIf(j -> "1".equals(j.getIsDeleted()));
        long total = all.size();
        long published = all.stream().filter(j -> "published".equals(j.getStatus())).count();
        long paused = all.stream().filter(j -> "paused".equals(j.getStatus())).count();
        long totalViews = all.stream().mapToLong(j -> j.getViewCount() != null ? j.getViewCount() : 0).sum();
        long totalApplies = all.stream().mapToLong(j -> j.getApplyCount() != null ? j.getApplyCount() : 0).sum();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("published", published);
        result.put("paused", paused);
        result.put("totalViews", totalViews);
        result.put("totalApplies", totalApplies);
        return Result.success(result);
    }

    private void applyJobData(JobPosition j, Map<String, Object> data) {
        if (data.get("jobName") != null) j.setJobName((String) data.get("jobName"));
        if (data.get("jobCategory") != null) j.setJobCategory((String) data.get("jobCategory"));
        if (data.get("jobType") != null) j.setJobType((String) data.get("jobType"));
        if (data.get("workCity") != null) j.setWorkCity((String) data.get("workCity"));
        if (data.get("workAddress") != null) j.setWorkAddress((String) data.get("workAddress"));
        if (data.get("salaryMin") != null) j.setSalaryMin(Integer.valueOf(data.get("salaryMin").toString()));
        if (data.get("salaryMax") != null) j.setSalaryMax(Integer.valueOf(data.get("salaryMax").toString()));
        if (data.get("salaryMonths") != null) j.setSalaryMonths((String) data.get("salaryMonths"));
        if (data.get("recruitNumber") != null) j.setRecruitNumber(Integer.valueOf(data.get("recruitNumber").toString()));
        if (data.get("requirement") != null) j.setRequirement((String) data.get("requirement"));
        if (data.get("responsibility") != null) j.setResponsibility((String) data.get("responsibility"));
        if (data.get("benefits") != null) j.setBenefits((String) data.get("benefits"));
        if (data.get("educationRequired") != null) j.setEducationRequired((String) data.get("educationRequired"));
        if (data.get("experienceRequired") != null) j.setExperienceRequired((String) data.get("experienceRequired"));
        if (data.get("skillRequired") != null) j.setSkillRequired((String) data.get("skillRequired"));
        if (data.get("isRemote") != null) j.setIsRemote((String) data.get("isRemote"));
        if (data.get("isHighSalary") != null) j.setIsHighSalary((String) data.get("isHighSalary"));
        if (data.get("deadline") != null) j.setDeadline((String) data.get("deadline"));
    }

    private Map<String, Object> toJobMap(JobPosition j) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", j.getId());
        m.put("companyId", j.getCompanyId());
        m.put("companyName", j.getCompanyName());
        m.put("jobName", j.getJobName());
        m.put("jobCategory", j.getJobCategory());
        m.put("jobType", j.getJobType());
        m.put("workCity", j.getWorkCity());
        m.put("workAddress", j.getWorkAddress());
        m.put("salaryMin", j.getSalaryMin());
        m.put("salaryMax", j.getSalaryMax());
        m.put("salaryMonths", j.getSalaryMonths());
        m.put("salaryText", buildSalaryText(j.getSalaryMin(), j.getSalaryMax()));
        m.put("recruitNumber", j.getRecruitNumber());
        m.put("requirement", j.getRequirement());
        m.put("responsibility", j.getResponsibility());
        m.put("benefits", j.getBenefits());
        m.put("educationRequired", j.getEducationRequired());
        m.put("experienceRequired", j.getExperienceRequired());
        m.put("skillRequired", j.getSkillRequired());
        m.put("isRemote", j.getIsRemote());
        m.put("isHighSalary", j.getIsHighSalary());
        m.put("viewCount", j.getViewCount());
        m.put("applyCount", j.getApplyCount());
        m.put("status", j.getStatus());
        m.put("publishTime", j.getPublishTime());
        m.put("deadline", j.getDeadline());
        m.put("createTime", j.getCreateTime());
        return m;
    }

    private String buildSalaryText(Integer min, Integer max) {
        if (min == null && max == null) return "面议";
        if (min != null && max != null) return min + "-" + max + "元/月";
        if (min != null) return min + "元/月以上";
        return "最高" + max + "元/月";
    }
}
