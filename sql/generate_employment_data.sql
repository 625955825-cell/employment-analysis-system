-- ============================================
-- 为2022届已毕业学生生成就业数据
-- employment_record 表当前为0条，必须修复
--
-- 就业类型分布：
-- 签订劳动合同 ~62%，三方协议 ~15%，继续深造 ~8%，
-- 自主创业 ~4%，出国出境 ~2%，应征入伍 ~2%，
-- 暂未就业 ~5%，自由职业 ~1%，其他 ~1%
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 首先检查当前状态
SELECT '当前employment_record数量' as info, COUNT(*) as cnt FROM employment_record;

-- 获取2022届有student角色的学生（通过user_id关联student_info）
SELECT '2022届学生数量' as info, COUNT(*) as cnt
FROM sys_user u
WHERE u.graduation_year = 2022
  AND u.class_id IS NOT NULL;

-- 获取所有企业（用于就业数据）
SELECT '企业总数' as info, COUNT(*) as cnt FROM company_info WHERE auth_status = 'approved';

-- ============================================
-- 生成就业记录（2022届630名学生）
-- ============================================

-- 先删除已有的就业记录（如果有的话）
DELETE FROM employment_record;
SELECT '已清空employment_record表' as result;

-- 为2022届学生生成就业记录
-- 使用存储过程来处理复杂的逻辑
DROP PROCEDURE IF EXISTS generate_employment_for_2022;

DELIMITER //

CREATE PROCEDURE generate_employment_for_2022()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE stu_id BIGINT;
    DECLARE stu_dept_id BIGINT;
    DECLARE stu_major_id BIGINT;
    DECLARE stu_industry VARCHAR(100);
    DECLARE emp_type VARCHAR(50);
    DECLARE emp_r INT;
    DECLARE comp_id BIGINT;
    DECLARE comp_name VARCHAR(200);
    DECLARE comp_code VARCHAR(50);
    DECLARE comp_scale VARCHAR(50);
    DECLARE comp_industry VARCHAR(100);
    DECLARE comp_city VARCHAR(50);
    DECLARE comp_province VARCHAR(50);
    DECLARE comp_cursor CURSOR FOR
        SELECT id, company_name, company_code, scale, industry, city, province
        FROM company_info WHERE auth_status = 'approved' AND status = '0';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- 临时表：学生就业类型分配
    DECLARE cur_stu CURSOR FOR
        SELECT u.id, u.dept_id, u.major_id
        FROM sys_user u
        JOIN sys_user_role ur ON u.id = ur.user_id
        JOIN sys_role r ON ur.role_id = r.id
        WHERE u.graduation_year = 2022
          AND u.class_id IS NOT NULL
          AND r.role_key = 'student';

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN comp_cursor;

    -- 获取第一个企业作为默认
    FETCH comp_cursor INTO comp_id, comp_name, comp_code, comp_scale, comp_industry, comp_city, comp_province;

    -- 遍历2022届学生，生成就业记录
    BEGIN
        DECLARE stu_cursor CURSOR FOR
            SELECT u.id, u.dept_id, u.major_id
            FROM sys_user u
            JOIN sys_user_role ur ON u.id = ur.user_id
            JOIN sys_role r ON ur.role_id = r.id
            WHERE u.graduation_year = 2022
              AND u.class_id IS NOT NULL
              AND r.role_key = 'student';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        DECLARE stu_done INT DEFAULT FALSE;

        OPEN stu_cursor;
        stu_loop: LOOP
            FETCH stu_cursor INTO stu_id, stu_dept_id, stu_major_id;
            IF stu_done THEN
                LEAVE stu_loop;
            END IF;

            -- 根据学院推断行业
            SELECT d.dept_code INTO @dept_code FROM sys_dept d WHERE d.id = stu_dept_id;
            SET stu_industry = CASE
                WHEN @dept_code = 'JIXIE' OR @dept_code = 'AIRONG' THEN '机械/装备'
                WHEN @dept_code = 'DASHUJU' THEN '互联网'
                WHEN @dept_code = 'TUMU' THEN '建筑/房地产'
                WHEN @dept_code = 'ZIYUAN' OR @dept_code = 'KUANGYE' THEN '矿业/能源'
                WHEN @dept_code = 'HANGKONG' THEN '航空航天'
                WHEN @dept_code = 'CAILIAO' THEN '化工/材料'
                WHEN @dept_code = 'HUAXUE' THEN '化工/材料'
                ELSE '互联网'
            END;

            -- 随机分配就业类型
            SET emp_r = FLOOR(1 + RAND() * 100);
            SET emp_type = CASE
                WHEN emp_r <= 62 THEN '签订劳动合同'
                WHEN emp_r <= 77 THEN '签订三方协议'
                WHEN emp_r <= 85 THEN '继续深造'
                WHEN emp_r <= 89 THEN '自主创业'
                WHEN emp_r <= 91 THEN '出国出境'
                WHEN emp_r <= 93 THEN '应征入伍'
                WHEN emp_r <= 98 THEN '暂未就业'
                WHEN emp_r <= 99 THEN '自由职业'
                ELSE '其他'
            END;

            -- 根据行业选择企业
            SELECT id, company_name, company_code, scale, industry, city, province
            INTO comp_id, comp_name, comp_code, comp_scale, comp_industry, comp_city, comp_province
            FROM company_info
            WHERE auth_status = 'approved' AND status = '0'
              AND (industry LIKE CONCAT('%', stu_industry, '%') OR industry LIKE '%互联网%')
            ORDER BY RAND()
            LIMIT 1;

            -- 如果没找到，用默认企业
            IF comp_id IS NULL THEN
                SELECT id, company_name, company_code, scale, industry, city, province
                INTO comp_id, comp_name, comp_code, comp_scale, comp_industry, comp_city, comp_province
                FROM company_info
                WHERE auth_status = 'approved' AND status = '0'
                ORDER BY RAND()
                LIMIT 1;
            END IF;

            -- 插入就业记录
            INSERT INTO employment_record (
                student_id, employment_type, company_name, company_code,
                company_scale, company_industry, position_name, position_category,
                work_city, work_province, salary, is_three_party_signed,
                contract_start_date, contract_end_date, probation_salary,
                audit_status, audit_time, remark
            ) VALUES (
                stu_id,
                emp_type,
                IF(emp_type IN ('继续深造', '自主创业', '出国出境', '应征入伍', '暂未就业', '其他'),
                IF(emp_type NOT IN ('继续深造', '自主创业', '出国出境', '应征入伍', '暂未就业', '其他'), comp_code),
                IF(emp_type NOT IN ('继续深蓝', '自主创业', '出国出境', '应征入伍', '暂未就业', '其他'), comp_scale),
                IF(emp_type NOT IN ('继续深造', '自主创业', '出国出境', '应征入伍', '暂未就业', '其他'), comp_industry),
                IF(emp_type IN ('签订劳动合同', '签订三方协议'),
                   ELT(FLOOR(1 + RAND() * 5), '技术研发', '软件工程', '数据分析师', '产品经理', '运维工程师'),
                   IF(emp_type = '继续深造', '继续深造', IF(emp_type = '应征入伍', '义务兵', '其他'))),
                IF(emp_type IN ('签订劳动合同', '签订三方协议'), '技术',
                   IF(emp_type = '继续深造', '深造', IF(emp_type = '应征入伍', '军', '其他'))),
                IF(emp_type IN ('继续深造', '出国出境', '应征入伍', '暂未就业', '其他'), '未知', comp_city),
                IF(emp_type = '继续深造', '湖北省',
                   IF(emp_type = '出国出境', '境外',
                      IF(emp_type IN ('应征入伍', '暂未就业', '其他'), '湖北省',
                         IF(comp_province IS NULL, '湖北省', comp_province)))),
                IF(emp_type IN ('签订劳动合同', '签订三方协议'),
                   CONCAT(FLOOR(5000 + RAND() * 10000), '元/月'),
                   '待定'),
                IF(emp_type = '签订三方协议', '1', '0'),
                IF(emp_type = '签订三方协议', '2022-07-01', NULL),
                IF(emp_type = '签订三方协议', '2025-06-30', NULL),
                IF(emp_type = '签订三方协议',
                   CONCAT(FLOOR(4000 + RAND() * 8000), '元/月'),
                   NULL),
                'approved',
                '2022-06-15 10:00:00',
                '系统自动生成的模拟就业数据'
            );

            SET comp_id = NULL;
        END LOOP;
        CLOSE stu_cursor;
    END;

    CLOSE comp_cursor;
END//

DELIMITER ;

-- 执行存储过程
CALL generate_employment_for_2022();

-- 验证结果
SELECT '生成后的employment_record数量' as info, COUNT(*) as cnt FROM employment_record;

-- 按就业类型统计
SELECT employment_type, COUNT(*) as count,
       ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM employment_record), 2) as percentage
FROM employment_record
GROUP BY employment_type
ORDER BY count DESC;

-- 删除存储过程
DROP PROCEDURE IF EXISTS generate_employment_for_2022;

SET FOREIGN_KEY_CHECKS = 1;

SELECT '=== 就业数据生成完成 ===' as result;
