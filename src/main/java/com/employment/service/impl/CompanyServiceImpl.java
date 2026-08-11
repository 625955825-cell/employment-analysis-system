package com.employment.service.impl;

import com.employment.common.PageResult;
import com.employment.exception.BusinessException;
import com.employment.model.dto.JobSearchDTO;
import com.employment.model.entity.*;
import com.employment.repository.*;
import com.employment.service.CompanyService;
import com.employment.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyInfoRepository companyInfoRepository;
    private final CompanyAuthRepository companyAuthRepository;
    private final JobPositionRepository jobPositionRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final StudentResumeRepository studentResumeRepository;
    private final InterviewInvitationRepository interviewInvitationRepository;
    private final OfferLetterRepository offerLetterRepository;
    private final InterviewRecordRepository interviewRecordRepository;
    private final TripartiteAgreementRepository tripartiteAgreementRepository;
    private final NotificationService notificationService;
    private final SysUserRepository sysUserRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final SysRoleRepository sysRoleRepository;

    @Override
    public CompanyInfo updateProfile(Long userId, CompanyInfo companyInfo) {
        CompanyInfo existing = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));

        Optional.ofNullable(companyInfo.getCompanyName()).ifPresent(existing::setCompanyName);
        Optional.ofNullable(companyInfo.getContactPerson()).ifPresent(existing::setContactPerson);
        Optional.ofNullable(companyInfo.getContactPhone()).ifPresent(existing::setContactPhone);
        Optional.ofNullable(companyInfo.getContactEmail()).ifPresent(existing::setContactEmail);
        Optional.ofNullable(companyInfo.getProvince()).ifPresent(existing::setProvince);
        Optional.ofNullable(companyInfo.getCity()).ifPresent(existing::setCity);
        Optional.ofNullable(companyInfo.getDistrict()).ifPresent(existing::setDistrict);
        Optional.ofNullable(companyInfo.getAddress()).ifPresent(existing::setAddress);
        Optional.ofNullable(companyInfo.getIndustry()).ifPresent(existing::setIndustry);
        Optional.ofNullable(companyInfo.getScale()).ifPresent(existing::setScale);
        Optional.ofNullable(companyInfo.getNature()).ifPresent(existing::setNature);
        Optional.ofNullable(companyInfo.getIntroduction()).ifPresent(existing::setIntroduction);

        return companyInfoRepository.save(existing);
    }

    @Override
    public Map<String, Object> getHomeStats(Long userId) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId).orElse(null);
        Map<String, Object> stats = new HashMap<>();
        if (company == null) {
            stats.put("jobCount", 0);
            stats.put("resumeCount", 0);
            stats.put("interviewCount", 0);
            stats.put("offerCount", 0);
            stats.put("authStatus", "none");
            return stats;
        }
        Long companyId = company.getId();

        long jobCount = jobPositionRepository.findByCompanyId(companyId).stream()
                .filter(j -> "published".equals(j.getStatus()) && !"1".equals(j.getIsDeleted()))
                .count();

        List<JobApplication> allApps = jobApplicationRepository.findByCompanyId(companyId);
        long resumeCount = allApps.size();
        long interviewCount = allApps.stream().filter(a -> "interview".equals(a.getStatus())).count();
        long offerCount = allApps.stream().filter(a -> "offer".equals(a.getStatus()) || "accepted".equals(a.getStatus())).count();

        stats.put("jobCount", jobCount);
        stats.put("resumeCount", resumeCount);
        stats.put("interviewCount", interviewCount);
        stats.put("offerCount", offerCount);
        stats.put("authStatus", company.getAuthStatus());
        return stats;
    }

    @Override
    public PageResult<?> getReceivedResumes(Long userId, JobSearchDTO dto) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));

        int p = dto.getPage() == null ? 1 : dto.getPage();
        int s = dto.getSize() == null ? 10 : dto.getSize();
        int page = Math.max(p, 1);
        int size = Math.min(Math.max(s, 1), 50);

        List<JobApplication> all = jobApplicationRepository.findByCompanyId(company.getId());
        int total = all.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);

        List<Map<String, Object>> records = new ArrayList<>();
        if (start < total) {
            for (JobApplication app : all.subList(start, end)) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", app.getId());
                map.put("jobId", app.getJobId());
                map.put("studentId", app.getStudentId());
                map.put("resumeId", app.getResumeId());
                map.put("status", app.getStatus());
                map.put("readStatus", app.getReadStatus());
                map.put("companyRemark", app.getCompanyRemark());
                map.put("applyLetter", app.getApplyLetter());
                map.put("createTime", app.getCreateTime());

                JobPosition job = jobPositionRepository.findById(app.getJobId()).orElse(null);
                if (job != null) {
                    map.put("jobName", job.getJobName());
                    map.put("workCity", job.getWorkCity());
                }

                StudentInfo student = studentInfoRepository.findById(app.getStudentId()).orElse(null);
                if (student != null) {
                    map.put("studentNo", student.getStudentNo());
                    map.put("realName", student.getRealName());
                    map.put("gender", student.getGender());
                    map.put("phone", student.getPhone());
                    map.put("email", student.getEmail());
                    map.put("deptName", student.getDeptName());
                    map.put("majorName", student.getMajorName());
                    map.put("className", student.getClassName());
                }

                if (app.getResumeId() != null) {
                    studentResumeRepository.findById(app.getResumeId()).ifPresent(resume -> {
                        map.put("resumeName", resume.getResumeName());
                        map.put("personalSummary", resume.getPersonalSummary());
                    });
                }

                records.add(map);
            }
        }

        return new PageResult<Map<String, Object>>((long) total, records, (long) page, (long) size);
    }

    @Override
    @Transactional
    public void updateApplicationStatus(Long applicationId, String status, String companyRemark) {
        JobApplication app = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(404, "投递记录不存在"));
        Long companyId = app.getCompanyId();
        CompanyInfo company = companyInfoRepository.findById(companyId).orElse(null);
        if (company != null && !"approved".equals(company.getAuthStatus())) {
            throw new BusinessException(403, "您的企业尚未通过认证审核，暂时无法使用该功能");
        }
        app.setStatus(status);
        if (companyRemark != null) {
            app.setCompanyRemark(companyRemark);
        }
        if ("reviewing".equals(status) || "interview".equals(status)) {
            app.setReadStatus("1");
        }
        jobApplicationRepository.save(app);

        // 通知学生
        String notiTitle = null;
        String notiContent = null;
        switch (status) {
            case "reviewing": notiTitle = "简历已被查看"; notiContent = "【企业】查看了您的简历"; break;
            case "interview": notiTitle = "进入面试环节"; notiContent = "【企业】邀请您进入面试，请留意面试通知"; break;
            case "rejected": notiTitle = "简历未通过"; notiContent = "【企业】暂时不考虑您的简历"; break;
        }
        if (notiTitle != null && company != null) {
            notificationService.sendNotification(app.getStudentId(), notiTitle, company.getCompanyName() + notiContent, "application", "application");
        }
        log.info("企业更新投递 {} 状态为 {}", applicationId, status);
    }

    @Override
    public PageResult<?> getCompanyInterviews(Long userId, JobSearchDTO dto) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));

        int p = dto.getPage() == null ? 1 : dto.getPage();
        int s = dto.getSize() == null ? 10 : dto.getSize();
        int page = Math.max(p, 1);
        int size = Math.min(Math.max(s, 1), 50);

        List<InterviewInvitation> all = interviewInvitationRepository.findByCompanyId(company.getId());
        int total = all.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);

        List<Map<String, Object>> records = new ArrayList<>();
        if (start < total) {
            for (InterviewInvitation inv : all.subList(start, end)) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", inv.getId());
                map.put("applicationId", inv.getApplicationId());
                map.put("studentId", inv.getStudentId());
                map.put("interviewTime", inv.getInterviewTime());
                map.put("interviewAddress", inv.getInterviewAddress());
                map.put("interviewType", inv.getInterviewType());
                map.put("contactPerson", inv.getContactPerson());
                map.put("contactPhone", inv.getContactPhone());
                map.put("interviewNote", inv.getRemark());
                map.put("status", inv.getStatus());
                map.put("createTime", inv.getCreateTime());

                if (inv.getApplicationId() != null) {
                    jobApplicationRepository.findById(inv.getApplicationId()).ifPresent(app -> {
                        map.put("jobName", jobPositionRepository.findById(app.getJobId())
                                .map(JobPosition::getJobName).orElse(null));
                        map.put("studentName", studentInfoRepository.findById(app.getStudentId())
                                .map(StudentInfo::getRealName).orElse(null));
                    });
                }
                records.add(map);
            }
        }

        return new PageResult<Map<String, Object>>((long) total, records, (long) page, (long) size);
    }

    @Override
    @Transactional
    public void createInterview(Long userId, Map<String, Object> data) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));
        if (!"approved".equals(company.getAuthStatus())) {
            throw new BusinessException(403, "您的企业尚未通过认证审核，暂时无法使用该功能");
        }

        InterviewInvitation inv = new InterviewInvitation();
        inv.setCompanyId(company.getId());
        Object appIdObj = data.get("applicationId");
        if (appIdObj != null) inv.setApplicationId(Long.valueOf(appIdObj.toString()));
        Object studentIdObj = data.get("studentId");
        if (studentIdObj != null) inv.setStudentId(Long.valueOf(studentIdObj.toString()));
        inv.setInterviewTime((String) data.get("interviewTime"));
        inv.setInterviewAddress((String) data.get("interviewAddress"));
        inv.setInterviewType((String) data.get("interviewType"));
        inv.setContactPerson((String) data.get("contactPerson"));
        inv.setContactPhone((String) data.get("contactPhone"));
        inv.setRemark((String) data.get("interviewNote"));
        inv.setStatus("pending");

        if (inv.getApplicationId() != null) {
            jobApplicationRepository.findById(inv.getApplicationId()).ifPresent(app -> {
                inv.setJobId(app.getJobId());
                app.setStatus("interview");
                app.setReadStatus("1");
                jobApplicationRepository.save(app);
            });
        }
        interviewInvitationRepository.save(inv);

        // 通知学生
        Long stuId = inv.getStudentId();
        if (stuId != null) {
            notificationService.sendNotification(stuId,
                    "您收到一份面试邀请",
                    "【" + company.getCompanyName() + "】邀请您参加面试，时间：" + inv.getInterviewTime() + "，地点：" + inv.getInterviewAddress(),
                    "interview", "interview");
        }

        log.info("企业 {} 创建面试邀请 {}", company.getId(), inv.getId());
    }

    @Override
    @Transactional
    public void cancelInterview(Long interviewId, Long userId) {
        InterviewInvitation inv = interviewInvitationRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException(404, "面试记录不存在"));
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(403, "无权操作"));
        if (!company.getId().equals(inv.getCompanyId())) {
            throw new BusinessException(403, "无权操作");
        }
        inv.setStatus("cancelled");
        interviewInvitationRepository.save(inv);
    }

    @Override
    public Map<String, Object> getStatistics(Long userId, String startDate, String endDate) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId).orElse(null);
        if (company == null) {
            return Collections.emptyMap();
        }
        List<JobApplication> apps = jobApplicationRepository.findByCompanyId(company.getId());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalResumes", apps.size());
        stats.put("pendingCount", apps.stream().filter(a -> "pending".equals(a.getStatus())).count());
        stats.put("reviewingCount", apps.stream().filter(a -> "reviewing".equals(a.getStatus())).count());
        stats.put("interviewCount", apps.stream().filter(a -> "interview".equals(a.getStatus())).count());
        stats.put("offerCount", apps.stream().filter(a -> "offer".equals(a.getStatus())).count());
        stats.put("rejectedCount", apps.stream().filter(a -> "rejected".equals(a.getStatus())).count());
        stats.put("acceptedCount", apps.stream().filter(a -> "accepted".equals(a.getStatus())).count());

        long jobCount = jobPositionRepository.findByCompanyId(company.getId()).stream()
                .filter(j -> "published".equals(j.getStatus()))
                .count();
        stats.put("activeJobCount", jobCount);

        return stats;
    }

    @Override
    @Transactional
    public CompanyInfo reApply(Long userId, Map<String, Object> data) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));
        if (!"rejected".equals(company.getAuthStatus())) {
            throw new BusinessException(400, "只能对已驳回的申请重新提交");
        }
        if (data.get("companyName") != null) company.setCompanyName((String) data.get("companyName"));
        if (data.get("contactPerson") != null) company.setContactPerson((String) data.get("contactPerson"));
        if (data.get("contactPhone") != null) company.setContactPhone((String) data.get("contactPhone"));
        if (data.get("contactEmail") != null) company.setContactEmail((String) data.get("contactEmail"));
        if (data.get("province") != null) company.setProvince((String) data.get("province"));
        if (data.get("city") != null) company.setCity((String) data.get("city"));
        if (data.get("district") != null) company.setDistrict((String) data.get("district"));
        if (data.get("address") != null) company.setAddress((String) data.get("address"));
        if (data.get("industry") != null) company.setIndustry((String) data.get("industry"));
        if (data.get("scale") != null) company.setScale((String) data.get("scale"));
        if (data.get("nature") != null) company.setNature((String) data.get("nature"));
        if (data.get("introduction") != null) company.setIntroduction((String) data.get("introduction"));
        if (data.get("unifiedCreditCode") != null) company.setUnifiedCreditCode((String) data.get("unifiedCreditCode"));
        company.setAuthStatus("pending");
        companyInfoRepository.save(company);

        CompanyAuth auth = new CompanyAuth();
        auth.setCompanyId(company.getId());
        auth.setAuthType("营业执照");
        auth.setAuthName(company.getCompanyName());
        auth.setAuditStatus("pending");
        companyAuthRepository.save(auth);

        // 通知该学院的院级老师有新企业重新申请入驻
        if (company.getDeptId() != null) {
            List<SysUser> deptTeachers = sysUserRepository.findByDeptId(company.getDeptId());
            for (SysUser teacher : deptTeachers) {
                // 通过 sys_user_role + sys_role 查询该用户的角色
                List<SysUserRole> userRoles = sysUserRoleRepository.findByUserId(teacher.getId());
                boolean isDeptTeacher = userRoles.stream().anyMatch(ur -> {
                    return sysRoleRepository.findById(ur.getRoleId())
                            .map(SysRole::getRoleKey)
                            .orElse("").equals("dept_teacher");
                });
                if (isDeptTeacher) {
                    notificationService.sendNotification(teacher.getId(),
                            "企业重新申请入驻",
                            "【" + company.getCompanyName() + "】重新提交了入驻申请，请及时审核。",
                            "system", "system");
                }
            }
        }

        log.info("企业 {} 重新申请入驻，已通知相关院级老师", company.getId());
        return company;
    }

    // ==================== Offer管理 ====================

    @Override
    public Map<String, Object> getMyOffers(Long userId, Map<String, Object> params) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));
        Long companyId = company.getId();

        List<OfferLetter> all = offerLetterRepository.findByCompanyId(companyId);
        List<Map<String, Object>> records = all.stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("applicationId", o.getApplicationId());
            m.put("studentId", o.getStudentId());
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
            studentInfoRepository.findByUserId(o.getStudentId()).ifPresent(si -> {
                m.put("studentName", si.getRealName());
                m.put("studentNo", si.getStudentNo());
            });
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", records.size());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> sendOffer(Long userId, Map<String, Object> data) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));
        Long companyId = company.getId();
        Long applicationId = Long.parseLong(data.get("applicationId").toString());
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(404, "申请记录不存在"));
        if (!application.getCompanyId().equals(companyId)) {
            throw new BusinessException(403, "无权操作此申请");
        }

        // 检查是否已发过offer
        List<OfferLetter> existing = offerLetterRepository.findByApplicationId(applicationId);
        if (!existing.isEmpty()) {
            throw new BusinessException(400, "该申请已发放过Offer");
        }

        JobPosition job = jobPositionRepository.findById(application.getJobId()).orElse(null);

        OfferLetter offer = new OfferLetter();
        offer.setApplicationId(applicationId);
        offer.setStudentId(application.getStudentId());
        offer.setCompanyId(companyId);
        offer.setJobId(application.getJobId());
        if (data.get("positionName") != null) offer.setPositionName((String) data.get("positionName"));
        else if (job != null) offer.setPositionName(job.getJobName());
        if (data.get("salary") != null) offer.setSalary((String) data.get("salary"));
        if (data.get("workCity") != null) offer.setWorkCity((String) data.get("workCity"));
        if (data.get("startDate") != null) offer.setStartDate((String) data.get("startDate"));
        if (data.get("probationPeriod") != null) offer.setProbationPeriod((String) data.get("probationPeriod"));
        if (data.get("probationSalary") != null) offer.setProbationSalary((String) data.get("probationSalary"));
        if (data.get("responseDeadline") != null) offer.setResponseDeadline((String) data.get("responseDeadline"));
        offer.setStatus("pending");
        offerLetterRepository.save(offer);

        // 更新申请状态为 offer
        application.setStatus("offer");
        application.setOfferStatus("pending");
        jobApplicationRepository.save(application);

        // 通知学生
        notificationService.sendNotification(
                application.getStudentId(),
                "您收到了一份录用通知",
                "【" + company.getCompanyName() + "】向您发放了Offer，请及时查看并回复",
                "offer", "offer"
        );

        Map<String, Object> result = new HashMap<>();
        result.put("id", offer.getId());
        result.put("status", offer.getStatus());
        return result;
    }

    @Override
    @Transactional
    public void withdrawOffer(Long userId, Long offerId) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));
        OfferLetter offer = offerLetterRepository.findById(offerId)
                .orElseThrow(() -> new BusinessException(404, "Offer不存在"));
        if (!offer.getCompanyId().equals(company.getId())) {
            throw new BusinessException(403, "无权操作此Offer");
        }
        if ("accepted".equals(offer.getStatus()) || "declined".equals(offer.getStatus())) {
            throw new BusinessException(400, "该Offer已被学生处理，无法撤回");
        }
        offer.setStatus("withdrawn");
        offerLetterRepository.save(offer);

        // 通知学生
        notificationService.sendNotification(
                offer.getStudentId(),
                "Offer已被撤回",
                "【" + company.getCompanyName() + "】撤回了向您发放的Offer",
                "offer", "offer"
        );
    }

    // ==================== 面试记录 ====================

    @Override
    @Transactional
    public Map<String, Object> addInterviewRecord(Long userId, Map<String, Object> data) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));
        Long companyId = company.getId();
        Long invitationId = Long.parseLong(data.get("invitationId").toString());

        InterviewInvitation invitation = interviewInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException(404, "面试邀请不存在"));
        if (!invitation.getCompanyId().equals(companyId)) {
            throw new BusinessException(403, "无权操作此面试");
        }

        InterviewRecord record = interviewRecordRepository.findByInvitationId(invitationId)
                .stream().findFirst().orElseGet(InterviewRecord::new);
        record.setInvitationId(invitationId);
        record.setStudentId(invitation.getStudentId());
        record.setCompanyId(companyId);
        if (data.get("interviewResult") != null) record.setInterviewResult((String) data.get("interviewResult"));
        if (data.get("interviewFeedback") != null) record.setInterviewFeedback((String) data.get("interviewFeedback"));
        if (data.get("score") != null) record.setScore(Integer.parseInt(data.get("score").toString()));
        if (data.get("companyRemark") != null) record.setCompanyRemark((String) data.get("companyRemark"));
        interviewRecordRepository.save(record);

        // 通知学生面试结果
        String resultTip = (String) data.get("interviewResult");
        notificationService.sendNotification(
                invitation.getStudentId(),
                "面试结果已录入",
                "【" + company.getCompanyName() + "】已录入您的面试结果，结果为：" + (resultTip != null ? resultTip : "待定"),
                "interview", "interview"
        );

        Map<String, Object> res = new HashMap<>();
        res.put("id", record.getId());
        return res;
    }

    // ==================== 三方协议 ====================

    @Override
    public Map<String, Object> getMyAgreements(Long userId, Map<String, Object> params) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));
        Long companyId = company.getId();

        List<TripartiteAgreement> all = tripartiteAgreementRepository.findByCompanyId(companyId);
        List<Map<String, Object>> records = all.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("studentId", a.getStudentId());
            m.put("agreementNo", a.getAgreementNo());
            m.put("studentSignTime", a.getStudentSignTime());
            m.put("companySignTime", a.getCompanySignTime());
            m.put("schoolSignTime", a.getSchoolSignTime());
            m.put("status", a.getStatus());
            m.put("createTime", a.getCreateTime());
            studentInfoRepository.findByUserId(a.getStudentId()).ifPresent(si -> {
                m.put("studentName", si.getRealName());
                m.put("studentNo", si.getStudentNo());
            });
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", records.size());
        return result;
    }

    @Override
    @Transactional
    public void signAgreement(Long userId, Long agreementId) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));
        TripartiteAgreement agreement = tripartiteAgreementRepository.findById(agreementId)
                .orElseThrow(() -> new BusinessException(404, "协议不存在"));
        if (!agreement.getCompanyId().equals(company.getId())) {
            throw new BusinessException(403, "无权操作此协议");
        }
        if (!"student_signed".equals(agreement.getStatus()) && !"pending".equals(agreement.getStatus())) {
            throw new BusinessException(400, "当前状态不允许企业签署");
        }

        agreement.setCompanySignTime(java.time.LocalDateTime.now().toString());
        agreement.setStatus("company_signed");
        tripartiteAgreementRepository.save(agreement);

        // 通知学生
        notificationService.sendNotification(
                agreement.getStudentId(),
                "三方协议已由企业签署",
                "【" + company.getCompanyName() + "】已签署三方协议，等待学校盖章",
                "agreement", "agreement"
        );
    }

    // ==================== 职位管理 ====================

    @Override
    public PageResult<?> getMyJobs(Long userId, Map<String, Object> params) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));

        int page = params.get("page") == null ? 1 : Integer.parseInt(params.get("page").toString());
        int size = params.get("size") == null ? 10 : Integer.parseInt(params.get("size").toString());
        String keyword = params.get("keyword") == null ? null : params.get("keyword").toString();
        String statusFilter = params.get("status") == null ? null : params.get("status").toString();

        List<JobPosition> all = jobPositionRepository.findByCompanyId(company.getId());
        if (keyword != null && !keyword.trim().isEmpty()) {
            all = all.stream()
                    .filter(j -> j.getJobName().contains(keyword) || (j.getJobCategory() != null && j.getJobCategory().contains(keyword)))
                    .collect(Collectors.toList());
        }
        if (statusFilter != null && !statusFilter.isEmpty()) {
            all = all.stream().filter(j -> statusFilter.equals(j.getStatus())).collect(Collectors.toList());
        }

        all.sort((a, b) -> {
            if (b.getCreateTime() == null && a.getCreateTime() == null) return 0;
            if (b.getCreateTime() == null) return 1;
            if (a.getCreateTime() == null) return -1;
            return b.getCreateTime().compareTo(a.getCreateTime());
        });

        int total = all.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);

        List<Map<String, Object>> records = new ArrayList<>();
        if (start < total) {
            for (JobPosition job : all.subList(start, end)) {
                Map<String, Object> map = toJobMap(job);
                map.put("applicationCount", jobApplicationRepository.findByJobId(job.getId()).size());
                records.add(map);
            }
        }
        return new PageResult<>((long) total, records, (long) page, (long) size);
    }

    @Override
    public Map<String, Object> getJobDetail(Long userId, Long jobId) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));
        JobPosition job = jobPositionRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(404, "职位不存在"));
        if (!job.getCompanyId().equals(company.getId())) {
            throw new BusinessException(403, "无权查看该职位");
        }
        Map<String, Object> map = toJobMap(job);
        List<JobApplication> apps = jobApplicationRepository.findByJobId(jobId);
        map.put("applicationCount", apps.size());
        map.put("viewCount", job.getViewCount());
        return map;
    }

    @Override
    @Transactional
    public JobPosition createJob(Long userId, Map<String, Object> data) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));
        if (!"approved".equals(company.getAuthStatus())) {
            throw new BusinessException(403, "您的企业尚未通过认证审核，暂时无法发布职位");
        }

        JobPosition job = new JobPosition();
        job.setCompanyId(company.getId());
        job.setCompanyName(company.getCompanyName());
        applyJobData(job, data);
        job.setStatus("published");
        job.setIsDeleted("0");
        job.setViewCount(0);
        job.setApplyCount(0);

        String now = java.time.LocalDate.now().toString();
        job.setPublishTime(now);
        if (job.getDeadline() == null) {
            job.setDeadline(java.time.LocalDate.now().plusDays(30).toString());
        }
        return jobPositionRepository.save(job);
    }

    @Override
    @Transactional
    public JobPosition updateJob(Long userId, Long jobId, Map<String, Object> data) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));
        JobPosition job = jobPositionRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(404, "职位不存在"));
        if (!job.getCompanyId().equals(company.getId())) {
            throw new BusinessException(403, "无权修改该职位");
        }
        applyJobData(job, data);
        return jobPositionRepository.save(job);
    }

    @Override
    @Transactional
    public void publishJob(Long userId, Long jobId) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));
        if (!"approved".equals(company.getAuthStatus())) {
            throw new BusinessException(403, "您的企业尚未通过认证审核，暂时无法发布职位");
        }
        JobPosition job = jobPositionRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(404, "职位不存在"));
        if (!job.getCompanyId().equals(company.getId())) {
            throw new BusinessException(403, "无权操作");
        }
        job.setStatus("published");
        jobPositionRepository.save(job);
        log.info("企业 {} 上架职位 {}", company.getId(), jobId);
    }

    @Override
    @Transactional
    public void pauseJob(Long userId, Long jobId) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));
        JobPosition job = jobPositionRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(404, "职位不存在"));
        if (!job.getCompanyId().equals(company.getId())) {
            throw new BusinessException(403, "无权操作");
        }
        job.setStatus("paused");
        jobPositionRepository.save(job);
        log.info("企业 {} 下架职位 {}", company.getId(), jobId);
    }

    @Override
    @Transactional
    public void deleteJob(Long userId, Long jobId) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));
        JobPosition job = jobPositionRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(404, "职位不存在"));
        if (!job.getCompanyId().equals(company.getId())) {
            throw new BusinessException(403, "无权操作");
        }
        job.setIsDeleted("1");
        jobPositionRepository.save(job);
        log.info("企业 {} 删除职位 {}", company.getId(), jobId);
    }

    @Override
    @Transactional
    public void deleteApplications(Long userId, List<Long> applicationIds) {
        CompanyInfo company = companyInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "企业信息不存在"));
        for (Long appId : applicationIds) {
            jobApplicationRepository.findById(appId).ifPresent(app -> {
                if (app.getCompanyId().equals(company.getId())) {
                    jobApplicationRepository.delete(app);
                }
            });
        }
        log.info("企业 {} 批量删除投递 {}", company.getId(), applicationIds);
    }

    private void applyJobData(JobPosition job, Map<String, Object> data) {
        if (data.get("jobName") != null) job.setJobName((String) data.get("jobName"));
        if (data.get("jobCategory") != null) job.setJobCategory((String) data.get("jobCategory"));
        if (data.get("jobType") != null) job.setJobType((String) data.get("jobType"));
        if (data.get("workCity") != null) job.setWorkCity((String) data.get("workCity"));
        if (data.get("workAddress") != null) job.setWorkAddress((String) data.get("workAddress"));
        if (data.get("salaryMin") != null) job.setSalaryMin(Integer.valueOf(data.get("salaryMin").toString()));
        if (data.get("salaryMax") != null) job.setSalaryMax(Integer.valueOf(data.get("salaryMax").toString()));
        if (data.get("salaryMonths") != null) job.setSalaryMonths((String) data.get("salaryMonths"));
        if (data.get("recruitNumber") != null) job.setRecruitNumber(Integer.valueOf(data.get("recruitNumber").toString()));
        if (data.get("requirement") != null) job.setRequirement((String) data.get("requirement"));
        if (data.get("responsibility") != null) job.setResponsibility((String) data.get("responsibility"));
        if (data.get("benefits") != null) job.setBenefits((String) data.get("benefits"));
        if (data.get("educationRequired") != null) job.setEducationRequired((String) data.get("educationRequired"));
        if (data.get("experienceRequired") != null) job.setExperienceRequired((String) data.get("experienceRequired"));
        if (data.get("skillRequired") != null) job.setSkillRequired((String) data.get("skillRequired"));
        if (data.get("isRemote") != null) job.setIsRemote((String) data.get("isRemote"));
        if (data.get("isHighSalary") != null) job.setIsHighSalary((String) data.get("isHighSalary"));
        if (data.get("deadline") != null) job.setDeadline((String) data.get("deadline"));
    }

    private Map<String, Object> toJobMap(JobPosition job) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", job.getId());
        map.put("jobName", job.getJobName());
        map.put("jobCategory", job.getJobCategory());
        map.put("jobType", job.getJobType());
        map.put("workCity", job.getWorkCity());
        map.put("workAddress", job.getWorkAddress());
        map.put("salaryMin", job.getSalaryMin());
        map.put("salaryMax", job.getSalaryMax());
        map.put("salaryMonths", job.getSalaryMonths());
        map.put("recruitNumber", job.getRecruitNumber());
        map.put("requirement", job.getRequirement());
        map.put("responsibility", job.getResponsibility());
        map.put("benefits", job.getBenefits());
        map.put("educationRequired", job.getEducationRequired());
        map.put("experienceRequired", job.getExperienceRequired());
        map.put("skillRequired", job.getSkillRequired());
        map.put("isRemote", job.getIsRemote());
        map.put("isHighSalary", job.getIsHighSalary());
        map.put("viewCount", job.getViewCount());
        map.put("applyCount", job.getApplyCount());
        map.put("status", job.getStatus());
        map.put("publishTime", job.getPublishTime());
        map.put("deadline", job.getDeadline());
        map.put("createTime", job.getCreateTime());
        return map;
    }
}
