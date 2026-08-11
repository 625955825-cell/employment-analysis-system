package com.employment.service.impl;

import com.employment.common.PageResult;
import com.employment.exception.BusinessException;
import com.employment.model.dto.JobSearchDTO;
import com.employment.model.entity.CompanyInfo;
import com.employment.model.entity.JobApplication;
import com.employment.model.entity.JobFavorite;
import com.employment.model.entity.JobPosition;
import com.employment.model.entity.StudentInfo;
import com.employment.model.vo.JobDetailVO;
import com.employment.repository.CompanyInfoRepository;
import com.employment.repository.JobApplicationRepository;
import com.employment.repository.JobFavoriteRepository;
import com.employment.repository.JobPositionRepository;
import com.employment.repository.StudentInfoRepository;
import com.employment.security.SecurityUtils;
import com.employment.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobPositionRepository jobPositionRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final JobFavoriteRepository jobFavoriteRepository;
    private final CompanyInfoRepository companyInfoRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final SecurityUtils securityUtils;

    @Override
    public PageResult<?> searchJobs(JobSearchDTO dto) {
        int page = Math.max(dto.getPage(), 1);
        int size = Math.min(Math.max(dto.getSize(), 1), 50);
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishTime"));

        Page<JobPosition> result = jobPositionRepository.searchJobs(
                dto.getKeyword() == null || dto.getKeyword().trim().isEmpty() ? null : dto.getKeyword(),
                dto.getCity() == null || dto.getCity().trim().isEmpty() ? null : dto.getCity(),
                pageable
        );

        Long userId = securityUtils.getCurrentUserId();
        Long studentId = null;
        if (userId != null) {
            StudentInfo info = studentInfoRepository.findByUserId(userId).orElse(null);
            if (info != null) studentId = info.getId();
        }
        final Long sid = studentId;

        List<Map<String, Object>> records = result.getContent().stream().map(job -> {
            Map<String, Object> map = toSimpleMap(job);
            if (sid != null) {
                map.put("hasApplied", jobApplicationRepository.existsByJobIdAndStudentId(job.getId(), sid));
                map.put("hasFavorite", jobFavoriteRepository.existsByJobIdAndStudentId(job.getId(), sid));
            } else {
                map.put("hasApplied", false);
                map.put("hasFavorite", false);
            }
            return map;
        }).collect(Collectors.toList());

        return new PageResult<>(result.getTotalElements(), records, (long) page, (long) size);
    }

    @Override
    @Transactional
    public JobDetailVO getJobDetail(Long id) {
        JobPosition job = jobPositionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "职位不存在或已下架"));

        jobPositionRepository.incrementViewCount(id);

        CompanyInfo company = companyInfoRepository.findById(job.getCompanyId()).orElse(null);

        Long userId = securityUtils.getCurrentUserId();
        boolean hasApplied = false;
        boolean hasFavorite = false;
        if (userId != null) {
            StudentInfo info = studentInfoRepository.findByUserId(userId).orElse(null);
            if (info != null) {
                Long studentId = info.getId();
                hasApplied = jobApplicationRepository.existsByJobIdAndStudentId(id, studentId);
                hasFavorite = jobFavoriteRepository.existsByJobIdAndStudentId(id, studentId);
            }
        }

        JobDetailVO vo = new JobDetailVO();
        vo.setId(job.getId());
        vo.setCompanyId(job.getCompanyId());
        vo.setCompanyName(job.getCompanyName());
        vo.setJobName(job.getJobName());
        vo.setJobCategory(job.getJobCategory());
        vo.setJobType(job.getJobType());
        vo.setWorkCity(job.getWorkCity());
        vo.setWorkAddress(job.getWorkAddress());
        vo.setSalaryMin(job.getSalaryMin() != null ? String.valueOf(job.getSalaryMin()) : null);
        vo.setSalaryMax(job.getSalaryMax() != null ? String.valueOf(job.getSalaryMax()) : null);
        vo.setSalaryMonths(job.getSalaryMonths());
        vo.setRecruitNumber(job.getRecruitNumber());
        vo.setRequirement(job.getRequirement());
        vo.setResponsibility(job.getResponsibility());
        vo.setBenefits(job.getBenefits());
        vo.setEducationRequired(job.getEducationRequired());
        vo.setExperienceRequired(job.getExperienceRequired());
        vo.setSkillRequired(job.getSkillRequired());
        vo.setIsRemote(job.getIsRemote());
        vo.setIsHighSalary(job.getIsHighSalary());
        vo.setViewCount(job.getViewCount());
        vo.setApplyCount(job.getApplyCount());
        vo.setStatus(job.getStatus());
        vo.setPublishTime(job.getPublishTime());
        vo.setDeadline(job.getDeadline());

        if (company != null) {
            vo.setCompanyLogo(company.getLogo());
            vo.setCompanyScale(company.getScale());
            vo.setCompanyIndustry(company.getIndustry());
            vo.setCompanyNature(company.getNature());
            vo.setCompanyCity(company.getCity());
            vo.setCompanyAddress(company.getAddress());
            vo.setCompanyIntroduction(company.getIntroduction());
        }

        vo.setHasApplied(hasApplied);
        vo.setHasFavorite(hasFavorite);
        return vo;
    }

    @Override
    @Transactional
    public void applyJob(Long jobId, Long resumeId, String applyLetter) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");

        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "请先完善个人信息"));

        JobPosition job = jobPositionRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(404, "职位不存在"));

        if (!"published".equals(job.getStatus()) || "1".equals(job.getIsDeleted())) {
            throw new BusinessException(400, "该职位已下架或不存在");
        }

        if (jobApplicationRepository.existsByJobIdAndStudentId(jobId, studentInfo.getId())) {
            throw new BusinessException(400, "您已投递过该职位，请勿重复投递");
        }

        JobApplication application = new JobApplication();
        application.setJobId(jobId);
        application.setStudentId(studentInfo.getId());
        application.setCompanyId(job.getCompanyId());
        application.setResumeId(resumeId);
        application.setApplyLetter(applyLetter);
        application.setStatus("pending");
        application.setReadStatus("0");
        jobApplicationRepository.save(application);

        job.setApplyCount(job.getApplyCount() != null ? job.getApplyCount() + 1 : 1);
        jobPositionRepository.save(job);

        log.info("学生 {} 投递职位 {} 成功", studentInfo.getId(), jobId);
    }

    @Override
    @Transactional
    public void favoriteJob(Long jobId) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");

        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "请先完善个人信息"));

        if (jobFavoriteRepository.existsByJobIdAndStudentId(jobId, studentInfo.getId())) {
            return;
        }

        JobFavorite favorite = new JobFavorite();
        favorite.setJobId(jobId);
        favorite.setStudentId(studentInfo.getId());
        jobFavoriteRepository.save(favorite);
        log.info("学生 {} 收藏职位 {}", studentInfo.getId(), jobId);
    }

    @Override
    @Transactional
    public void unfavoriteJob(Long jobId) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");

        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "请先完善个人信息"));

        jobFavoriteRepository.deleteByJobIdAndStudentId(jobId, studentInfo.getId());
        log.info("学生 {} 取消收藏职位 {}", studentInfo.getId(), jobId);
    }

    @Override
    public PageResult<?> getMyApplications(JobSearchDTO dto) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");

        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId).orElse(null);
        if (studentInfo == null) {
            int page = Math.max(dto.getPage(), 1);
            int size = Math.min(Math.max(dto.getSize(), 1), 50);
            return new PageResult<>(0L, Collections.emptyList(), (long) page, (long) size);
        }

        int page = Math.max(dto.getPage(), 1);
        int size = Math.min(Math.max(dto.getSize(), 1), 50);
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));

        List<JobApplication> all = jobApplicationRepository.findByStudentId(studentInfo.getId());
        int total = all.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<Map<String, Object>> records;
        if (start >= total) {
            records = Collections.emptyList();
        } else {
            records = all.subList(start, end).stream().map(app -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", app.getId());
                map.put("jobId", app.getJobId());
                map.put("studentId", app.getStudentId());
                map.put("companyId", app.getCompanyId());
                map.put("resumeId", app.getResumeId());
                map.put("status", app.getStatus());
                map.put("readStatus", app.getReadStatus());
                map.put("applyLetter", app.getApplyLetter());
                map.put("companyRemark", app.getCompanyRemark());
                map.put("interviewStatus", app.getInterviewStatus());
                map.put("offerStatus", app.getOfferStatus());
                map.put("createTime", app.getCreateTime());
                jobPositionRepository.findById(app.getJobId()).ifPresent(job -> {
                    map.put("jobName", job.getJobName());
                    map.put("workCity", job.getWorkCity());
                    map.put("salaryMin", job.getSalaryMin());
                    map.put("salaryMax", job.getSalaryMax());
                });
                return map;
            }).collect(Collectors.toList());
        }

        return new PageResult<>((long) total, records, (long) page, (long) size);
    }

    @Override
    public PageResult<?> getMyFavorites(JobSearchDTO dto) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");

        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId).orElse(null);
        if (studentInfo == null) {
            int page = Math.max(dto.getPage(), 1);
            int size = Math.min(Math.max(dto.getSize(), 1), 50);
            return new PageResult<>(0L, Collections.emptyList(), (long) page, (long) size);
        }

        int page = Math.max(dto.getPage(), 1);
        int size = Math.min(Math.max(dto.getSize(), 1), 50);
        List<JobFavorite> all = jobFavoriteRepository.findByStudentId(studentInfo.getId());
        int total = all.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);

        List<Map<String, Object>> records;
        if (start >= total) {
            records = Collections.emptyList();
        } else {
            records = all.subList(start, end).stream().map(fav -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", fav.getId());
                map.put("jobId", fav.getJobId());
                map.put("studentId", fav.getStudentId());
                map.put("notes", fav.getNotes());
                map.put("createTime", fav.getCreateTime());
                jobPositionRepository.findById(fav.getJobId()).ifPresent(job -> {
                    map.put("jobName", job.getJobName());
                    map.put("companyName", job.getCompanyName());
                    map.put("workCity", job.getWorkCity());
                    map.put("salaryMin", job.getSalaryMin());
                    map.put("salaryMax", job.getSalaryMax());
                    map.put("deadline", job.getDeadline());
                    map.put("status", job.getStatus());
                });
                return map;
            }).collect(Collectors.toList());
        }

        return new PageResult<>((long) total, records, (long) page, (long) size);
    }

    @Override
    @Transactional
    public void cancelApplication(Long applicationId) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");

        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "学生信息不存在，请先完善信息"));

        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(404, "投递记录不存在"));

        if (!application.getStudentId().equals(studentInfo.getId())) {
            throw new BusinessException(403, "无权操作");
        }

        if (!"pending".equals(application.getStatus())) {
            throw new BusinessException(400, "只有待处理的投递可以撤回");
        }

        jobApplicationRepository.delete(application);
        log.info("学生 {} 撤回投递 {}", studentInfo.getId(), applicationId);
    }

    private Map<String, Object> toSimpleMap(JobPosition job) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", job.getId());
        map.put("companyId", job.getCompanyId());
        map.put("companyName", job.getCompanyName());
        map.put("jobName", job.getJobName());
        map.put("jobCategory", job.getJobCategory());
        map.put("jobType", job.getJobType());
        map.put("workCity", job.getWorkCity());
        map.put("salaryMin", job.getSalaryMin());
        map.put("salaryMax", job.getSalaryMax());
        map.put("educationRequired", job.getEducationRequired());
        map.put("experienceRequired", job.getExperienceRequired());
        map.put("recruitNumber", job.getRecruitNumber());
        map.put("viewCount", job.getViewCount());
        map.put("applyCount", job.getApplyCount());
        map.put("status", job.getStatus());
        map.put("publishTime", job.getPublishTime());
        map.put("deadline", job.getDeadline());
        map.put("isHighSalary", job.getIsHighSalary());
        map.put("isRemote", job.getIsRemote());
        return map;
    }
}
