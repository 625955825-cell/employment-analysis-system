package com.employment.config;

import com.employment.model.entity.ClassEmploymentReminder;
import com.employment.model.entity.EmploymentRecord;
import com.employment.model.entity.SysClass;
import com.employment.model.entity.SysUser;
import com.employment.model.entity.StudentInfo;
import com.employment.repository.*;
import com.employment.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class EmploymentReminderScheduler {

    private final StudentInfoRepository studentInfoRepository;
    private final EmploymentRecordRepository employmentRecordRepository;
    private final SysClassRepository sysClassRepository;
    private final SysUserRepository sysUserRepository;
    private final ClassEmploymentReminderRepository reminderRepository;
    private final NotificationService notificationService;

    /**
     * 每天早上9点执行，检查未就业学生并发送提醒
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void dailyEmploymentReminder() {
        log.info("[Scheduler] 开始执行每日就业提醒任务");

        int currentYear = LocalDate.now().getYear();
        LocalDate todayDate = LocalDate.now();

        List<StudentInfo> allStudents = studentInfoRepository.findAll();
        List<StudentInfo> targetStudents = new ArrayList<>();

        for (StudentInfo student : allStudents) {
            if (student.getGraduationYear() == null) continue;
            Integer gradYear = student.getGraduationYear();
            if (gradYear <= currentYear && gradYear >= currentYear - 3) {
                targetStudents.add(student);
            }
        }

        if (targetStudents.isEmpty()) {
            log.info("[Scheduler] 没有需要检查的学生");
            return;
        }

        // 构建 studentId -> latest EmploymentRecord 映射
        Map<Long, EmploymentRecord> latestMap = new HashMap<>();
        for (StudentInfo student : targetStudents) {
            List<EmploymentRecord> records = employmentRecordRepository.findByStudentId(student.getId());
            if (records != null && !records.isEmpty()) {
                records.sort((a, b) -> {
                    if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
                    if (a.getCreateTime() == null) return 1;
                    if (b.getCreateTime() == null) return -1;
                    return b.getCreateTime().compareTo(a.getCreateTime());
                });
                latestMap.put(student.getId(), records.get(0));
            }
        }

        // 按班级分组未就业学生
        Map<Long, List<StudentInfo>> byClass = new LinkedHashMap<>();
        for (StudentInfo student : targetStudents) {
            EmploymentRecord rec = latestMap.get(student.getId());
            boolean employed = rec != null && "approved".equals(rec.getAuditStatus());
            if (!employed) {
                Long classId = student.getClassId();
                if (classId != null) {
                    List<StudentInfo> list = byClass.computeIfAbsent(classId, k -> new ArrayList<>());
                    list.add(student);
                }
            }
        }

        if (byClass.isEmpty()) {
            log.info("[Scheduler] 所有目标学生均已就业，无需提醒");
            return;
        }

        // 获取班级信息
        Map<Long, SysClass> classMap = new HashMap<>();
        for (SysClass c : sysClassRepository.findAllById(byClass.keySet())) {
            classMap.put(c.getId(), c);
        }

        int reminderCount = 0;
        for (Map.Entry<Long, List<StudentInfo>> entry : byClass.entrySet()) {
            Long classId = entry.getKey();
            List<StudentInfo> unemployedStudents = entry.getValue();
            SysClass cls = classMap.get(classId);

            if (cls == null || cls.getAdvisorId() == null) continue;

            SysUser advisor = sysUserRepository.findById(cls.getAdvisorId()).orElse(null);
            if (advisor == null) continue;

            // 检查是否今天已经发过提醒（避免重复发送）
            List<ClassEmploymentReminder> todayReminders = reminderRepository
                    .findByReceiverIdOrderByCreateTimeDesc(advisor.getId()).stream()
                    .filter(r -> r.getCreateTime() != null &&
                            r.getCreateTime().toLocalDate().equals(todayDate))
                    .collect(java.util.stream.Collectors.toList());
            boolean alreadySent = todayReminders.stream()
                    .anyMatch(r -> classId.equals(r.getClassId()));
            if (alreadySent) {
                log.debug("班主任 {} 今天已收到班级 {} 的提醒，跳过", advisor.getRealName(), cls.getClassName());
                continue;
            }

            // 统计就业数据
            long totalClassStudents = 0;
            long employedClassStudents = 0;
            for (StudentInfo s : targetStudents) {
                if (classId.equals(s.getClassId())) {
                    totalClassStudents++;
                    EmploymentRecord r = latestMap.get(s.getId());
                    if (r != null && "approved".equals(r.getAuditStatus())) {
                        employedClassStudents++;
                    }
                }
            }
            double empRate = totalClassStudents > 0 ? (double) employedClassStudents / totalClassStudents * 100 : 0;

            // 保存提醒记录
            ClassEmploymentReminder reminder = new ClassEmploymentReminder();
            reminder.setSenderId(1L);
            reminder.setSenderName("系统自动");
            reminder.setReceiverId(advisor.getId());
            reminder.setReceiverName(advisor.getRealName());
            reminder.setClassId(classId);
            reminder.setClassName(cls.getClassName());
            reminder.setTitle("【就业提醒】" + cls.getClassName() + " 尚有 " + unemployedStudents.size() + " 名学生未登记就业");
            reminder.setContent("截止 " + todayDate + "，" + cls.getClassName() + " 仍有 " + unemployedStudents.size()
                    + " 名学生尚未完成就业登记。请及时与学生沟通，督促其尽快完成就业登记。");
            reminder.setEmploymentRate(String.format("%.1f", empRate) + "%");
            reminder.setTotalStudents((int) totalClassStudents);
            reminder.setEmployedStudents((int) employedClassStudents);
            reminder.setUnemployedStudents(unemployedStudents.size());
            reminder.setStatus("unread");
            reminder.setIsRead("0");
            reminderRepository.save(reminder);

            // 发送实时通知（WebSocket推送）
            notificationService.sendNotification(
                    advisor.getId(),
                    reminder.getTitle(),
                    reminder.getContent(),
                    "system",
                    "system"
            );

            reminderCount++;
            log.info("[Scheduler] 已向班主任 {} 发送班级 {} 的就业提醒，未就业学生 {} 人",
                    advisor.getRealName(), cls.getClassName(), unemployedStudents.size());
        }

        log.info("[Scheduler] 每日就业提醒任务完成，共发送 {} 条提醒", reminderCount);
    }

    /**
     * 每周一早上9点执行更详细的统计报告
     */
    @Scheduled(cron = "0 0 9 ? * MON")
    @Transactional
    public void weeklyEmploymentReport() {
        log.info("[Scheduler] 开始执行每周就业统计报告任务");

        int currentYear = LocalDate.now().getYear();
        String today = LocalDate.now().toString();

        List<StudentInfo> allStudents = studentInfoRepository.findAll();
        List<StudentInfo> targetStudents = new ArrayList<>();
        for (StudentInfo s : allStudents) {
            if (s.getGraduationYear() != null && s.getGraduationYear().equals(currentYear)) {
                targetStudents.add(s);
            }
        }

        if (targetStudents.isEmpty()) {
            log.info("[Scheduler] 本年度无毕业生，跳过周报");
            return;
        }

        Map<Long, EmploymentRecord> latestMap = new HashMap<>();
        for (StudentInfo student : targetStudents) {
            List<EmploymentRecord> records = employmentRecordRepository.findByStudentId(student.getId());
            if (records != null && !records.isEmpty()) {
                records.sort((a, b) -> {
                    if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
                    if (a.getCreateTime() == null) return 1;
                    if (b.getCreateTime() == null) return -1;
                    return b.getCreateTime().compareTo(a.getCreateTime());
                });
                latestMap.put(student.getId(), records.get(0));
            }
        }

        // 按班级统计
        Map<Long, List<StudentInfo>> byClass = new LinkedHashMap<>();
        for (StudentInfo s : targetStudents) {
            if (s.getClassId() != null) {
                List<StudentInfo> list = byClass.computeIfAbsent(s.getClassId(), k -> new ArrayList<>());
                list.add(s);
            }
        }

        Map<Long, SysClass> classMap = new HashMap<>();
        for (SysClass c : sysClassRepository.findAllById(byClass.keySet())) {
            classMap.put(c.getId(), c);
        }

        int reportCount = 0;

        for (Map.Entry<Long, List<StudentInfo>> entry : byClass.entrySet()) {
            Long classId = entry.getKey();
            SysClass cls = classMap.get(classId);
            if (cls == null || cls.getAdvisorId() == null) continue;

            SysUser advisor = sysUserRepository.findById(cls.getAdvisorId()).orElse(null);
            if (advisor == null) continue;

            List<StudentInfo> students = entry.getValue();
            long employed = 0;
            long pending = 0;
            for (StudentInfo s : students) {
                EmploymentRecord r = latestMap.get(s.getId());
                if (r != null) {
                    if ("approved".equals(r.getAuditStatus())) employed++;
                    else if ("pending".equals(r.getAuditStatus())) pending++;
                }
            }
            double rate = students.size() > 0 ? (double) employed / students.size() * 100 : 0;
            long unemployed = Math.max(0, students.size() - employed - pending);

            String title = "【周报】" + cls.getClassName() + " 就业率 " + String.format("%.1f", rate) + "%";
            StringBuilder sb = new StringBuilder();
            sb.append(currentYear).append("届毕业生就业周报（").append(today).append("）：\n");
            sb.append("班级总人数：").append(students.size()).append(" 人\n");
            sb.append("已就业：").append(employed).append(" 人（").append(String.format("%.1f", rate)).append("%%）\n");
            sb.append("待审核：").append(pending).append(" 人\n");
            sb.append("未登记：").append(unemployed).append(" 人\n");
            sb.append("请持续关注学生就业动态。");
            String content = sb.toString();

            ClassEmploymentReminder reminder = new ClassEmploymentReminder();
            reminder.setSenderId(1L);
            reminder.setSenderName("系统自动");
            reminder.setReceiverId(advisor.getId());
            reminder.setReceiverName(advisor.getRealName());
            reminder.setClassId(classId);
            reminder.setClassName(cls.getClassName());
            reminder.setTitle(title);
            reminder.setContent(content);
            reminder.setEmploymentRate(String.format("%.1f", rate) + "%");
            reminder.setTotalStudents(students.size());
            reminder.setEmployedStudents((int) employed);
            reminder.setUnemployedStudents((int) unemployed);
            reminder.setStatus("unread");
            reminder.setIsRead("0");
            reminderRepository.save(reminder);

            notificationService.sendNotification(advisor.getId(), title, content, "system", "system");

            reportCount++;
        }

        log.info("[Scheduler] 每周就业统计报告完成，共 {} 个班级", reportCount);
    }
}
