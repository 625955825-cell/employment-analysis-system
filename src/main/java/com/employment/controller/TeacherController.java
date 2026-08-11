package com.employment.controller;

import com.employment.common.Constants;
import com.employment.common.Result;
import com.employment.config.OperationLog;
import com.employment.model.entity.EmploymentRecord;
import com.employment.model.entity.StudentInfo;
import com.employment.repository.EmploymentRecordRepository;
import com.employment.repository.StudentInfoRepository;
import com.employment.security.SecurityUtils;
import com.employment.service.StatsCacheService;
import com.employment.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;
    private final SecurityUtils securityUtils;
    private final EmploymentRecordRepository employmentRecordRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final StatsCacheService statsCacheService;

    @GetMapping("/my-class")
    public Result<Map<String, Object>> getMyClass() {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(teacherService.getMyClass(userId));
    }

    @GetMapping("/students")
    public Result<?> getStudents(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        Object result = teacherService.getStudents(userId, role, keyword, page, size);
        return Result.success(result);
    }

    @GetMapping("/classes")
    public Result<List<Map<String, Object>>> getClasses() {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        return Result.success(teacherService.getClasses(userId, role));
    }

    @GetMapping("/employment-stats")
    public Result<Map<String, Object>> getEmploymentStats(@RequestParam(required = false) Integer graduationYear) {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        return Result.success(teacherService.getEmploymentStats(userId, role, graduationYear));
    }

    @GetMapping("/class-employment-stats/{classId}")
    public Result<Map<String, Object>> getClassEmploymentStats(@PathVariable Long classId,
            @RequestParam(required = false) Integer graduationYear) {
        return Result.success(teacherService.getClassEmploymentStats(classId, graduationYear));
    }

    @GetMapping("/employment-detail")
    public Result<Map<String, Object>> getEmploymentDetail(
            @RequestParam(required = false) Long classId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        return Result.success(teacherService.getEmploymentDetail(userId, role, classId, page, size));
    }

    @GetMapping("/employment-detail-all")
    public Result<Map<String, Object>> getEmploymentDetailAll(@RequestParam(required = false) Long classId) {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        return Result.success(teacherService.getEmploymentDetailAll(userId, role, classId));
    }

    @GetMapping("/employment-pending")
    public Result<List<Map<String, Object>>> getPendingEmployments() {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        return Result.success(teacherService.getPendingEmployments(userId, role));
    }

    @GetMapping("/employment-history")
    public Result<List<Map<String, Object>>> getEmploymentHistory() {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        return Result.success(teacherService.getEmploymentHistory(userId, role));
    }

    @PutMapping("/employment/{id}/audit")
    @Transactional
    @OperationLog(module = "就业管理", content = "审核就业登记")
    public Result<Void> auditEmployment(@PathVariable Long id, @RequestParam String action, @RequestParam(required = false) String remark) {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        EmploymentRecord record = employmentRecordRepository.findById(id)
                .orElseThrow(() -> new com.employment.exception.BusinessException(404, "记录不存在"));

        if (Constants.ROLE_CLASS_TEACHER.equals(role) || Constants.ROLE_DEPT_TEACHER.equals(role)) {
            if ("approve".equals(action)) {
                record.setAuditStatus("approved");
            } else if ("reject".equals(action)) {
                record.setAuditStatus("rejected");
            } else {
                return Result.error("无效的审核操作");
            }
            record.setAuditUserId(userId);
            record.setAuditTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            if (StringUtils.hasText(remark)) {
                record.setAuditRemark(remark);
            }
            employmentRecordRepository.save(record);
            StudentInfo stu = studentInfoRepository.findById(record.getStudentId()).orElse(null);
            if (stu != null) {
                statsCacheService.evictByStudent(stu.getClassId(), stu.getDeptId());
            }
            return Result.success(null);
        }
        return Result.error("无权限进行此操作");
    }

    @GetMapping("/conversations")
    public Result<List<Map<String, Object>>> getConversations() {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(teacherService.getConversations(userId));
    }

    @PostMapping("/conversation")
    @Transactional
    @OperationLog(module = "就业管理", content = "发起沟通")
    public Result<Void> createConversation(@RequestBody Map<String, Object> data) {
        Long userId = securityUtils.getCurrentUserId();
        teacherService.createConversation(userId, data);
        return Result.success(null);
    }

    @DeleteMapping("/conversation/{id}")
    @Transactional
    @OperationLog(module = "就业管理", content = "删除沟通记录")
    public Result<Void> deleteConversation(@PathVariable Long id) {
        teacherService.deleteConversation(id);
        return Result.success(null);
    }

    @GetMapping("/conversation/{id}")
    public Result<Map<String, Object>> getConversation(@PathVariable Long id) {
        return Result.success(teacherService.getConversation(id));
    }

    @PutMapping("/conversation/{id}")
    @Transactional
    public Result<Void> updateConversation(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        teacherService.updateConversation(id, data);
        return Result.success(null);
    }

    @GetMapping("/permission-requests")
    public Result<List<Map<String, Object>>> getPermissionRequests() {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        return Result.success(teacherService.getPermissionRequests(userId, role));
    }

    @PutMapping("/permission-request/{id}/audit")
    @Transactional
    @OperationLog(module = "就业管理", content = "审核数据权限申请")
    public Result<Void> auditPermissionRequest(@PathVariable Long id, @RequestParam String action, @RequestParam(required = false) String remark) {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        teacherService.auditPermissionRequest(id, action, remark, userId, role);
        return Result.success(null);
    }

    @GetMapping("/permission-history")
    public Result<List<Map<String, Object>>> getPermissionHistory() {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        return Result.success(teacherService.getPermissionHistory(userId, role));
    }

    @GetMapping("/employment-reminders")
    public Result<List<Map<String, Object>>> getEmploymentReminders() {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(teacherService.getEmploymentReminders(userId));
    }

    @PostMapping("/employment-reminder")
    @Transactional
    public Result<Void> sendEmploymentReminder(@RequestBody Map<String, Object> data) {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        if (!Constants.ROLE_DEPT_TEACHER.equals(role) && !Constants.ROLE_DATA_ANALYST.equals(role)) {
            return Result.error("只有院级老师或数据分析员可以发送就业提醒");
        }
        teacherService.sendEmploymentReminder(userId, data);
        return Result.success(null);
    }

    @PutMapping("/employment-reminder/{id}/read")
    public Result<Void> markReminderAsRead(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        teacherService.markReminderAsRead(id, userId);
        return Result.success(null);
    }

    @GetMapping("/employment-reminder/unread-count")
    public Result<Integer> getUnreadReminderCount() {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(teacherService.getUnreadReminderCount(userId));
    }

    /**
     * 数据分析员：批量通知未就业学生对应的班主任
     */
    @PostMapping("/employment-reminder/batch-notify")
    @Transactional
    public Result<Map<String, Object>> batchNotifyUnemployedStudents(@RequestBody Map<String, Object> data) {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        if (!Constants.ROLE_DATA_ANALYST.equals(role)) {
            return Result.error("只有数据分析员可以执行此操作");
        }
        Integer graduationYear = data.get("graduationYear") != null
                ? Integer.valueOf(data.get("graduationYear").toString()) : null;
        Map<String, Object> result = teacherService.batchNotifyAdvisors(userId, graduationYear);
        return Result.success(result);
    }

    @GetMapping("/company-auth-pending")
    public Result<List<Map<String, Object>>> getPendingCompanyAuths() {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        return Result.success(teacherService.getPendingCompanyAuths(userId, role));
    }

    @PutMapping("/company-auth/{id}/audit")
    @Transactional
    @OperationLog(module = "企业管理", content = "审核企业入驻")
    public Result<Void> auditCompanyAuth(@PathVariable Long id, @RequestParam String action, @RequestParam(required = false) String remark) {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        teacherService.auditCompanyAuth(id, action, remark, userId, role);
        return Result.success(null);
    }

    @GetMapping("/agreements")
    public Result<List<Map<String, Object>>> getAgreements() {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        return Result.success(teacherService.getAgreements(userId, role));
    }

    @PutMapping("/agreement/{id}/sign")
    @Transactional
    public Result<Void> signAgreement(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        teacherService.signAgreement(id, userId, role);
        return Result.success(null);
    }

    @GetMapping("/agreement-stats")
    public Result<List<Map<String, Object>>> getAgreementStats() {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        return Result.success(teacherService.getAgreementStats(userId, role));
    }

    @GetMapping("/home-stats")
    public Result<Map<String, Object>> getHomeStats() {
        Long userId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentRole();
        return Result.success(teacherService.getDashboard(userId, role));
    }
}
