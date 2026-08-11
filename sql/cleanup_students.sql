-- ==============================================================
-- 学生数据清理脚本
-- 用于删除所有批量导入的学生及相关数据
-- ==============================================================

USE employment_db;
SET FOREIGN_KEY_CHECKS = 0;

-- ==============================================================
-- Step 1: 获取需要删除的学生ID列表
-- ==============================================================
DROP TEMPORARY TABLE IF EXISTS tmp_student_ids;
CREATE TEMPORARY TABLE tmp_student_ids AS
SELECT id AS uid FROM sys_user WHERE remark = '批量导入学生';

SELECT CONCAT('找到 ', COUNT(*), ' 个学生账号待删除') AS info FROM tmp_student_ids;

-- ==============================================================
-- Step 2: 删除关联数据（按依赖关系，从叶子到根）
-- ==============================================================

-- conversation_record（谈心谈话）
DELETE cr FROM conversation_record cr
INNER JOIN tmp_student_ids t ON cr.student_id = t.uid;
SELECT CONCAT('已删除 conversation_record: ', ROW_COUNT(), ' 条') AS result;

-- employment_attachment（就业附件）
DELETE ea FROM employment_attachment ea
INNER JOIN employment_record er ON ea.employment_id = er.id
INNER JOIN tmp_student_ids t ON er.student_id = t.uid;
SELECT CONCAT('已删除 employment_attachment: ', ROW_COUNT(), ' 条') AS result;

-- tripartite_agreement（三方协议）
DELETE ta FROM tripartite_agreement ta
INNER JOIN tmp_student_ids t ON ta.student_id = t.uid;
SELECT CONCAT('已删除 tripartite_agreement: ', ROW_COUNT(), ' 条') AS result;

-- offer_letter（录用通知）
DELETE ol FROM offer_letter ol
INNER JOIN tmp_student_ids t ON ol.student_id = t.uid;
SELECT CONCAT('已删除 offer_letter: ', ROW_COUNT(), ' 条') AS result;

-- interview_record（面试记录）→ 需要先找到invitation_id
DELETE ir FROM interview_record ir
INNER JOIN interview_invitation ii ON ir.invitation_id = ii.id
INNER JOIN tmp_student_ids t ON ii.student_id = t.uid;
SELECT CONCAT('已删除 interview_record: ', ROW_COUNT(), ' 条') AS result;

-- interview_invitation（面试邀请）
DELETE ii FROM interview_invitation ii
INNER JOIN tmp_student_ids t ON ii.student_id = t.uid;
SELECT CONCAT('已删除 interview_invitation: ', ROW_COUNT(), ' 条') AS result;

-- job_application（投递申请）
DELETE ja FROM job_application ja
INNER JOIN tmp_student_ids t ON ja.student_id = t.uid;
SELECT CONCAT('已删除 job_application: ', ROW_COUNT(), ' 条') AS result;

-- employment_record（就业记录）
DELETE er FROM employment_record er
INNER JOIN tmp_student_ids t ON er.student_id = t.uid;
SELECT CONCAT('已删除 employment_record: ', ROW_COUNT(), ' 条') AS result;

-- student_resume（简历）
DELETE sr FROM student_resume sr
INNER JOIN tmp_student_ids t ON sr.student_id = t.uid;
SELECT CONCAT('已删除 student_resume: ', ROW_COUNT(), ' 条') AS result;

-- student_info（学生信息）
DELETE si FROM student_info si
INNER JOIN tmp_student_ids t ON si.user_id = t.uid;
SELECT CONCAT('已删除 student_info: ', ROW_COUNT(), ' 条') AS result;

-- sys_user_role（用户角色）
DELETE ur FROM sys_user_role ur
INNER JOIN tmp_student_ids t ON ur.user_id = t.uid;
SELECT CONCAT('已删除 sys_user_role: ', ROW_COUNT(), ' 条') AS result;

-- sys_user（用户账号，最后删）
DELETE FROM sys_user WHERE remark = '批量导入学生';
SELECT CONCAT('已删除 sys_user: ', ROW_COUNT(), ' 条') AS result;

-- ==============================================================
-- Step 3: 重置班级学生数
-- ==============================================================
UPDATE sys_class SET student_count = 0;
SELECT '已重置 sys_class.student_count 为 0' AS result;

-- ==============================================================
-- Step 4: 验证结果
-- ==============================================================
SELECT '--- 清理后验证 ---' AS info;
SELECT 'sys_user(批量)' AS 表, COUNT(*) AS 数量 FROM sys_user WHERE remark = '批量导入学生'
UNION ALL SELECT 'sys_user(总计)', COUNT(*) FROM sys_user
UNION ALL SELECT 'student_info', COUNT(*) FROM student_info
UNION ALL SELECT 'student_resume', COUNT(*) FROM student_resume
UNION ALL SELECT 'employment_record', COUNT(*) FROM employment_record
UNION ALL SELECT 'conversation_record', COUNT(*) FROM conversation_record
UNION ALL SELECT 'job_application', COUNT(*) FROM job_application
UNION ALL SELECT 'offer_letter', COUNT(*) FROM offer_letter
UNION ALL SELECT 'interview_invitation', COUNT(*) FROM interview_invitation
UNION ALL SELECT 'tripartite_agreement', COUNT(*) FROM tripartite_agreement
UNION ALL SELECT 'sys_class.student_count>0', COUNT(*) FROM sys_class WHERE student_count > 0;

DROP TEMPORARY TABLE IF EXISTS tmp_student_ids;

SET FOREIGN_KEY_CHECKS = 1;

SELECT '清理完成！' AS result;
