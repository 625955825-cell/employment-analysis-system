package com.employment.service.impl;

import com.employment.exception.BusinessException;
import com.employment.model.dto.StudentInfoDTO;
import com.employment.model.entity.EmploymentRecord;
import com.employment.model.entity.StudentInfo;
import com.employment.model.entity.SysUser;
import com.employment.repository.EmploymentRecordRepository;
import com.employment.repository.InterviewInvitationRepository;
import com.employment.repository.JobApplicationRepository;
import com.employment.repository.JobFavoriteRepository;
import com.employment.repository.StudentInfoRepository;
import com.employment.repository.SysUserRepository;
import com.employment.security.SecurityUtils;
import com.employment.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import com.employment.model.vo.StudentHomeStatsVO;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentInfoRepository studentInfoRepository;
    private final SysUserRepository sysUserRepository;
    private final SecurityUtils securityUtils;
    private final JobApplicationRepository jobApplicationRepository;
    private final InterviewInvitationRepository interviewInvitationRepository;
    private final JobFavoriteRepository jobFavoriteRepository;
    private final EmploymentRecordRepository employmentRecordRepository;

    @Override
    public StudentInfoDTO getProfile() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "学生信息不存在，请先完善信息"));
        return toDTO(studentInfo);
    }

    @Override
    @Transactional
    public StudentInfoDTO updateProfile(StudentInfoDTO dto) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        StudentInfo existing = studentInfoRepository.findByUserId(userId).orElse(null);
        StudentInfo studentInfo;

        if (existing == null) {
            studentInfo = new StudentInfo();
            studentInfo.setUserId(userId);
            studentInfo.setStatus("studying");
            String studentNo = userId.toString();
            String realName = userId.toString();
            SysUser user = sysUserRepository.findById(userId).orElse(null);
            if (user != null) {
                studentNo = user.getUsername();
                realName = (user.getRealName() != null && !user.getRealName().trim().isEmpty())
                        ? user.getRealName()
                        : (dto.getRealName() != null ? dto.getRealName() : user.getUsername());
            }
            studentInfo.setStudentNo(studentNo);
            studentInfo.setRealName(realName);
        } else {
            studentInfo = existing;
            if (dto.getRealName() != null && !dto.getRealName().trim().isEmpty()) {
                studentInfo.setRealName(dto.getRealName());
            }
        }

        Optional.ofNullable(dto.getGender()).ifPresent(studentInfo::setGender);
        Optional.ofNullable(dto.getBirthDate()).ifPresent(studentInfo::setBirthDate);
        Optional.ofNullable(dto.getNation()).ifPresent(studentInfo::setNation);
        Optional.ofNullable(dto.getPoliticsStatus()).ifPresent(studentInfo::setPoliticsStatus);
        Optional.ofNullable(dto.getPhone()).ifPresent(studentInfo::setPhone);
        Optional.ofNullable(dto.getEmail()).ifPresent(studentInfo::setEmail);
        Optional.ofNullable(dto.getProvince()).ifPresent(studentInfo::setProvince);
        Optional.ofNullable(dto.getCity()).ifPresent(studentInfo::setCity);
        Optional.ofNullable(dto.getAddress()).ifPresent(studentInfo::setAddress);
        Optional.ofNullable(dto.getStudyType()).ifPresent(studentInfo::setStudyType);
        Optional.ofNullable(dto.getDormitory()).ifPresent(studentInfo::setDormitory);
        Optional.ofNullable(dto.getEmergencyContact()).ifPresent(studentInfo::setEmergencyContact);
        Optional.ofNullable(dto.getEmergencyPhone()).ifPresent(studentInfo::setEmergencyPhone);
        Optional.ofNullable(dto.getAvatar()).ifPresent(studentInfo::setAvatar);

        StudentInfo saved = studentInfoRepository.save(studentInfo);

        if (dto.getRealName() != null && !dto.getRealName().trim().isEmpty()) {
            sysUserRepository.findById(userId).ifPresent(user -> {
                user.setRealName(dto.getRealName());
                sysUserRepository.save(user);
            });
        }
        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            sysUserRepository.findById(userId).ifPresent(user -> {
                user.setPhone(dto.getPhone());
                sysUserRepository.save(user);
            });
        }
        if (dto.getAvatar() != null && !dto.getAvatar().trim().isEmpty()) {
            sysUserRepository.findById(userId).ifPresent(user -> {
                user.setAvatar(dto.getAvatar());
                sysUserRepository.save(user);
            });
        }

        log.info("学生 {} 更新个人信息成功", userId);
        return toDTO(saved);
    }

    private StudentInfoDTO toDTO(StudentInfo entity) {
        StudentInfoDTO dto = new StudentInfoDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setStudentNo(entity.getStudentNo());
        dto.setRealName(entity.getRealName());
        dto.setGender(entity.getGender());
        dto.setBirthDate(entity.getBirthDate());
        dto.setIdCard(entity.getIdCard());
        dto.setNation(entity.getNation());
        dto.setPoliticsStatus(entity.getPoliticsStatus());
        dto.setPhone(entity.getPhone());
        dto.setEmail(entity.getEmail());
        dto.setProvince(entity.getProvince());
        dto.setCity(entity.getCity());
        dto.setAddress(entity.getAddress());
        dto.setDeptId(entity.getDeptId());
        dto.setDeptName(entity.getDeptName());
        dto.setMajorId(entity.getMajorId());
        dto.setMajorName(entity.getMajorName());
        dto.setClassName(entity.getClassName());
        dto.setGraduationYear(entity.getGraduationYear());
        dto.setStudyType(entity.getStudyType());
        dto.setDormitory(entity.getDormitory());
        dto.setEmergencyContact(entity.getEmergencyContact());
        dto.setEmergencyPhone(entity.getEmergencyPhone());
        dto.setAvatar(entity.getAvatar());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    @Override
    public StudentHomeStatsVO getHomeStats() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return new StudentHomeStatsVO(0L, 0L, 0L, "", "请完善信息");
        }
        StudentInfo studentInfo = studentInfoRepository.findByUserId(userId).orElse(null);
        if (studentInfo == null) {
            return new StudentHomeStatsVO(0L, 0L, 0L, "", "请完善信息");
        }

        Long studentId = studentInfo.getId();

        long applicationCount = jobApplicationRepository.findByStudentId(studentId).size();
        long interviewCount = interviewInvitationRepository.findByStudentId(studentId).stream()
                .filter(inv -> "pending".equals(inv.getStatus()) || "accepted".equals(inv.getStatus()))
                .count();
        long favoriteCount = jobFavoriteRepository.findByStudentId(studentId).size();

        List<EmploymentRecord> records = employmentRecordRepository.findByStudentId(studentId);
        String employmentStatus;
        String employmentStatusText;
        if (records.isEmpty()) {
            employmentStatus = "unregistered";
            employmentStatusText = "待登记";
        } else {
            EmploymentRecord latest = records.get(records.size() - 1);
            String status = latest.getAuditStatus();
            employmentStatus = status;
            if ("approved".equals(status)) {
                employmentStatusText = "已登记";
            } else if ("pending".equals(status)) {
                employmentStatusText = "审核中";
            } else if ("rejected".equals(status)) {
                employmentStatusText = "已驳回";
            } else {
                employmentStatusText = "未知";
            }
        }

        return new StudentHomeStatsVO(applicationCount, interviewCount, favoriteCount, employmentStatus, employmentStatusText);
    }
}
