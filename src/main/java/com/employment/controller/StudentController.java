package com.employment.controller;

import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.exception.BusinessException;
import com.employment.model.dto.StudentInfoDTO;
import com.employment.model.entity.*;
import com.employment.model.vo.StudentHomeStatsVO;
import com.employment.repository.*;
import com.employment.security.SecurityUtils;
import com.employment.service.NotificationService;
import com.employment.service.StatsCacheService;
import com.employment.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final StudentInfoRepository studentInfoRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final OfferLetterRepository offerLetterRepository;
    private final TripartiteAgreementRepository tripartiteAgreementRepository;
    private final InterviewInvitationRepository interviewInvitationRepository;
    private final CompanyInfoRepository companyInfoRepository;
    private final JobPositionRepository jobPositionRepository;
    private final InterviewRecordRepository interviewRecordRepository;
    private final EmploymentRecordRepository employmentRecordRepository;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;
    private final StatsCacheService statsCacheService;

    @GetMapping("/profile")
    public Result<StudentInfoDTO> getProfile() {
        return Result.success(studentService.getProfile());
    }

    @PutMapping("/profile")
    @OperationLog(module = "学生信息", content = "更新个人资料")
    public Result<StudentInfoDTO> updateProfile(@RequestBody StudentInfoDTO dto) {
        return Result.success(studentService.updateProfile(dto));
    }

    @GetMapping("/home-stats")
    public Result<StudentHomeStatsVO> getHomeStats() {
        return Result.success(studentService.getHomeStats());
    }

    // ==================== Offer管理 ====================

    @GetMapping("/offers")
    public Result<Map<String, Object>> getMyOffers() {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo student = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "学生信息不存在"));
        List<OfferLetter> all = offerLetterRepository.findByStudentId(student.getId());
        List<Map<String, Object>> records = all.stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("applicationId", o.getApplicationId());
            m.put("companyId", o.getCompanyId());
            m.put("jobId", o.getJobId());
            m.put("positionName", o.getPositionName());
            m.put("salary", o.getSalary());
            m.put("workCity", o.getWorkCity());
            m.put("startDate", o.getStartDate());
            m.put("probationPeriod", o.getProbationPeriod());
            m.put("probationSalary", o.getProbationSalary());
            m.put("responseDeadline", o.getResponseDeadline());
            m.put("status", o.getStatus());
            m.put("createTime", o.getCreateTime());
            companyInfoRepository.findById(o.getCompanyId()).ifPresent(c -> {
                m.put("companyName", c.getCompanyName());
                m.put("contactPerson", c.getContactPerson());
                m.put("contactPhone", c.getContactPhone());
            });
            jobPositionRepository.findById(o.getJobId()).ifPresent(j -> m.put("jobName", j.getJobName()));
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", records.size());
        return Result.success(result);
    }

    @PutMapping("/offer/{id}/accept")
    @Transactional
    @OperationLog(module = "Offer管理", content = "接受Offer")
    public Result<Void> acceptOffer(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo student = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "学生信息不存在"));
        OfferLetter offer = offerLetterRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Offer不存在"));
        if (!offer.getStudentId().equals(student.getId())) {
            throw new BusinessException(403, "无权操作此Offer");
        }
        if (!"pending".equals(offer.getStatus())) {
            throw new BusinessException(400, "该Offer已被处理");
        }
        offer.setStatus("accepted");
        offerLetterRepository.save(offer);

        // 同步更新申请状态为已录取
        jobApplicationRepository.findById(offer.getApplicationId()).ifPresent(app -> {
            app.setStatus("accepted");
            jobApplicationRepository.save(app);
        });

        // 通知企业
        notificationService.sendNotification(offer.getCompanyId(),
                "学生已接受Offer",
                "学生【" + student.getRealName() + "】已接受贵公司发放的Offer",
                "offer", "offer");

        return Result.success();
    }

    @PutMapping("/offer/{id}/decline")
    @Transactional
    @OperationLog(module = "Offer管理", content = "拒绝Offer")
    public Result<Void> declineOffer(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo student = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "学生信息不存在"));
        OfferLetter offer = offerLetterRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Offer不存在"));
        if (!offer.getStudentId().equals(student.getId())) {
            throw new BusinessException(403, "无权操作此Offer");
        }
        if (!"pending".equals(offer.getStatus())) {
            throw new BusinessException(400, "该Offer已被处理");
        }
        offer.setStatus("declined");
        offerLetterRepository.save(offer);

        // 同步更新申请状态回面试中
        jobApplicationRepository.findById(offer.getApplicationId()).ifPresent(app -> {
            app.setStatus("interview");
            app.setOfferStatus("declined");
            jobApplicationRepository.save(app);
        });

        // 通知企业
        notificationService.sendNotification(offer.getCompanyId(),
                "学生已拒绝Offer",
                "学生【" + student.getRealName() + "】已拒绝贵公司发放的Offer",
                "offer", "offer");

        return Result.success();
    }

    // ==================== 三方协议 ====================

    @GetMapping("/agreements")
    public Result<Map<String, Object>> getMyAgreements() {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo student = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "学生信息不存在"));
        List<TripartiteAgreement> all = tripartiteAgreementRepository.findByStudentId(student.getId());
        List<Map<String, Object>> records = all.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("companyId", a.getCompanyId());
            m.put("agreementNo", a.getAgreementNo());
            m.put("studentSignTime", a.getStudentSignTime());
            m.put("companySignTime", a.getCompanySignTime());
            m.put("schoolSignTime", a.getSchoolSignTime());
            m.put("status", a.getStatus());
            m.put("createTime", a.getCreateTime());
            companyInfoRepository.findById(a.getCompanyId()).ifPresent(c -> {
                m.put("companyName", c.getCompanyName());
                m.put("companyScale", c.getScale());
                m.put("companyIndustry", c.getIndustry());
                m.put("workCity", c.getCity());
                m.put("workProvince", c.getProvince());
            });
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", records.size());
        return Result.success(result);
    }

    @GetMapping("/agreement/{id}/detail")
    public Result<Map<String, Object>> getAgreementDetail(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo student = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "学生信息不存在"));
        TripartiteAgreement agreement = tripartiteAgreementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "协议不存在"));
        if (!agreement.getStudentId().equals(student.getId())) {
            throw new BusinessException(403, "无权查看此协议");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", agreement.getId());
        m.put("agreementNo", agreement.getAgreementNo());
        m.put("status", agreement.getStatus());
        companyInfoRepository.findById(agreement.getCompanyId()).ifPresent(c -> {
            m.put("companyId", c.getId());
            m.put("companyName", c.getCompanyName());
            m.put("companyScale", c.getScale());
            m.put("companyIndustry", c.getIndustry());
            m.put("workCity", c.getCity());
            m.put("workProvince", c.getProvince());
        });
        return Result.success(m);
    }

    @PostMapping("/agreement/apply")
    @Transactional
    @OperationLog(module = "三方协议", content = "申请三方协议")
    public Result<Map<String, Object>> applyAgreement(@RequestBody Map<String, Object> data) {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo student = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "学生信息不存在"));
        Long companyId = Long.parseLong(data.get("companyId").toString());
        CompanyInfo company = companyInfoRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));

        // 生成协议编号
        String agreementNo = "TPA-" + System.currentTimeMillis();

        TripartiteAgreement agreement = new TripartiteAgreement();
        agreement.setStudentId(student.getId());
        agreement.setCompanyId(companyId);
        agreement.setAgreementNo(agreementNo);
        agreement.setStudentSignTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        agreement.setStatus("student_signed");
        agreement = tripartiteAgreementRepository.save(agreement);

        // 自动创建/更新就业记录（就业去向自动填入企业信息）
        // 每个学生只保留一条最新的就业去向记录
        List<EmploymentRecord> existingRecords = employmentRecordRepository.findByStudentId(student.getId());
        EmploymentRecord empRecord;
        if (!existingRecords.isEmpty()) {
            // 已有记录，更新为三方信息（只更新最新那条）
            existingRecords.sort((a, b) -> {
                if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
                if (a.getCreateTime() == null) return 1;
                if (b.getCreateTime() == null) return -1;
                return b.getCreateTime().compareTo(a.getCreateTime());
            });
            empRecord = existingRecords.get(0);
        } else {
            empRecord = new EmploymentRecord();
            empRecord.setStudentId(student.getId());
        }
        empRecord.setEmploymentType("签订三方协议");
        empRecord.setCompanyName(company.getCompanyName());
        empRecord.setCompanyScale(company.getScale());
        empRecord.setCompanyIndustry(company.getIndustry());
        empRecord.setIsThreePartySigned("1");
        empRecord.setThreePartyNo(agreementNo);
        empRecord.setWorkCity(company.getCity());
        empRecord.setWorkProvince(company.getProvince());
        empRecord.setAuditStatus("pending");
        empRecord = employmentRecordRepository.save(empRecord);
        statsCacheService.evictByStudent(student.getClassId(), student.getDeptId());

        // 将就业记录ID关联到三方协议，便于后续精确查找
        agreement.setEmploymentRecordId(empRecord.getId());
        agreement = tripartiteAgreementRepository.save(agreement);

        // 通知企业
        notificationService.sendNotification(company.getUserId(),
                "有新三方协议待签署",
                "学生【" + student.getRealName() + "】发起了三方协议，请及时签署",
                "agreement", "agreement");

        Map<String, Object> result = new HashMap<>();
        result.put("id", agreement.getId());
        result.put("agreementNo", agreementNo);
        result.put("status", agreement.getStatus());
        result.put("employmentRecordId", empRecord.getId());
        return Result.success("协议申请已提交，就业去向已自动填入企业信息", result);
    }

    // ==================== 面试邀请（学生端） ====================

    @GetMapping("/interviews")
    public Result<Map<String, Object>> getMyInterviews() {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo student = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "学生信息不存在"));
        List<InterviewInvitation> all = interviewInvitationRepository.findByStudentId(student.getId());
        List<Map<String, Object>> records = all.stream().map(inv -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", inv.getId());
            m.put("applicationId", inv.getApplicationId());
            m.put("companyId", inv.getCompanyId());
            m.put("jobId", inv.getJobId());
            m.put("interviewTime", inv.getInterviewTime());
            m.put("interviewAddress", inv.getInterviewAddress());
            m.put("interviewType", inv.getInterviewType());
            m.put("contactPerson", inv.getContactPerson());
            m.put("contactPhone", inv.getContactPhone());
            m.put("remark", inv.getRemark());
            m.put("status", inv.getStatus());
            m.put("createTime", inv.getCreateTime());
            companyInfoRepository.findById(inv.getCompanyId()).ifPresent(c -> {
                m.put("companyName", c.getCompanyName());
                m.put("contactPhone", c.getContactPhone());
            });
            jobPositionRepository.findById(inv.getJobId()).ifPresent(j -> m.put("jobName", j.getJobName()));
            // 面试记录
            List<InterviewRecord> records2 = interviewRecordRepository.findByInvitationId(inv.getId());
            if (!records2.isEmpty()) {
                InterviewRecord ir = records2.get(0);
                m.put("interviewResult", ir.getInterviewResult());
                m.put("interviewFeedback", ir.getInterviewFeedback());
                m.put("score", ir.getScore());
            }
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", records.size());
        return Result.success(result);
    }

    @PutMapping("/interview/{id}/accept")
    @Transactional
    @OperationLog(module = "面试管理", content = "接受面试邀请")
    public Result<Void> acceptInterview(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo student = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "学生信息不存在"));
        InterviewInvitation inv = interviewInvitationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "面试邀请不存在"));
        if (!inv.getStudentId().equals(student.getId())) {
            throw new BusinessException(403, "无权操作");
        }
        inv.setStatus("confirmed");
        interviewInvitationRepository.save(inv);
        return Result.success();
    }

    @PutMapping("/interview/{id}/decline")
    @Transactional
    @OperationLog(module = "面试管理", content = "拒绝面试邀请")
    public Result<Void> declineInterview(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        StudentInfo student = studentInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "学生信息不存在"));
        InterviewInvitation inv = interviewInvitationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "面试邀请不存在"));
        if (!inv.getStudentId().equals(student.getId())) {
            throw new BusinessException(403, "无权操作");
        }
        inv.setStatus("cancelled");
        interviewInvitationRepository.save(inv);
        return Result.success();
    }
}
