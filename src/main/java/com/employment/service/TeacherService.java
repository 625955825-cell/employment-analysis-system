package com.employment.service;

import java.util.List;
import java.util.Map;

public interface TeacherService {
    Map<String, Object> getMyClass(Long userId);
    List<Map<String, Object>> getStudents(Long userId, String role, String keyword);
    Object getStudents(Long userId, String role, String keyword, Integer page, Integer size);
    List<Map<String, Object>> getClasses(Long userId, String role);
    Map<String, Object> getEmploymentStats(Long userId, String role);
    Map<String, Object> getEmploymentStats(Long userId, String role, Integer graduationYear);
    Map<String, Object> getClassEmploymentStats(Long classId);
    Map<String, Object> getClassEmploymentStats(Long classId, Integer graduationYear);
    List<Map<String, Object>> getPendingEmployments(Long userId, String role);
    List<Map<String, Object>> getEmploymentHistory(Long userId, String role);
    List<Map<String, Object>> getConversations(Long userId);
    void createConversation(Long userId, Map<String, Object> data);
    Map<String, Object> getConversation(Long id);
    void updateConversation(Long id, Map<String, Object> data);
    void deleteConversation(Long id);
    List<Map<String, Object>> getPermissionRequests(Long userId, String role);
    List<Map<String, Object>> getPermissionHistory(Long userId, String role);
    void auditPermissionRequest(Long id, String action, String remark, Long userId, String role);
    List<Map<String, Object>> getEmploymentReminders(Long userId);
    void sendEmploymentReminder(Long userId, Map<String, Object> data);
    void markReminderAsRead(Long reminderId, Long userId);
    int getUnreadReminderCount(Long userId);
    /** 数据分析员批量通知班主任（针对未就业学生） */
    Map<String, Object> batchNotifyAdvisors(Long userId, Integer graduationYear);
    List<Map<String, Object>> getPendingCompanyAuths(Long userId, String role);
    void auditCompanyAuth(Long authId, String action, String remark, Long userId, String role);
    List<Map<String, Object>> getAgreements(Long userId, String role);
    void signAgreement(Long id, Long userId, String role);
    List<Map<String, Object>> getAgreementStats(Long userId, String role);
    Map<String, Object> getDashboard(Long userId, String role);
    /** 就业明细（分页，SQL 查询，不加载全量数据） */
    Map<String, Object> getEmploymentDetail(Long userId, String role, Long classId, Integer page, Integer size);
    /** 就业明细（全量，用于导出，SQL 查询） */
    Map<String, Object> getEmploymentDetailAll(Long userId, String role, Long classId);
}
