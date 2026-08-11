package com.employment.service.impl;

import com.employment.exception.BusinessException;
import com.employment.model.dto.ResumeDTO;
import com.employment.model.entity.StudentInfo;
import com.employment.model.entity.StudentResume;
import com.employment.repository.StudentInfoRepository;
import com.employment.repository.StudentResumeRepository;
import com.employment.security.SecurityUtils;
import com.employment.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final StudentResumeRepository resumeRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final SecurityUtils securityUtils;

    @Override
    public List<ResumeDTO> getMyResumes() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        StudentInfo info = studentInfoRepository.findByUserId(userId).orElse(null);
        if (info == null) {
            return Collections.emptyList();
        }
        return resumeRepository.findByStudentId(info.getId()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ResumeDTO getResumeById(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        Long studentId = getStudentId(userId);
        StudentResume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "简历不存在"));
        if (!resume.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权访问此简历");
        }
        return toDTO(resume);
    }

    @Override
    @Transactional
    public ResumeDTO createResume(ResumeDTO dto) {
        Long userId = securityUtils.getCurrentUserId();
        Long studentId = getStudentId(userId);

        StudentResume resume = new StudentResume();
        resume.setStudentId(studentId);
        resume.setResumeName(dto.getResumeName());
        resume.setPersonalSummary(dto.getPersonalSummary());
        resume.setEducationExperience(dto.getEducationExperience());
        resume.setProjectExperience(dto.getProjectExperience());
        resume.setWorkExperience(dto.getWorkExperience());
        resume.setSkillCertificates(dto.getSkillCertificates());
        resume.setAwardsHonors(dto.getAwardsHonors());
        resume.setSelfEvaluation(dto.getSelfEvaluation());
        resume.setExpectedSalaryMin(dto.getExpectedSalaryMin());
        resume.setExpectedSalaryMax(dto.getExpectedSalaryMax());
        resume.setExpectedCity(dto.getExpectedCity());
        resume.setExpectedPosition(dto.getExpectedPosition());
        resume.setExpectedIndustry(dto.getExpectedIndustry());

        // 如果是第一份简历，自动设为默认
        int count = resumeRepository.countByStudentId(studentId);
        if (count == 0) {
            resume.setIsDefault("1");
        } else {
            resume.setIsDefault("0");
        }

        StudentResume saved = resumeRepository.save(resume);
        log.info("用户 {} 创建简历成功: id={}", userId, saved.getId());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public ResumeDTO updateResume(Long id, ResumeDTO dto) {
        Long userId = securityUtils.getCurrentUserId();
        Long studentId = getStudentId(userId);

        StudentResume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "简历不存在"));
        if (!resume.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权修改此简历");
        }

        if (dto.getResumeName() != null) resume.setResumeName(dto.getResumeName());
        if (dto.getPersonalSummary() != null) resume.setPersonalSummary(dto.getPersonalSummary());
        if (dto.getEducationExperience() != null) resume.setEducationExperience(dto.getEducationExperience());
        if (dto.getProjectExperience() != null) resume.setProjectExperience(dto.getProjectExperience());
        if (dto.getWorkExperience() != null) resume.setWorkExperience(dto.getWorkExperience());
        if (dto.getSkillCertificates() != null) resume.setSkillCertificates(dto.getSkillCertificates());
        if (dto.getAwardsHonors() != null) resume.setAwardsHonors(dto.getAwardsHonors());
        if (dto.getSelfEvaluation() != null) resume.setSelfEvaluation(dto.getSelfEvaluation());
        if (dto.getExpectedSalaryMin() != null) resume.setExpectedSalaryMin(dto.getExpectedSalaryMin());
        if (dto.getExpectedSalaryMax() != null) resume.setExpectedSalaryMax(dto.getExpectedSalaryMax());
        if (dto.getExpectedCity() != null) resume.setExpectedCity(dto.getExpectedCity());
        if (dto.getExpectedPosition() != null) resume.setExpectedPosition(dto.getExpectedPosition());
        if (dto.getExpectedIndustry() != null) resume.setExpectedIndustry(dto.getExpectedIndustry());

        StudentResume saved = resumeRepository.save(resume);
        log.info("用户 {} 更新简历 {} 成功", userId, id);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public void deleteResume(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        Long studentId = getStudentId(userId);

        StudentResume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "简历不存在"));
        if (!resume.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权删除此简历");
        }

        resumeRepository.delete(resume);
        log.info("用户 {} 删除简历 {}", userId, id);

        // 如果删除的是默认简历，将剩下的第一份设为默认
        if ("1".equals(resume.getIsDefault())) {
            List<StudentResume> remaining = resumeRepository.findByStudentId(studentId);
            if (!remaining.isEmpty()) {
                remaining.get(0).setIsDefault("1");
                resumeRepository.save(remaining.get(0));
            }
        }
    }

    @Override
    @Transactional
    public void setDefaultResume(Long id) {
        Long userId = securityUtils.getCurrentUserId();
        Long studentId = getStudentId(userId);

        StudentResume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "简历不存在"));
        if (!resume.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权操作此简历");
        }

        // 取消所有默认
        List<StudentResume> all = resumeRepository.findByStudentId(studentId);
        for (StudentResume r : all) {
            r.setIsDefault("0");
        }
        resumeRepository.saveAll(all);

        // 设置新的默认
        resume.setIsDefault("1");
        resumeRepository.save(resume);
        log.info("用户 {} 设置简历 {} 为默认", userId, id);
    }

    private Long getStudentId(Long userId) {
        return studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "学生信息不存在，请先完善个人信息"))
                .getId();
    }

    private ResumeDTO toDTO(StudentResume entity) {
        ResumeDTO dto = new ResumeDTO();
        dto.setId(entity.getId());
        dto.setStudentId(entity.getStudentId());
        dto.setResumeName(entity.getResumeName());
        dto.setIsDefault(entity.getIsDefault());
        dto.setPersonalSummary(entity.getPersonalSummary());
        dto.setEducationExperience(entity.getEducationExperience());
        dto.setProjectExperience(entity.getProjectExperience());
        dto.setWorkExperience(entity.getWorkExperience());
        dto.setSkillCertificates(entity.getSkillCertificates());
        dto.setAwardsHonors(entity.getAwardsHonors());
        dto.setSelfEvaluation(entity.getSelfEvaluation());
        dto.setExpectedSalaryMin(entity.getExpectedSalaryMin());
        dto.setExpectedSalaryMax(entity.getExpectedSalaryMax());
        dto.setExpectedCity(entity.getExpectedCity());
        dto.setExpectedPosition(entity.getExpectedPosition());
        dto.setExpectedIndustry(entity.getExpectedIndustry());
        dto.setFilePath(entity.getFilePath());
        dto.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime().toString() : null);
        dto.setUpdateTime(entity.getUpdateTime() != null ? entity.getUpdateTime().toString() : null);
        return dto;
    }
}
