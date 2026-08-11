-- ============================================
-- 企业院系绑定修复脚本
-- 修复 fix_all_data.sql 中错误地将所有企业 dept_id 设为 49（环资）的问题
--
-- 执行方式：
--   mysqlsh --sql --uri root:123456@localhost:3306 --database=employment_db
--   然后执行: SOURCE /path/to/project/sql/fix_company_dept.sql;
-- ============================================

USE employment_db;

-- ============================================
-- 重置所有企业的 dept_id（将之前错误设置的 dept_id 全部清空）
-- ============================================
UPDATE company_info SET dept_id = NULL WHERE dept_id IS NOT NULL;
SELECT CONCAT('重置完成：所有企业 dept_id 已清空，影响行数=', ROW_COUNT()) AS result;

-- ============================================
-- 根据行业分配院系（完整匹配）
-- ============================================

-- 互联网/IT/软件/数据/计算机/通信 → 大数据学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'DASHUJU' LIMIT 1)
WHERE industry LIKE '%互联网%' OR industry LIKE '%IT%' OR industry LIKE '%计算机%'
   OR industry LIKE '%软件%' OR industry LIKE '%通信%' OR industry LIKE '%数据%'
   OR industry LIKE '%信息%'
  AND dept_id IS NULL;
SELECT CONCAT('互联网/IT行业: ', ROW_COUNT(), ' 家企业') AS result;

-- 机械/装备/制造/汽车/激光/橡胶/焊接/装备 → 机械工程学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'JIXIE' LIMIT 1)
WHERE (industry LIKE '%机械%' OR industry LIKE '%装备%' OR industry LIKE '%制造%'
   OR industry LIKE '%汽车%' OR industry LIKE '%激光%' OR industry LIKE '%橡胶%')
  AND dept_id IS NULL;
SELECT CONCAT('机械/装备行业: ', ROW_COUNT(), ' 家企业') AS result;

-- 电子/半导体/电气/自动化/工业自动化/军工/传感/控制 → 人工智能与电气工程学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'AIRONG' LIMIT 1)
WHERE (industry LIKE '%电子%' OR industry LIKE '%半导体%' OR industry LIKE '%电气%'
   OR industry LIKE '%自动化%' OR industry LIKE '%军工%' OR industry LIKE '%传感%'
   OR industry LIKE '%控制%')
  AND dept_id IS NULL;
SELECT CONCAT('电子/电气行业: ', ROW_COUNT(), ' 家企业') AS result;

-- 建筑/施工/设计/监理/交通/水利/铁路/工程/勘探/勘察 → 土木工程学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'TUMU' LIMIT 1)
WHERE (industry LIKE '%建筑%' OR industry LIKE '%施工%' OR industry LIKE '%设计%'
   OR industry LIKE '%监理%' OR industry LIKE '%交通%' OR industry LIKE '%水利%'
   OR industry LIKE '%铁路%' OR industry LIKE '%工程%' OR industry LIKE '%勘探%'
   OR industry LIKE '%勘察%')
  AND dept_id IS NULL;
SELECT CONCAT('建筑/工程行业: ', ROW_COUNT(), ' 家企业') AS result;

-- 矿业/金属/能源/电力/燃气/安全 → 矿业工程学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'KUANGYE' LIMIT 1)
WHERE (industry LIKE '%矿业%' OR industry LIKE '%金属%' OR industry LIKE '%能源%'
   OR industry LIKE '%电力%' OR industry LIKE '%燃气%' OR industry LIKE '%安全%'
   OR industry LIKE '%安全咨询%')
  AND dept_id IS NULL;
SELECT CONCAT('矿业/能源行业: ', ROW_COUNT(), ' 家企业') AS result;

-- 环保/资源/地质/勘查/节能环保/环保工程/环境 → 资源与环境工程学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'ZIYUAN' LIMIT 1)
WHERE (industry LIKE '%环保%' OR industry LIKE '%资源%' OR industry LIKE '%地质%'
   OR industry LIKE '%勘查%' OR industry LIKE '%节能%' OR industry LIKE '%环境%')
  AND dept_id IS NULL;
SELECT CONCAT('环保/资源行业: ', ROW_COUNT(), ' 家企业') AS result;

-- 航空/航天/航空发动机/无人机/商飞/科工 → 航空航天工程学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'HANGKONG' LIMIT 1)
WHERE (industry LIKE '%航空%' OR industry LIKE '%航天%' OR industry LIKE '%无人机%')
  AND dept_id IS NULL;
SELECT CONCAT('航空航天行业: ', ROW_COUNT(), ' 家企业') AS result;

-- 材料/冶金/焊接/新材料/金属加工 → 材料科学与冶金工程学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'CAILIAO' LIMIT 1)
WHERE (industry LIKE '%材料%' OR industry LIKE '%冶金%' OR industry LIKE '%焊接%'
   OR industry LIKE '%金属%')
  AND dept_id IS NULL;
SELECT CONCAT('材料/冶金行业: ', ROW_COUNT(), ' 家企业') AS result;

-- 化工/制药/医药/生物/酒/石化/日化/能源化工 → 化学工程学院
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'HUAXUE' LIMIT 1)
WHERE (industry LIKE '%化工%' OR industry LIKE '%制药%' OR industry LIKE '%医药%'
   OR industry LIKE '%生物%' OR industry LIKE '%酒%' OR industry LIKE '%石化%'
   OR industry LIKE '%日化%' OR industry LIKE '%矿产%')
  AND dept_id IS NULL;
SELECT CONCAT('化工/制药行业: ', ROW_COUNT(), ' 家企业') AS result;

-- 教育/培训/体育/旅游/文化/传媒/农业 → 资源与环境工程学院（兜底）
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'ZIYUAN' LIMIT 1)
WHERE (industry LIKE '%教育%' OR industry LIKE '%培训%' OR industry LIKE '%体育%'
   OR industry LIKE '%旅游%' OR industry LIKE '%文化%' OR industry LIKE '%传媒%'
   OR industry LIKE '%农业%')
  AND dept_id IS NULL;
SELECT CONCAT('教育/文体行业: ', ROW_COUNT(), ' 家企业') AS result;

-- 金融/银行/保险/投资/咨询/法律/审计 → 大数据学院（兜底）
UPDATE company_info
SET dept_id = (SELECT id FROM sys_dept WHERE dept_code = 'DASHUJU' LIMIT 1)
WHERE (industry LIKE '%金融%' OR industry LIKE '%银行%' OR industry LIKE '%保险%'
   OR industry LIKE '%投资%' OR industry LIKE '%咨询%' OR industry LIKE '%法律%'
   OR industry LIKE '%审计%')
  AND dept_id IS NULL;
SELECT CONCAT('金融/咨询行业: ', ROW_COUNT(), ' 家企业') AS result;

-- ============================================
-- 修复后验证
-- ============================================
SELECT '=== 修复后企业 dept_id 分布 ===' AS info;
SELECT c.dept_id, d.dept_name, COUNT(*) AS cnt
FROM company_info c
LEFT JOIN sys_dept d ON c.dept_id = d.id
GROUP BY c.dept_id, d.dept_name
ORDER BY cnt DESC;

SELECT '=== 各行业企业分配结果 ===' AS info;
SELECT
    d.dept_name AS 院系,
    c.industry AS 行业,
    COUNT(*) AS 企业数
FROM company_info c
LEFT JOIN sys_dept d ON c.dept_id = d.id
GROUP BY d.dept_name, c.industry
ORDER BY d.dept_name, COUNT(*) DESC;

SELECT '=== 修复完成 ===' AS result;
