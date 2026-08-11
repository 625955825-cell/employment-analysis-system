package com.employment.service.impl;

import com.employment.common.PageResult;
import com.employment.exception.BusinessException;
import com.employment.model.dto.JobSearchDTO;
import com.employment.model.entity.InterviewInvitation;
import com.employment.model.entity.JobPosition;
import com.employment.model.entity.OfferLetter;
import com.employment.model.entity.StudentInfo;
import com.employment.repository.InterviewInvitationRepository;
import com.employment.repository.JobApplicationRepository;
import com.employment.repository.JobPositionRepository;
import com.employment.repository.OfferLetterRepository;
import com.employment.repository.StudentInfoRepository;
import com.employment.security.SecurityUtils;
import com.employment.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class InterviewServiceImpl implements InterviewService {

    private final InterviewInvitationRepository interviewInvitationRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final JobPositionRepository jobPositionRepository;
    private final OfferLetterRepository offerLetterRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final SecurityUtils securityUtils;

    @Override
    public PageResult<?> getMyInterviews(JobSearchDTO dto) {
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

        List<InterviewInvitation> all = interviewInvitationRepository.findByStudentId(studentInfo.getId());
        int total = all.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);

        List<Map<String, Object>> records;
        if (start >= total) {
            records = Collections.emptyList();
        } else {
            records = all.subList(start, end).stream().map(inv -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", inv.getId());
                map.put("applicationId", inv.getApplicationId());
                map.put("companyId", inv.getCompanyId());
                map.put("jobId", inv.getJobId());
                map.put("interviewTime", inv.getInterviewTime());
                map.put("interviewAddress", inv.getInterviewAddress());
                map.put("interviewType", inv.getInterviewType());
                map.put("contactPerson", inv.getContactPerson());
                map.put("contactPhone", inv.getContactPhone());
                map.put("remark", inv.getRemark());
                map.put("status", inv.getStatus());
                map.put("createTime", inv.getCreateTime());
                jobPositionRepository.findById(inv.getJobId()).ifPresent(job -> {
                    map.put("jobName", job.getJobName());
                    map.put("companyName", job.getCompanyName());
                    map.put("workCity", job.getWorkCity());
                });
                return map;
            }).collect(Collectors.toList());
        }

        return new PageResult<>((long) total, records, (long) page, (long) size);
    }

    @Override
    @Transactional
    public void acceptInterview(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");

        InterviewInvitation inv = interviewInvitationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "面试邀请不存在"));

        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "请先完善个人信息"));

        if (!inv.getStudentId().equals(studentInfo.getId())) {
            throw new BusinessException(403, "无权操作");
        }

        inv.setStatus("accepted");
        interviewInvitationRepository.save(inv);
        log.info("学生 {} 接受了面试邀请 {}", studentInfo.getId(), id);
    }

    @Override
    @Transactional
    public void rejectInterview(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");

        InterviewInvitation inv = interviewInvitationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "面试邀请不存在"));

        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "请先完善个人信息"));

        if (!inv.getStudentId().equals(studentInfo.getId())) {
            throw new BusinessException(403, "无权操作");
        }

        inv.setStatus("rejected");
        interviewInvitationRepository.save(inv);
        log.info("学生 {} 拒绝了面试邀请 {}", studentInfo.getId(), id);
    }

    @Override
    public PageResult<?> getMyOffers(JobSearchDTO dto) {
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

        List<OfferLetter> all = offerLetterRepository.findByStudentId(studentInfo.getId());
        int total = all.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);

        List<Map<String, Object>> records;
        if (start >= total) {
            records = Collections.emptyList();
        } else {
            records = all.subList(start, end).stream().map(offer -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", offer.getId());
                map.put("applicationId", offer.getApplicationId());
                map.put("companyId", offer.getCompanyId());
                map.put("jobId", offer.getJobId());
                map.put("positionName", offer.getPositionName());
                map.put("salary", offer.getSalary());
                map.put("workCity", offer.getWorkCity());
                map.put("startDate", offer.getStartDate());
                map.put("probationPeriod", offer.getProbationPeriod());
                map.put("probationSalary", offer.getProbationSalary());
                map.put("status", offer.getStatus());
                map.put("responseDeadline", offer.getResponseDeadline());
                map.put("createTime", offer.getCreateTime());
                jobPositionRepository.findById(offer.getJobId()).ifPresent(job -> {
                    map.put("jobName", job.getJobName());
                    map.put("companyName", job.getCompanyName());
                });
                return map;
            }).collect(Collectors.toList());
        }

        return new PageResult<>((long) total, records, (long) page, (long) size);
    }

    @Override
    @Transactional
    public void acceptOffer(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");

        OfferLetter offer = offerLetterRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Offer不存在"));

        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "请先完善个人信息"));

        if (!offer.getStudentId().equals(studentInfo.getId())) {
            throw new BusinessException(403, "无权操作");
        }

        offer.setStatus("accepted");
        offerLetterRepository.save(offer);

        // 同步更新申请状态为已录取
        jobApplicationRepository.findById(offer.getApplicationId()).ifPresent(app -> {
            app.setStatus("accepted");
            jobApplicationRepository.save(app);
        });

        log.info("学生 {} 接受了Offer {}", studentInfo.getId(), id);
    }

    @Override
    @Transactional
    public void rejectOffer(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");

        OfferLetter offer = offerLetterRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Offer不存在"));

        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "请先完善个人信息"));

        if (!offer.getStudentId().equals(studentInfo.getId())) {
            throw new BusinessException(403, "无权操作");
        }

        offer.setStatus("rejected");
        offerLetterRepository.save(offer);
        log.info("学生 {} 拒绝了Offer {}", studentInfo.getId(), id);
    }
}
