USE employment_db;

-- 验证清理结果和数据正确性
SELECT '=== 1. 当前各表记录数 ===' as info;

SELECT 'sys_user (总用户数)' as tbl, COUNT(*) as cnt FROM sys_user
UNION ALL SELECT 'sys_user (有班级的学生)', COUNT(*) FROM sys_user WHERE class_id IS NOT NULL
UNION ALL SELECT 'sys_user (批量导入标记)', COUNT(*) FROM sys_user WHERE remark = '批量导入学生'
UNION ALL SELECT 'student_info (总记录数)', COUNT(*) FROM student_info
UNION ALL SELECT 'student_info (有user_id的)', COUNT(*) FROM student_info WHERE user_id IS NOT NULL
UNION ALL SELECT 'job_application (总投递数)', COUNT(*) FROM job_application
UNION ALL SELECT 'job_application (待处理)', COUNT(*) FROM job_application WHERE status = 'pending'
UNION ALL SELECT 'employment_record (总就业数)', COUNT(*) FROM employment_record
UNION ALL SELECT 'student_resume (总简历数)', COUNT(*) FROM student_resume;

SELECT '=== 2. student_info 与 sys_user 数量对比 ===' as info;
SELECT
    (SELECT COUNT(*) FROM sys_user WHERE class_id IS NOT NULL) AS sys_user_student_count,
    (SELECT COUNT(*) FROM student_info) AS student_info_count,
    (SELECT COUNT(*) FROM student_info WHERE user_id IS NOT NULL AND user_id NOT IN (SELECT id FROM sys_user))
        AS orphan_student_info;

SELECT '=== 3. sys_user 中有 student_info 记录的 ===' as info;
SELECT COUNT(*) AS matched FROM sys_user u
JOIN student_info s ON u.id = s.user_id
WHERE u.class_id IS NOT NULL;

SELECT '=== 4. sys_user 中无 student_info 记录的 ===' as info;
SELECT u.id, u.username, u.real_name, u.class_id
FROM sys_user u
WHERE u.class_id IS NOT NULL
  AND u.id NOT IN (SELECT user_id FROM student_info WHERE user_id IS NOT NULL)
LIMIT 10;

SELECT '=== 5. student_info 中无 sys_user 记录的 ===' as info;
SELECT s.id, s.student_no, s.real_name, s.user_id
FROM student_info s
WHERE s.user_id NOT IN (SELECT id FROM sys_user)
LIMIT 10;

SELECT '=== 6. 投递记录对应的学生身份验证 ===' as info;
SELECT
    (SELECT COUNT(*) FROM job_application ja
     JOIN sys_user u ON ja.student_id = u.id
     WHERE u.class_id IS NOT NULL) AS app_with_valid_student,
    (SELECT COUNT(*) FROM job_application ja
     JOIN sys_user u ON ja.student_id = u.id
     WHERE u.class_id IS NULL) AS app_with_admin_user,
    (SELECT COUNT(*) FROM job_application ja
     WHERE ja.student_id NOT IN (SELECT id FROM sys_user)) AS app_with_ghost_student;
