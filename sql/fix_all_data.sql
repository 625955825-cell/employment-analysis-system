-- ============================================
-- 全面数据修复脚本 v1.0
-- 修复项目中的所有数据问题
--
-- 执行方式：
--   mysql -u <用户名> -p employment_db < sql/fix_all_data.sql
--
-- 或者直接在 MySQL 客户端中执行：
--   SOURCE /path/to/project/sql/fix_all_data.sql;
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 修复1：删除admin用户重复的角色记录
-- ============================================
DELETE FROM sys_user_role
WHERE id NOT IN (
    SELECT id FROM (
        SELECT MIN(id) as id FROM sys_user_role GROUP BY user_id, role_id
    ) t
);
SELECT '修复1完成：admin重复角色记录已删除' as result;

-- ============================================
-- 修复2：为800名class_teacher用户设置合理的graduation_year
-- （班主任不需要毕业年份，但设置为当前年份以避免NULL导致的问题）
-- ============================================
UPDATE sys_user u
JOIN sys_user_role ur ON u.id = ur.user_id
JOIN sys_role r ON ur.role_id = r.id
SET u.graduation_year = YEAR(CURDATE())
WHERE r.role_key = 'class_teacher'
  AND u.graduation_year IS NULL;
SELECT CONCAT('修复2完成：更新了 ', ROW_COUNT(), ' 条班主任用户毕业年份') as result;

-- ============================================
-- 修复3：为所有有class_id但无graduation_year的学生设置毕业年份
-- 根据班级grade字段推算：grade + 4 = graduation_year
-- ============================================
UPDATE sys_user u
JOIN sys_class c ON u.class_id = c.id
SET u.graduation_year = CAST(c.grade AS SIGNED) + 4
WHERE u.graduation_year IS NULL
  AND u.class_id IS NOT NULL
  AND c.grade IS NOT NULL;
SELECT CONCAT('修复3完成：更新了 ', ROW_COUNT(), ' 条学生用户毕业年份') as result;

-- ============================================
-- 修复4：为student_info中毕业年份为NULL的记录同步graduation_year
-- ============================================
UPDATE student_info si
JOIN sys_user su ON si.user_id = su.id
SET si.graduation_year = su.graduation_year
WHERE si.graduation_year IS NULL
  AND su.graduation_year IS NOT NULL;
SELECT CONCAT('修复4完成：同步了 ', ROW_COUNT(), ' 条student_info毕业年份') as result;

-- ============================================
-- 修复5：为所有company_info设置统一的company_code
-- 如果没有统一社会信用代码，用自增ID生成模拟代码
-- ============================================
UPDATE company_info
SET company_code = CONCAT('91110000', LPAD(id, 8, '0'), 'X')
WHERE company_code IS NULL OR company_code = '';
SELECT CONCAT('修复5完成：更新了 ', ROW_COUNT(), ' 条企业company_code') as result;

-- ============================================
-- 修复6：根据企业行业分配 dept_id（行业匹配）
-- 企业入驻时选择的学院直接写入 dept_id，此处只处理历史遗留问题
-- ============================================
-- 查看当前企业行业分布（作为参考）
-- SELECT industry, COUNT(*) AS cnt FROM company_info GROUP BY industry ORDER BY cnt DESC;

-- 互联网/IT → 大数据学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'DASHUJU' LIMIT 1)
WHERE (industry LIKE '%互联网%' OR industry LIKE '%IT%' OR industry LIKE '%计算机%' OR industry LIKE '%软件%' OR industry LIKE '%通信%')
  AND dept_id IS NULL;
SELECT CONCAT('修复6完成（互联网/IT）: ', ROW_COUNT(), ' 条') AS result;

-- 机械/装备 → 机械工程学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'JIXIE' LIMIT 1)
WHERE (industry LIKE '%机械%' OR industry LIKE '%装备%' OR industry LIKE '%汽车%')
  AND dept_id IS NULL;
SELECT CONCAT('修复6完成（机械/装备）: ', ROW_COUNT(), ' 条') AS result;

-- 电子/半导体/电气 → 人工智能与电气工程学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'AIRONG' LIMIT 1)
WHERE (industry LIKE '%电子%' OR industry LIKE '%半导体%' OR industry LIKE '%自动化%' OR industry LIKE '%电气%')
  AND dept_id IS NULL;
SELECT CONCAT('修复6完成（电子/电气）: ', ROW_COUNT(), ' 条') AS result;

-- 建筑/房地产 → 土木工程学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'TUMU' LIMIT 1)
WHERE (industry LIKE '%建筑%' OR industry LIKE '%房地产%')
  AND dept_id IS NULL;
SELECT CONCAT('修复6完成（建筑/房地产）: ', ROW_COUNT(), ' 条') AS result;

-- 矿业/能源 → 矿业工程学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'KUANGYE' LIMIT 1)
WHERE (industry LIKE '%矿业%' OR industry LIKE '%能源%' OR industry LIKE '%电力%')
  AND dept_id IS NULL;
SELECT CONCAT('修复6完成（矿业/能源）: ', ROW_COUNT(), ' 条') AS result;

-- 资源/环保 → 资源与环境工程学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'ZIYUAN' LIMIT 1)
WHERE (industry LIKE '%资源%' OR industry LIKE '%环保%' OR industry LIKE '%环境%')
  AND dept_id IS NULL;
SELECT CONCAT('修复6完成（资源/环保）: ', ROW_COUNT(), ' 条') AS result;

-- 航空航天 → 航空航天工程学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'HANGKONG' LIMIT 1)
WHERE (industry LIKE '%航空%' OR industry LIKE '%航天%')
  AND dept_id IS NULL;
SELECT CONCAT('修复6完成（航空航天）: ', ROW_COUNT(), ' 条') AS result;

-- 化工/材料/冶金/制药 → 化学工程学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'HUAXUE' LIMIT 1)
WHERE (industry LIKE '%化工%' OR industry LIKE '%材料%' OR industry LIKE '%冶金%' OR industry LIKE '%制药%')
  AND dept_id IS NULL;
SELECT CONCAT('修复6完成（化工/材料）: ', ROW_COUNT(), ' 条') AS result;

-- ============================================
-- 修复7：为所有student_info设置user_id对应的真实dept_name, major_name
-- ============================================
UPDATE student_info si
JOIN sys_user su ON si.user_id = su.id
JOIN sys_dept sd ON su.dept_id = sd.id
SET si.dept_name = sd.dept_name
WHERE (si.dept_name IS NULL OR si.dept_name = '')
  AND su.dept_id IS NOT NULL;
SELECT CONCAT('修复7a完成：更新了 ', ROW_COUNT(), ' 条dept_name') as result;

UPDATE student_info si
JOIN sys_user su ON si.user_id = su.id
JOIN sys_major sm ON su.major_id = sm.id
SET si.major_name = sm.major_name
WHERE (si.major_name IS NULL OR si.major_name = '')
  AND su.major_id IS NOT NULL;
SELECT CONCAT('修复7b完成：更新了 ', ROW_COUNT(), ' 条major_name') as result;

-- ============================================
-- 修复8：为所有student_resume设置正确的student_id
-- （student_resume.student_id应与sys_user.id一致）
-- ============================================
UPDATE student_resume sr
JOIN student_info si ON sr.student_id = si.id
SET sr.student_id = si.user_id
WHERE sr.student_id != si.user_id;
SELECT CONCAT('修复8完成：更新了 ', ROW_COUNT(), ' 条student_resume关联') as result;

-- ============================================
-- 修复9：为job_application设置正确的student_id（通过user_id关联）
-- ============================================
UPDATE job_application ja
JOIN student_info si ON ja.student_id = si.id
SET ja.student_id = si.user_id
WHERE ja.student_id != si.user_id;
SELECT CONCAT('修复9完成：更新了 ', ROW_COUNT(), ' 条job_application关联') as result;

-- ============================================
-- 修复10：为interview_invitation设置正确的student_id
-- ============================================
UPDATE interview_invitation ii
JOIN student_info si ON ii.student_id = si.id
SET ii.student_id = si.user_id
WHERE ii.student_id != si.user_id;
SELECT CONCAT('修复10完成：更新了 ', ROW_COUNT(), ' 条interview_invitation关联') as result;

-- ============================================
-- 修复11：为offer_letter设置正确的student_id
-- ============================================
UPDATE offer_letter ol
JOIN student_info si ON ol.student_id = si.id
SET ol.student_id = si.user_id
WHERE ol.student_id != si.user_id;
SELECT CONCAT('修复11完成：更新了 ', ROW_COUNT(), ' 条offer_letter关联') as result;

-- ============================================
-- 修复12：为tripartite_agreement设置正确的student_id
-- ============================================
UPDATE tripartite_agreement ta
JOIN student_info si ON ta.student_id = si.id
SET ta.student_id = si.user_id
WHERE ta.student_id != si.user_id;
SELECT CONCAT('修复12完成：更新了 ', ROW_COUNT(), ' 条tripartite_agreement关联') as result;

-- ============================================
-- 修复13：清理sys_user_role中无效的user_id引用
-- ============================================
DELETE FROM sys_user_role
WHERE user_id NOT IN (SELECT id FROM sys_user);
SELECT CONCAT('修复13完成：删除了 ', ROW_COUNT(), ' 条无效sys_user_role记录') as result;

-- ============================================
-- 修复14：确保sys_class的advisor_id与实际分配的教师一致
-- ============================================
UPDATE sys_class sc
JOIN sys_user su ON sc.advisor_id = su.id
SET sc.advisor = su.real_name
WHERE sc.advisor IS NULL OR sc.advisor = ''
  AND su.real_name IS NOT NULL;
SELECT CONCAT('修复14完成：更新了 ', ROW_COUNT(), ' 条班级advisor') as result;

-- ============================================
-- 数据验证：输出关键表的数据统计
-- ============================================
SELECT '=== 数据验证 ===' as info;
SELECT 'sys_user' as tbl, COUNT(*) as total FROM sys_user
UNION ALL SELECT 'sys_user_role', COUNT(*) FROM sys_user_role
UNION ALL SELECT 'student_info', COUNT(*) FROM student_info
UNION ALL SELECT 'student_resume', COUNT(*) FROM student_resume
UNION ALL SELECT 'sys_class', COUNT(*) FROM sys_class
UNION ALL SELECT 'sys_dept', COUNT(*) FROM sys_dept
UNION ALL SELECT 'sys_major', COUNT(*) FROM sys_major
UNION ALL SELECT 'company_info', COUNT(*) FROM company_info
UNION ALL SELECT 'job_position', COUNT(*) FROM job_position
UNION ALL SELECT 'job_application', COUNT(*) FROM job_application
UNION ALL SELECT 'interview_invitation', COUNT(*) FROM interview_invitation
UNION ALL SELECT 'offer_letter', COUNT(*) FROM offer_letter
UNION ALL SELECT 'employment_record', COUNT(*) FROM employment_record
UNION ALL SELECT 'sys_role', COUNT(*) FROM sys_role;

-- 验证毕业生分布
SELECT '=== 学生按毕业年份分布 ===' as info;
SELECT graduation_year, COUNT(*) as student_count
FROM sys_user
WHERE class_id IS NOT NULL AND graduation_year IS NOT NULL
GROUP BY graduation_year
ORDER BY graduation_year;

-- 验证已审核企业
SELECT '=== 企业认证状态 ===' as info;
SELECT auth_status, COUNT(*) as count FROM company_info GROUP BY auth_status;

SET FOREIGN_KEY_CHECKS = 1;
SELECT '=== 所有修复完成 ===' as result;
