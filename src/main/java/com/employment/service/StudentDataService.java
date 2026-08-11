package com.employment.service;

import com.employment.init.DataConstants;
import com.employment.init.RandomDataUtil;
import com.employment.model.entity.*;
import com.employment.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 学生数据生成服务
 * 包含全部学生数据生成逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentDataService {

    private final SysClassRepository sysClassRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final CompanyInfoRepository companyInfoRepository;
    private final JobPositionRepository jobPositionRepository;
    private final EntityManager entityManager;

    private static final long SEED = 42L;
    private static final String BATCH_REMARK = "批量导入学生";
    private static final String STUDENT_PASSWORD = "123456";

    private RandomDataUtil rand;
    private BCryptPasswordEncoder passwordEncoder;

    // 统计
    private int totalStudents = 0;
    private int totalResumes = 0;
    private int totalEmployment = 0;
    private int totalApplications = 0;
    private int totalConversations = 0;
    private int totalInterviews = 0;
    private int totalOffers = 0;
    private int totalAgreements = 0;
    private int totalAgreementsSeq = 0; // 三方协议序号，全局递增不重复

    private static final List<Map.Entry<String, Integer>> EMPLOYMENT_TYPES = Arrays.asList(
            new AbstractMap.SimpleEntry<>("签订三方协议", 52),
            new AbstractMap.SimpleEntry<>("继续深造", 12),
            new AbstractMap.SimpleEntry<>("签订劳动合同", 20),
            new AbstractMap.SimpleEntry<>("自由职业", 7),
            new AbstractMap.SimpleEntry<>("出国出境", 2),
            new AbstractMap.SimpleEntry<>("应征入伍", 1)
    );

    @Transactional
    public void initializeAllStudents() {
        passwordEncoder = new BCryptPasswordEncoder();
        rand = new RandomDataUtil(SEED);

        Long studentRoleId = sysRoleRepository.findAll().stream()
                .filter(r -> "student".equals(r.getRoleKey()))
                .findFirst()
                .map(SysRole::getId)
                .orElseThrow(() -> new RuntimeException("未找到 student 角色，请确保 sys_role 表中有 role_key='student' 的记录"));

        List<SysClass> allClasses = sysClassRepository.findAll();
        if (allClasses.isEmpty()) {
            throw new RuntimeException("sys_class 表为空，请先运行班级导入脚本！");
        }
        log.info("共加载 {} 个班级", allClasses.size());

        List<CompanyInfo> allCompanies = companyInfoRepository.findAll();
        log.info("共加载 {} 家企业", allCompanies.size());

        List<JobPosition> allJobs = jobPositionRepository.findAll();
        log.info("共加载 {} 个职位", allJobs.size());

        int classCount = 0;
        for (SysClass cls : allClasses) {
            classCount++;
            String className = cls.getClassName();
            int gradYear = Integer.parseInt(cls.getGrade());
            double empRate = DataConstants.EMPLOYMENT_RATE.getOrDefault(gradYear, 0.94);

            SysMajor major = entityManager.find(SysMajor.class, cls.getMajorId());
            SysDept dept = entityManager.find(SysDept.class, cls.getDeptId());
            String majorName = major != null ? major.getMajorName() : "";
            String deptName = dept != null ? dept.getDeptName() : "";
            List<String> industryKws = DataConstants.MAJOR_INDUSTRY_KEYWORDS.getOrDefault(majorName, Arrays.asList());

            int stuCount = rand.nextInt(
                    DataConstants.STUDENTS_MAX - DataConstants.STUDENTS_MIN + 1)
                    + DataConstants.STUDENTS_MIN;

            processClass(cls, studentRoleId, gradYear, empRate,
                    majorName, deptName, className, stuCount, industryKws,
                    allCompanies, allJobs);

            if (classCount % 50 == 0) {
                log.info("已处理 {}/{} 个班级...", classCount, allClasses.size());
                entityManager.flush();
                entityManager.clear();
            }
        }

        updateClassStudentCounts();

        log.info("========================================");
        log.info("学生数据生成完成！");
        log.info("总学生数: {}", totalStudents);
        log.info("总简历数: {}", totalResumes);
        log.info("总就业记录: {}", totalEmployment);
        log.info("总投递申请: {}", totalApplications);
        log.info("总谈话记录: {}", totalConversations);
        log.info("总面试邀请: {}", totalInterviews);
        log.info("总Offer: {}", totalOffers);
        log.info("总三方协议: {}", totalAgreements);
        log.info("========================================");
    }

    private void processClass(SysClass cls, Long studentRoleId, int gradYear,
                            double empRate, String majorName, String deptName,
                            String className, int stuCount,
                            List<String> industryKws,
                            List<CompanyInfo> allCompanies,
                            List<JobPosition> allJobs) {

        int enrollYear = gradYear - 4;

        for (int i = 1; i <= stuCount; i++) {
            String studentNo = rand.generateStudentNo(gradYear, cls.getId(), i);

            String gender = rand.randomChoice(Arrays.asList("男", "女"));
            String name = rand.generateName(gender);
            // 一次性生成学生完整画像（姓名、GPA、课程、项目、证书等），保证个人信息和简历内容完全一致
            RandomDataUtil.StudentProfile profile = rand.generateStudentProfile(name, gender, majorName);
            String idCard = rand.generateIdCard(gradYear);
            String birthDate = extractBirthDateFromIdCard(idCard);
            String nation = rand.generateNation("");
            String politics = rand.generatePolitics();
            String phone = rand.generatePhone();
            String email = studentNo + "@student.edu.cn";

            RandomDataUtil.Pair<String, String> pc = rand.generateProvinceCity();
            String province = pc.first;
            String city = pc.second;
            String address = rand.generateAddress(province, city);
            String dormitory = rand.generateDormitory();
            String emergencyContact = rand.generateEmergencyContact();
            String emergencyPhone = rand.generatePhone();

            // SysUser
            SysUser user = new SysUser();
            user.setUsername(studentNo);
            user.setPassword(passwordEncoder.encode(STUDENT_PASSWORD));
            user.setRealName(name);
            user.setGender(gender);
            user.setPhone(phone);
            user.setEmail(email);
            user.setIdCard(idCard);
            user.setStatus("0");
            user.setDeptId(cls.getDeptId());
            user.setMajorId(cls.getMajorId());
            user.setClassName(className);
            user.setClassId(cls.getId());
            user.setStudentNo(studentNo);
            user.setGraduationYear(gradYear);
            user.setRemark(BATCH_REMARK);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            entityManager.persist(user);
            Long userId = user.getId();

            // SysUserRole
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(studentRoleId);
            ur.setCreateTime(LocalDateTime.now());
            ur.setUpdateTime(LocalDateTime.now());
            entityManager.persist(ur);

            // StudentInfo
            StudentInfo info = new StudentInfo();
            info.setUserId(userId);
            info.setStudentNo(studentNo);
            info.setRealName(name);
            info.setGender(gender);
            info.setBirthDate(birthDate);
            info.setIdCard(idCard);
            info.setNation(nation);
            info.setPoliticsStatus(politics);
            info.setPhone(phone);
            info.setEmail(email);
            info.setProvince(province);
            info.setCity(city);
            info.setAddress(address);
            info.setDeptId(cls.getDeptId());
            info.setDeptName(deptName);
            info.setMajorId(cls.getMajorId());
            info.setMajorName(majorName);
            info.setClassName(className);
            info.setClassId(cls.getId());
            info.setGraduationYear(gradYear);
            info.setStudyType("统招");
            info.setDormitory(dormitory);
            info.setEmergencyContact(emergencyContact);
            info.setEmergencyPhone(emergencyPhone);
            info.setAvatar("");
            info.setStatus("graduated");
            info.setCreateTime(LocalDateTime.now());
            info.setUpdateTime(LocalDateTime.now());
            entityManager.persist(info);
            totalStudents++;

            boolean isEmployed = rand.nextDouble() < empRate;

            // 先确定就业去向（公司+岗位），这样简历中才能用真实数据
            String employmentType = isEmployed ? rand.weightedChoiceInt(EMPLOYMENT_TYPES) : null;
            CompanyInfo matchedCompany = null;
            JobPosition matchedJob = null;
            if (isEmployed && !"继续深造".equals(employmentType)
                    && !"应征入伍".equals(employmentType) && !"出国出境".equals(employmentType)) {
                matchedCompany = findMatchingCompany(industryKws, allCompanies);
                matchedJob = findMatchingJob(matchedCompany, industryKws, allJobs);
            }

            // 简历（就业学生传入真实公司信息，未就业学生用XX占位）
            Long resumeId = null;
            if (rand.nextDouble() < 0.5 || isEmployed) {
                RandomDataUtil.ResumeData rd;
                if (isEmployed) {
                    String companyCity = (matchedCompany != null && matchedCompany.getCity() != null)
                            ? matchedCompany.getCity() : null;
                    String companyName = matchedCompany != null ? matchedCompany.getCompanyName() : null;
                    String jobName = matchedJob != null ? matchedJob.getJobName() : null;
                    String companyIndustry = matchedCompany != null ? matchedCompany.getIndustry() : null;
                    String companyScale = matchedCompany != null ? matchedCompany.getScale() : null;
                    String workCity = matchedCompany != null ? matchedCompany.getCity() : null;
                    rd = rand.generateFullResume(profile, enrollYear, gradYear, deptName, majorName, className,
                            companyCity, companyName, jobName, companyIndustry, companyScale, workCity);
                } else {
                    rd = rand.generateSimpleResume(profile.name, enrollYear, gradYear, deptName, majorName, className);
                }

                StudentResume resume = new StudentResume();
                resume.setStudentId(userId);
                resume.setResumeName(rd.resumeName);
                resume.setIsDefault("1");
                resume.setPersonalSummary(rd.personalSummary);
                resume.setEducationExperience(rd.educationExperience);
                resume.setProjectExperience(rd.projectExperience);
                resume.setWorkExperience(rd.workExperience);
                resume.setSkillCertificates(rd.skillCertificates);
                resume.setAwardsHonors(rd.awardsHonors);
                resume.setSelfEvaluation(rd.selfEvaluation);
                resume.setExpectedSalaryMin(rd.expectedSalaryMin);
                resume.setExpectedSalaryMax(rd.expectedSalaryMax);
                resume.setExpectedCity(rd.expectedCity);
                resume.setExpectedPosition(rd.expectedPosition);
                resume.setExpectedIndustry(rd.expectedIndustry);
                resume.setFilePath("");
                resume.setCreateTime(LocalDateTime.now());
                resume.setUpdateTime(LocalDateTime.now());
                entityManager.persist(resume);
                resumeId = resume.getId();
                totalResumes++;
            }

            if (isEmployed) {
                processEmployment(userId, resumeId, gradYear, majorName, employmentType, matchedCompany, matchedJob, industryKws, allCompanies, allJobs);
            } else {
                processConversations(userId, cls, gradYear);
            }
        }
    }

    private void processEmployment(Long userId, Long resumeId, int gradYear,
                                  String majorName, String employmentType,
                                  CompanyInfo matchedCompany, JobPosition matchedJob,
                                  List<String> industryKws,
                                  List<CompanyInfo> allCompanies,
                                  List<JobPosition> allJobs) {

        // 深造 / 入伍 / 出国 — 不创建三方和投递
        if ("继续深造".equals(employmentType) || "应征入伍".equals(employmentType) || "出国出境".equals(employmentType)) {
            EmploymentRecord er = new EmploymentRecord();
            er.setStudentId(userId);
            er.setEmploymentType(employmentType);
            er.setCompanyName("继续深造".equals(employmentType) ? "继续深造" :
                    ("应征入伍".equals(employmentType) ? "应征入伍" : "出国出境"));
            er.setPositionName("继续深造".equals(employmentType) ? "硕士研究生在读" :
                    ("应征入伍".equals(employmentType) ? "部队服役" : "境外深造/工作"));
            er.setIsThreePartySigned("0");
            er.setAuditStatus("approved");
            er.setCreateTime(LocalDateTime.now());
            er.setUpdateTime(LocalDateTime.now());
            entityManager.persist(er);
            totalEmployment++;
            return;
        }

        // 找工作型：使用已确定的 company 和 job
        CompanyInfo company = matchedCompany != null ? matchedCompany : findMatchingCompany(industryKws, allCompanies);
        JobPosition job = matchedJob != null ? matchedJob : findMatchingJob(company, industryKws, allJobs);

        if (company == null || job == null) {
            EmploymentRecord er = new EmploymentRecord();
            er.setStudentId(userId);
            er.setEmploymentType(employmentType);
            er.setCompanyName("已就业（匹配企业未找到）");
            er.setSalary(rand.generateSalary(null, null));
            er.setIsThreePartySigned("0");
            er.setAuditStatus("approved");
            er.setCreateTime(LocalDateTime.now());
            er.setUpdateTime(LocalDateTime.now());
            entityManager.persist(er);
            totalEmployment++;
            return;
        }

        boolean isThreeParty = "签订三方协议".equals(employmentType);

        // 投递记录（签约那家）
        JobApplication acceptedApp = new JobApplication();
        acceptedApp.setJobId(job.getId());
        acceptedApp.setStudentId(userId);
        acceptedApp.setCompanyId(company.getId());
        acceptedApp.setResumeId(resumeId);
        acceptedApp.setStatus("accepted");
        acceptedApp.setReadStatus("1");
        acceptedApp.setInterviewStatus("passed");
        acceptedApp.setOfferStatus("offered");
        acceptedApp.setCreateTime(LocalDateTime.now());
        acceptedApp.setUpdateTime(LocalDateTime.now());
        entityManager.persist(acceptedApp);
        Long acceptedAppId = acceptedApp.getId();
        totalApplications++;

        // 再投1-2家（与 primary job 使用相同的行业匹配逻辑）
        int extraCount = rand.nextInt(2) + 1;
        List<JobPosition> otherJobs = findOtherJobs(company.getId(), job.getId(), industryKws, allJobs, allCompanies, extraCount);
        for (JobPosition oj : otherJobs) {
            CompanyInfo ojComp = findCompanyById(oj.getCompanyId(), allCompanies);
            if (ojComp == null) continue;
            String intStatus = rand.nextDouble() < 0.7 ? "passed" : "no_response";
            JobApplication extra = new JobApplication();
            extra.setJobId(oj.getId());
            extra.setStudentId(userId);
            extra.setCompanyId(ojComp.getId());
            extra.setResumeId(resumeId);
            extra.setStatus("rejected");
            extra.setReadStatus("1");
            extra.setInterviewStatus(intStatus);
            extra.setOfferStatus("no_response");
            extra.setCreateTime(LocalDateTime.now());
            extra.setUpdateTime(LocalDateTime.now());
            entityManager.persist(extra);
            totalApplications++;
        }

        // 面试邀请
        String interviewAddr = (company.getAddress() != null && !company.getAddress().isEmpty())
                ? company.getAddress() : (company.getCity() != null ? company.getCity() + "公司总部" : "待通知");
        InterviewInvitation inv = new InterviewInvitation();
        inv.setApplicationId(acceptedAppId);
        inv.setStudentId(userId);
        inv.setCompanyId(company.getId());
        inv.setJobId(job.getId());
        inv.setInterviewTime(rand.generateInterviewTime(gradYear));
        inv.setInterviewAddress(interviewAddr);
        inv.setInterviewType(rand.randomChoice(Arrays.asList("线上面试", "现场面试", "电话面试")));
        inv.setContactPerson("HR");
        inv.setContactPhone(rand.generatePhone());
        inv.setStatus("completed");
        inv.setCreateTime(LocalDateTime.now());
        inv.setUpdateTime(LocalDateTime.now());
        entityManager.persist(inv);
        Long invId = inv.getId();
        totalInterviews++;

        // 面试记录
        InterviewRecord ir = new InterviewRecord();
        ir.setInvitationId(invId);
        ir.setStudentId(userId);
        ir.setCompanyId(company.getId());
        ir.setInterviewResult("passed");
        ir.setInterviewFeedback("面试表现良好，专业能力符合岗位要求，予以录用。");
        ir.setScore(rand.nextInt(20) + 80);
        entityManager.persist(ir);

        // Offer
        int salMin = job.getSalaryMin() != null ? job.getSalaryMin() : 5000;
        int salMax = job.getSalaryMax() != null ? job.getSalaryMax() : 8000;
        OfferLetter offer = new OfferLetter();
        offer.setApplicationId(acceptedAppId);
        offer.setStudentId(userId);
        offer.setCompanyId(company.getId());
        offer.setJobId(job.getId());
        offer.setSalary(rand.generateSalary(salMin, salMax));
        offer.setWorkCity(job.getWorkCity());
        offer.setStartDate((gradYear + 1) + "-01-01");
        offer.setProbationPeriod("3个月");
        offer.setProbationSalary(rand.generateProbationSalary(salMin));
        offer.setStatus("accepted");
        offer.setResponseDeadline(gradYear + "-05-01");
        offer.setCreateTime(LocalDateTime.now());
        offer.setUpdateTime(LocalDateTime.now());
        entityManager.persist(offer);
        totalOffers++;

        // EmploymentRecord
        EmploymentRecord er = new EmploymentRecord();
        er.setStudentId(userId);
        er.setEmploymentType(employmentType);
        er.setCompanyName(company.getCompanyName());
        er.setCompanyCode(company.getCompanyCode());
        er.setCompanyScale(company.getScale());
        er.setCompanyIndustry(company.getIndustry());
        er.setPositionName(job.getJobName());
        er.setWorkCity(job.getWorkCity());
        er.setWorkProvince(company.getProvince());
        er.setSalary(rand.generateSalary(salMin, salMax));
        er.setIsThreePartySigned(isThreeParty ? "1" : "0");
        if (isThreeParty) {
            er.setThreePartyNo(String.format("TP%d%06d", gradYear, ++totalAgreementsSeq));
        }
        er.setContractStartDate(gradYear + "-07-01");
        er.setContractEndDate(gradYear + 3 + "-06-30");
        er.setProbationSalary(rand.generateProbationSalary(salMin));
        er.setAuditStatus("approved");
        er.setCreateTime(LocalDateTime.now());
        er.setUpdateTime(LocalDateTime.now());
        entityManager.persist(er);
        Long erId = er.getId();
        totalEmployment++;

        // TripartiteAgreement
        if (isThreeParty) {
            TripartiteAgreement ta = new TripartiteAgreement();
            ta.setStudentId(userId);
            ta.setCompanyId(company.getId());
            ta.setEmploymentRecordId(erId);
            ta.setAgreementNo(er.getThreePartyNo());
            ta.setStudentSignTime(gradYear + "-03-01");
            ta.setCompanySignTime(gradYear + "-04-15");
            ta.setSchoolSignTime(gradYear + "-06-01");
            ta.setStatus("approved");
            ta.setCreateTime(LocalDateTime.now());
            ta.setUpdateTime(LocalDateTime.now());
            entityManager.persist(ta);
            totalAgreements++;
        }

        // 5%概率已就业学生也有1次谈话
        if (rand.nextDouble() < 0.05) {
            processConversations(userId, null, gradYear);
        }
    }

    private void processConversations(Long userId, SysClass cls, int gradYear) {
        // SPEC要求：所有未就业学生必须有谈心谈话记录
        // advisorId 为 null 时应从 sys_user 中查找 class_teacher 角色的用户兜底
        Long teacherId = null;
        if (cls != null) {
            teacherId = cls.getAdvisorId();
        }
        if (teacherId == null) {
            // 兜底：从 sys_user 中查找 class_teacher 角色的第一个用户
            Long fallbackTeacherId = sysUserRoleRepository.findAll().stream()
                    .filter(sur -> {
                        SysRole role = entityManager.find(SysRole.class, sur.getRoleId());
                        return role != null && "class_teacher".equals(role.getRoleKey());
                    })
                    .map(sur -> sur.getUserId())
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            teacherId = fallbackTeacherId;
        }
        if (teacherId == null) {
            log.warn("无法找到辅导员ID，学生 {} 的谈话记录将跳过", userId);
            return;
        }

        int count = rand.nextInt(3) + 2;

        List<Map.Entry<String, Integer>> types = Arrays.asList(
                new AbstractMap.SimpleEntry<>("就业指导", 40),
                new AbstractMap.SimpleEntry<>("心理疏导", 25),
                new AbstractMap.SimpleEntry<>("学业辅导", 15),
                new AbstractMap.SimpleEntry<>("生活关怀", 15),
                new AbstractMap.SimpleEntry<>("其他", 5)
        );

        for (int j = 0; j < count; j++) {
            String convType = rand.weightedChoiceInt(types);
            RandomDataUtil.ConversationData cd = rand.generateConversation(convType, gradYear, j, count);

            ConversationRecord conv = new ConversationRecord();
            conv.setTeacherId(teacherId);
            conv.setStudentId(userId);
            conv.setConversationTime(cd.getConversationTime());
            conv.setConversationType(cd.getConversationType());
            conv.setConversationPlace(cd.getConversationPlace());
            conv.setTopic(cd.getTopic());
            conv.setContent(cd.getContent());
            conv.setResult(cd.getResult());
            conv.setNextPlan(cd.getNextPlan());
            conv.setCreateTime(LocalDateTime.now());
            conv.setUpdateTime(LocalDateTime.now());
            entityManager.persist(conv);
            totalConversations++;
        }
    }

    private CompanyInfo findMatchingCompany(List<String> keywords, List<CompanyInfo> all) {
        for (String kw : keywords) {
            for (CompanyInfo c : all) {
                if (c.getIndustry() != null && c.getIndustry().contains(kw)) {
                    return c;
                }
            }
        }
        return all.isEmpty() ? null : rand.randomChoice(all);
    }

    private JobPosition findMatchingJob(CompanyInfo company, List<String> keywords, List<JobPosition> all) {
        if (company == null) {
            // 无公司时，按行业关键词找匹配职位
            List<JobPosition> candidates = new ArrayList<>();
            for (String kw : keywords) {
                for (JobPosition j : all) {
                    String jobName = j.getJobName() != null ? j.getJobName() : "";
                    String jobCat = j.getJobCategory() != null ? j.getJobCategory() : "";
                    if (jobName.contains(kw) || jobCat.contains(kw)) {
                        candidates.add(j);
                    }
                }
            }
            if (!candidates.isEmpty()) return rand.randomChoice(candidates);
            return all.isEmpty() ? null : rand.randomChoice(all);
        }
        // 先找同公司匹配行业的职位（按行业关键词精确匹配）
        List<JobPosition> candidates = new ArrayList<>();
        for (String kw : keywords) {
            for (JobPosition j : all) {
                if (!j.getCompanyId().equals(company.getId())) continue;
                // 优先匹配公司行业字段（包含 /），其次匹配职位名/类别
                if (company.getIndustry() != null && company.getIndustry().contains(kw)) {
                    candidates.add(j);
                }
            }
        }
        if (!candidates.isEmpty()) return rand.randomChoice(candidates);
        // 保底：同公司任意职位
        for (JobPosition j : all) {
            if (j.getCompanyId().equals(company.getId())) candidates.add(j);
        }
        if (!candidates.isEmpty()) return rand.randomChoice(candidates);
        // 保底：随机职位
        return all.isEmpty() ? null : rand.randomChoice(all);
    }

    private List<JobPosition> findOtherJobs(Long excludeCompanyId, Long excludeJobId,
                                            List<String> keywords,
                                            List<JobPosition> all, List<CompanyInfo> allCompanies, int count) {
        // 优先选同行业（同专业匹配）的其他公司职位
        List<JobPosition> pool = new ArrayList<>();
        for (JobPosition j : all) {
            if (j.getId().equals(excludeJobId)) continue;
            if (j.getCompanyId().equals(excludeCompanyId)) continue;
            // 检查是否是同行业职位
            CompanyInfo cj = findCompanyById(j.getCompanyId(), allCompanies);
            boolean sameIndustry = false;
            if (cj != null && cj.getIndustry() != null) {
                for (String kw : keywords) {
                    if (cj.getIndustry().contains(kw)) {
                        sameIndustry = true;
                        break;
                    }
                }
            }
            if (sameIndustry) pool.add(j);
        }
        if (pool.size() < count) {
            // 补足：加入不同行业的职位
            for (JobPosition j : all) {
                if (pool.size() >= count) break;
                if (j.getId().equals(excludeJobId)) continue;
                if (j.getCompanyId().equals(excludeCompanyId)) continue;
                pool.add(j);
            }
        }
        Collections.shuffle(pool, new Random(rand.nextInt(Integer.MAX_VALUE)));
        return pool.subList(0, Math.min(count, pool.size()));
    }

    private CompanyInfo findCompanyById(Long id, List<CompanyInfo> all) {
        for (CompanyInfo c : all) {
            if (c.getId().equals(id)) return c;
        }
        return null;
    }

    private void updateClassStudentCounts() {
        log.info("更新班级学生数...");
        List<SysClass> classes = sysClassRepository.findAll();
        for (SysClass cls : classes) {
            Long cnt = entityManager.createQuery(
                            "SELECT COUNT(u) FROM SysUser u WHERE u.classId = :cid AND u.remark = :rmk", Long.class)
                    .setParameter("cid", cls.getId())
                    .setParameter("rmk", BATCH_REMARK)
                    .getSingleResult();
            cls.setStudentCount(cnt.intValue());
            entityManager.merge(cls);
        }
        entityManager.flush();
    }

    private String extractBirthDateFromIdCard(String idCard) {
        if (idCard == null || idCard.length() < 14) return "2000-01-01";
        return idCard.substring(6, 10) + "-" + idCard.substring(10, 12) + "-" + idCard.substring(12, 14);
    }
}
