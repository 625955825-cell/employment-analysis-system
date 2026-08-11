package com.employment.controller;

import com.employment.common.PageResult;
import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.entity.CompanyAuth;
import com.employment.model.entity.CompanyInfo;
import com.employment.repository.CompanyAuthRepository;
import com.employment.repository.CompanyInfoRepository;
import com.employment.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/company")
@RequiredArgsConstructor
public class AdminCompanyController {

    private final CompanyInfoRepository companyInfoRepository;
    private final CompanyAuthRepository companyAuthRepository;
    private final SysUserRepository sysUserRepository;

    /**
     * 企业列表（分页）
     * @param page   页码（从1开始）
     * @param size   每页数量
     * @param keyword 搜索关键字（企业名称/联系人）
     * @param status 认证状态：pending/approved/rejected
     */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean exportAll) {

        List<CompanyInfo> all = companyInfoRepository.findAll();
        if (status != null && !status.isEmpty()) {
            all.removeIf(c -> !status.equals(c.getAuthStatus()));
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            all.removeIf(c -> {
                String name = c.getCompanyName() != null ? c.getCompanyName().toLowerCase() : "";
                String contact = c.getContactPerson() != null ? c.getContactPerson().toLowerCase() : "";
                return !name.contains(kw) && !contact.contains(kw);
            });
        }

        all.sort((a, b) -> {
            if (b.getCreateTime() == null && a.getCreateTime() == null) return 0;
            if (b.getCreateTime() == null) return 1;
            if (a.getCreateTime() == null) return -1;
            return b.getCreateTime().compareTo(a.getCreateTime());
        });

        // 导出全部：不分页，直接返回所有记录
        if (Boolean.TRUE.equals(exportAll)) {
            List<Map<String, Object>> records = all.stream().map(this::toCompanyMap).collect(Collectors.toList());
            return Result.success(new PageResult<>((long) all.size(), records, 1L, (long) all.size()));
        }

        int total = all.size();
        int start = Math.min((page - 1) * size, total);
        int end = Math.min(start + size, total);

        List<Map<String, Object>> records = new ArrayList<>();
        if (start < total) {
            for (CompanyInfo c : all.subList(start, end)) {
                records.add(toCompanyMap(c));
            }
        }

        return Result.success(new PageResult<>((long) total, records, (long) page, (long) size));
    }

    /**
     * 获取单个企业详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        CompanyInfo c = companyInfoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("企业不存在"));
        return Result.success(toCompanyMap(c));
    }

    /**
     * 新增企业（管理员直接录入，不走申请流程）
     */
    @PostMapping
    @OperationLog(module = "企业管理", content = "新增企业")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> data) {
        CompanyInfo c = new CompanyInfo();

        if (data.get("companyName") != null) c.setCompanyName((String) data.get("companyName"));
        if (data.get("companyCode") != null) c.setCompanyCode((String) data.get("companyCode"));
        if (data.get("unifiedCreditCode") != null) c.setUnifiedCreditCode((String) data.get("unifiedCreditCode"));
        if (data.get("legalPerson") != null) c.setLegalPerson((String) data.get("legalPerson"));
        if (data.get("contactPerson") != null) c.setContactPerson((String) data.get("contactPerson"));
        if (data.get("contactPhone") != null) c.setContactPhone((String) data.get("contactPhone"));
        if (data.get("contactEmail") != null) c.setContactEmail((String) data.get("contactEmail"));
        if (data.get("province") != null) c.setProvince((String) data.get("province"));
        if (data.get("city") != null) c.setCity((String) data.get("city"));
        if (data.get("district") != null) c.setDistrict((String) data.get("district"));
        if (data.get("address") != null) c.setAddress((String) data.get("address"));
        if (data.get("industry") != null) c.setIndustry((String) data.get("industry"));
        if (data.get("scale") != null) c.setScale((String) data.get("scale"));
        if (data.get("nature") != null) c.setNature((String) data.get("nature"));
        if (data.get("introduction") != null) c.setIntroduction((String) data.get("introduction"));
        if (data.get("website") != null) c.setWebsite((String) data.get("website"));
        if (data.get("deptId") != null) c.setDeptId(Long.valueOf(data.get("deptId").toString()));

        c.setAuthStatus("approved");
        c.setStatus("0");

        CompanyInfo saved = companyInfoRepository.save(c);
        return Result.success(toCompanyMap(saved));
    }

    /**
     * 更新企业信息
     */
    @PutMapping("/{id}")
    @OperationLog(module = "企业管理", content = "编辑企业")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        CompanyInfo c = companyInfoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("企业不存在"));

        if (data.get("companyName") != null) c.setCompanyName((String) data.get("companyName"));
        if (data.get("companyCode") != null) c.setCompanyCode((String) data.get("companyCode"));
        if (data.get("unifiedCreditCode") != null) c.setUnifiedCreditCode((String) data.get("unifiedCreditCode"));
        if (data.get("legalPerson") != null) c.setLegalPerson((String) data.get("legalPerson"));
        if (data.get("contactPerson") != null) c.setContactPerson((String) data.get("contactPerson"));
        if (data.get("contactPhone") != null) c.setContactPhone((String) data.get("contactPhone"));
        if (data.get("contactEmail") != null) c.setContactEmail((String) data.get("contactEmail"));
        if (data.get("province") != null) c.setProvince((String) data.get("province"));
        if (data.get("city") != null) c.setCity((String) data.get("city"));
        if (data.get("district") != null) c.setDistrict((String) data.get("district"));
        if (data.get("address") != null) c.setAddress((String) data.get("address"));
        if (data.get("industry") != null) c.setIndustry((String) data.get("industry"));
        if (data.get("scale") != null) c.setScale((String) data.get("scale"));
        if (data.get("nature") != null) c.setNature((String) data.get("nature"));
        if (data.get("introduction") != null) c.setIntroduction((String) data.get("introduction"));
        if (data.get("website") != null) c.setWebsite((String) data.get("website"));
        if (data.get("deptId") != null) c.setDeptId(Long.valueOf(data.get("deptId").toString()));
        if (data.get("status") != null) c.setStatus((String) data.get("status"));

        CompanyInfo saved = companyInfoRepository.save(c);
        return Result.success(toCompanyMap(saved));
    }

    /**
     * 删除企业
     */
    @DeleteMapping("/{id}")
    @OperationLog(module = "企业管理", content = "删除企业")
    public Result<Void> delete(@PathVariable Long id) {
        companyInfoRepository.deleteById(id);
        return Result.success();
    }

    /**
     * 审核企业入驻申请
     * @param action approve / reject
     */
    @PutMapping("/{id}/audit")
    @OperationLog(module = "企业管理", content = "审核企业入驻")
    public Result<Void> audit(@PathVariable Long id, @RequestParam String action, @RequestParam(required = false) String remark) {
        CompanyInfo c = companyInfoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("企业不存在"));

        if ("approve".equals(action)) {
            c.setAuthStatus("approved");
        } else if ("reject".equals(action)) {
            c.setAuthStatus("rejected");
        } else {
            return Result.error("无效的审核操作");
        }
        companyInfoRepository.save(c);

        // 同时更新最新的认证记录
        List<CompanyAuth> auths = companyAuthRepository.findByCompanyId(id);
        if (!auths.isEmpty()) {
            CompanyAuth latest = auths.stream()
                    .max((a, b) -> {
                        if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
                        if (a.getCreateTime() == null) return 1;
                        if (b.getCreateTime() == null) return -1;
                        return a.getCreateTime().compareTo(b.getCreateTime());
                    })
                    .orElse(auths.get(0));
            latest.setAuditStatus("approved".equals(action) ? "approved" : "rejected");
            latest.setAuditRemark(remark);
            latest.setAuditTime(java.time.LocalDateTime.now().toString());
            companyAuthRepository.save(latest);
        }

        return Result.success();
    }

    /**
     * 获取认证记录（某个企业的入驻申请历史）
     */
    @GetMapping("/{id}/auth-records")
    public Result<List<Map<String, Object>>> getAuthRecords(@PathVariable Long id) {
        List<CompanyAuth> auths = companyAuthRepository.findByCompanyId(id);
        auths.sort((a, b) -> {
            if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
            if (a.getCreateTime() == null) return 1;
            if (b.getCreateTime() == null) return -1;
            return b.getCreateTime().compareTo(a.getCreateTime());
        });
        List<Map<String, Object>> result = new ArrayList<>();
        for (CompanyAuth a : auths) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("authType", a.getAuthType());
            m.put("authName", a.getAuthName());
            m.put("filePath", a.getFilePath());
            m.put("auditStatus", a.getAuditStatus());
            m.put("auditRemark", a.getAuditRemark());
            m.put("auditTime", a.getAuditTime());
            m.put("createTime", a.getCreateTime());
            result.add(m);
        }
        return Result.success(result);
    }

    /**
     * 统计：各状态企业数量
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        List<CompanyInfo> all = companyInfoRepository.findAll();
        long pending = all.stream().filter(c -> "pending".equals(c.getAuthStatus())).count();
        long approved = all.stream().filter(c -> "approved".equals(c.getAuthStatus())).count();
        long rejected = all.stream().filter(c -> "rejected".equals(c.getAuthStatus())).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", all.size());
        result.put("pending", pending);
        result.put("approved", approved);
        result.put("rejected", rejected);
        return Result.success(result);
    }

    private Map<String, Object> toCompanyMap(CompanyInfo c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("userId", c.getUserId());
        m.put("companyName", c.getCompanyName());
        m.put("companyCode", c.getCompanyCode());
        m.put("unifiedCreditCode", c.getUnifiedCreditCode());
        m.put("legalPerson", c.getLegalPerson());
        m.put("contactPerson", c.getContactPerson());
        m.put("contactPhone", c.getContactPhone());
        m.put("contactEmail", c.getContactEmail());
        m.put("province", c.getProvince());
        m.put("city", c.getCity());
        m.put("district", c.getDistrict());
        m.put("address", c.getAddress());
        m.put("industry", c.getIndustry());
        m.put("scale", c.getScale());
        m.put("nature", c.getNature());
        m.put("introduction", c.getIntroduction());
        m.put("website", c.getWebsite());
        m.put("logo", c.getLogo());
        m.put("businessLicense", c.getBusinessLicense());
        m.put("authStatus", c.getAuthStatus());
        m.put("status", c.getStatus());
        m.put("deptId", c.getDeptId());
        m.put("createTime", c.getCreateTime());
        return m;
    }
}
