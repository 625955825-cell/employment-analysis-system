package com.employment.controller;

import com.employment.common.PageResult;
import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.dto.JobSearchDTO;
import com.employment.model.entity.CompanyInfo;
import com.employment.model.entity.JobPosition;
import com.employment.repository.CompanyInfoRepository;
import com.employment.security.SecurityUtils;
import com.employment.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyInfoRepository companyInfoRepository;
    private final SecurityUtils securityUtils;

    @GetMapping("/profile")
    public Result<CompanyInfo> getProfile() {
        Long userId = securityUtils.getCurrentUserId();
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("企业信息不存在"));
        return Result.success(company);
    }

    @PutMapping("/profile")
    @OperationLog(module = "企业管理", content = "更新企业资料")
    public Result<CompanyInfo> updateProfile(@RequestBody CompanyInfo companyInfo) {
        Long userId = securityUtils.getCurrentUserId();
        CompanyInfo updated = companyService.updateProfile(userId, companyInfo);
        return Result.success(updated);
    }

    @GetMapping("/home-stats")
    public Result<Map<String, Object>> getHomeStats() {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(companyService.getHomeStats(userId));
    }

    @GetMapping("/resumes")
    public Result<PageResult<?>> getResumes(JobSearchDTO dto) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(companyService.getReceivedResumes(userId, dto));
    }

    @PutMapping("/application/{id}/status")
    public Result<Void> updateApplicationStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String companyRemark) {
        companyService.updateApplicationStatus(id, status, companyRemark);
        return Result.success();
    }

    @GetMapping("/interviews")
    public Result<PageResult<?>> getInterviews(JobSearchDTO dto) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(companyService.getCompanyInterviews(userId, dto));
    }

    @PostMapping("/interview")
    @OperationLog(module = "企业管理", content = "发起面试邀请")
    public Result<Void> createInterview(@RequestBody Map<String, Object> data) {
        companyService.createInterview(securityUtils.getCurrentUserId(), data);
        return Result.success();
    }

    @PutMapping("/interview/{id}/cancel")
    public Result<Void> cancelInterview(@PathVariable Long id) {
        companyService.cancelInterview(id, securityUtils.getCurrentUserId());
        return Result.success();
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(companyService.getStatistics(userId, startDate, endDate));
    }

    // ==================== 职位管理 ====================

    @GetMapping("/jobs")
    public Result<PageResult<?>> getMyJobs(@RequestParam Map<String, Object> params) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(companyService.getMyJobs(userId, params));
    }

    @GetMapping("/job/{id}")
    public Result<Map<String, Object>> getJobDetail(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(companyService.getJobDetail(userId, id));
    }

    @PostMapping("/job")
    @OperationLog(module = "企业管理", content = "发布职位")
    public Result<JobPosition> createJob(@RequestBody Map<String, Object> data) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(companyService.createJob(userId, data));
    }

    @PutMapping("/job/{id}")
    @OperationLog(module = "企业管理", content = "编辑职位")
    public Result<JobPosition> updateJob(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(companyService.updateJob(userId, id, data));
    }

    @PutMapping("/job/{id}/publish")
    @OperationLog(module = "企业管理", content = "上架职位")
    public Result<Void> publishJob(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        companyService.publishJob(userId, id);
        return Result.success();
    }

    @PutMapping("/job/{id}/pause")
    @OperationLog(module = "企业管理", content = "下架职位")
    public Result<Void> pauseJob(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        companyService.pauseJob(userId, id);
        return Result.success();
    }

    @DeleteMapping("/job/{id}")
    @OperationLog(module = "企业管理", content = "删除职位")
    public Result<Void> deleteJob(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        companyService.deleteJob(userId, id);
        return Result.success();
    }

    @PostMapping("/applications/delete-batch")
    public Result<String> deleteApplicationsBatch(@RequestBody Map<String, Object> data) {
        Long userId = securityUtils.getCurrentUserId();
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) data.get("ids");
        if (rawIds == null || rawIds.isEmpty()) {
            return Result.error("请选择要删除的记录");
        }
        List<Long> ids = rawIds.stream()
                .map(id -> id instanceof Long ? (Long) id : Long.valueOf(id.toString()))
                .collect(Collectors.toList());
        companyService.deleteApplications(userId, ids);
        return Result.success("已删除" + ids.size() + "条记录");
    }

    @PostMapping("/reapply")
    public Result<CompanyInfo> reApply(@RequestBody Map<String, Object> data) {
        Long userId = securityUtils.getCurrentUserId();
        CompanyInfo updated = companyService.reApply(userId, data);
        return Result.success("重新申请已提交，请等待审核", updated);
    }

    // ==================== Offer管理 ====================

    @GetMapping("/offers")
    public Result<Map<String, Object>> getOffers(@RequestParam Map<String, Object> params) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(companyService.getMyOffers(userId, params));
    }

    @PostMapping("/offer")
    @OperationLog(module = "企业管理", content = "发放Offer")
    public Result<Map<String, Object>> sendOffer(@RequestBody Map<String, Object> data) {
        Long userId = securityUtils.getCurrentUserId();
        Map<String, Object> result = companyService.sendOffer(userId, data);
        return Result.success("Offer已发放，请等待学生回复", result);
    }

    @PutMapping("/offer/{id}/withdraw")
    public Result<Void> withdrawOffer(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        companyService.withdrawOffer(userId, id);
        return Result.success();
    }

    // ==================== 面试记录 ====================

    @PostMapping("/interview-record")
    public Result<Map<String, Object>> addInterviewRecord(@RequestBody Map<String, Object> data) {
        Long userId = securityUtils.getCurrentUserId();
        Map<String, Object> result = companyService.addInterviewRecord(userId, data);
        return Result.success("面试记录已录入", result);
    }

    // ==================== 三方协议 ====================

    @GetMapping("/agreements")
    public Result<Map<String, Object>> getAgreements(@RequestParam Map<String, Object> params) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(companyService.getMyAgreements(userId, params));
    }

    @PutMapping("/agreement/{id}/sign")
    public Result<Void> signAgreement(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        companyService.signAgreement(userId, id);
        return Result.success();
    }
}
