package com.employment.service;

import com.employment.common.PageResult;
import com.employment.model.dto.JobSearchDTO;
import com.employment.model.entity.CompanyInfo;
import com.employment.model.entity.JobPosition;

import java.util.List;
import java.util.Map;

public interface CompanyService {
    CompanyInfo updateProfile(Long userId, CompanyInfo companyInfo);
    Map<String, Object> getHomeStats(Long userId);
    PageResult<?> getReceivedResumes(Long userId, JobSearchDTO dto);
    void updateApplicationStatus(Long applicationId, String status, String companyRemark);
    PageResult<?> getCompanyInterviews(Long userId, JobSearchDTO dto);
    void createInterview(Long userId, Map<String, Object> data);
    void cancelInterview(Long interviewId, Long userId);
    Map<String, Object> getStatistics(Long userId, String startDate, String endDate);

    // 重新申请入驻（驳回后）
    CompanyInfo reApply(Long userId, Map<String, Object> data);

    // Offer管理
    Map<String, Object> getMyOffers(Long userId, Map<String, Object> params);
    Map<String, Object> sendOffer(Long userId, Map<String, Object> data);
    void withdrawOffer(Long userId, Long offerId);

    // 面试记录（企业填写反馈）
    Map<String, Object> addInterviewRecord(Long userId, Map<String, Object> data);

    // 三方协议
    Map<String, Object> getMyAgreements(Long userId, Map<String, Object> params);
    void signAgreement(Long userId, Long agreementId);

    // 职位管理
    PageResult<?> getMyJobs(Long userId, Map<String, Object> params);
    Map<String, Object> getJobDetail(Long userId, Long jobId);
    JobPosition createJob(Long userId, Map<String, Object> data);
    JobPosition updateJob(Long userId, Long jobId, Map<String, Object> data);
    void publishJob(Long userId, Long jobId);
    void pauseJob(Long userId, Long jobId);
    void deleteJob(Long userId, Long jobId);

    // 批量删除投递记录
    void deleteApplications(Long userId, List<Long> applicationIds);
}
