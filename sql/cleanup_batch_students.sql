-- ============================================
-- 清理批量导入的学生数据（配合 generate_students_fixed.py 使用）
--
-- 执行方式：
--   mysql -u <用户名> -p employment_db < sql/cleanup_batch_students.sql
--
-- 或者直接在 MySQL 客户端中执行：
--   SOURCE /path/to/project/sql/cleanup_batch_students.sql;
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 先查询要删除的用户ID（避免误删）
-- ============================================
SELECT '=== 待清理学生账号 ===' as info;
SELECT COUNT(*) as student_count FROM sys_user WHERE remark = '批量导入学生';
SELECT id, username, real_name, class_id FROM sys_user WHERE remark = '批量导入学生' LIMIT 10;

-- ============================================
-- 删除顺序：先删子表，再删主表
-- 按外键依赖从底往上删除
-- ============================================

-- 1. sys_log（记录日志，可能关联到学生用户）
DELETE FROM sys_log WHERE user_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生');
SELECT CONCAT('1. sys_log: 删除了 ', ROW_COUNT(), ' 条记录') as result;

-- 2. employment_attachment（就业材料附件）
DELETE FROM employment_attachment
WHERE employment_id IN (
    SELECT id FROM employment_record
    WHERE student_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生')
);
SELECT CONCAT('2. employment_attachment: 删除了 ', ROW_COUNT(), ' 条记录') as result;

-- 3. tripartite_agreement（三方协议）
DELETE FROM tripartite_agreement
WHERE student_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生');
SELECT CONCAT('3. tripartite_agreement: 删除了 ', ROW_COUNT(), ' 条记录') as result;

-- 4. conversation_record（谈心谈话记录）
DELETE FROM conversation_record
WHERE student_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生');
SELECT CONCAT('4. conversation_record: 删除了 ', ROW_COUNT(), ' 条记录') as result;

-- 5. offer_letter（录用通知）
DELETE FROM offer_letter
WHERE student_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生');
SELECT CONCAT('5. offer_letter: 删除了 ', ROW_COUNT(), ' 条记录') as result;

-- 6. interview_invitation（面试邀约）
DELETE FROM interview_invitation
WHERE student_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生');
SELECT CONCAT('6. interview_invitation: 删除了 ', ROW_COUNT(), ' 条记录') as result;

-- 7. job_application（投递记录）
DELETE FROM job_application
WHERE student_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生');
SELECT CONCAT('7. job_application: 删除了 ', ROW_COUNT(), ' 条记录') as result;

-- 8. student_favorite（学生收藏职位，不存在则跳过）
-- 注意：需要先确认表中是否存在该表，以下为安全写法
-- DELETE FROM student_favorite
-- WHERE student_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生');
-- 本脚本已禁用此语句，因为 student_favorite 表可能尚未创建
SELECT '8. student_favorite: 跳过（表不存在或无数据）' as result;

-- 9. notification（学生通知）
DELETE FROM notification
WHERE user_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生');
SELECT CONCAT('9. notification: 删除了 ', ROW_COUNT(), ' 条记录') as result;

-- 10. data_permission（数据权限申请，不存在则跳过）
-- DELETE FROM data_permission
-- WHERE student_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生');
-- 本脚本已禁用此语句，因为 data_permission 表可能尚未创建
SELECT '10. data_permission: 跳过（表不存在或无数据）' as result;

-- 11. student_resume（学生简历）
DELETE FROM student_resume
WHERE student_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生');
SELECT CONCAT('11. student_resume: 删除了 ', ROW_COUNT(), ' 条记录') as result;

-- 12. employment_record（就业记录）
DELETE FROM employment_record
WHERE student_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生');
SELECT CONCAT('12. employment_record: 删除了 ', ROW_COUNT(), ' 条记录') as result;

-- 13. student_info（学生信息）
DELETE FROM student_info
WHERE user_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生');
SELECT CONCAT('13. student_info: 删除了 ', ROW_COUNT(), ' 条记录') as result;

-- 14. sys_user_role（用户角色关联）
DELETE FROM sys_user_role
WHERE user_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生');
SELECT CONCAT('14. sys_user_role: 删除了 ', ROW_COUNT(), ' 条记录') as result;

-- 15. sys_user（用户主表，最后删除）
DELETE FROM sys_user WHERE remark = '批量导入学生';
SELECT CONCAT('15. sys_user: 删除了 ', ROW_COUNT(), ' 条记录') as result;

-- ============================================
-- 验证清理结果
-- ============================================
SELECT '=== 清理后验证 ===' as info;

SELECT 'sys_user' as tbl, COUNT(*) as remaining FROM sys_user WHERE remark = '批量导入学生'
UNION ALL SELECT 'student_info', COUNT(*) FROM student_info
WHERE user_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生')
UNION ALL SELECT 'student_resume', COUNT(*) FROM student_resume
WHERE student_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生')
UNION ALL SELECT 'job_application', COUNT(*) FROM job_application
WHERE student_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生')
UNION ALL SELECT 'employment_record', COUNT(*) FROM employment_record
WHERE student_id IN (SELECT id FROM sys_user WHERE remark = '批量导入学生');

SELECT '=== 当前学生总数 ===' as info;
SELECT COUNT(*) as total_students FROM sys_user WHERE class_id IS NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;
SELECT '=== 清理完成 ===' as result;
