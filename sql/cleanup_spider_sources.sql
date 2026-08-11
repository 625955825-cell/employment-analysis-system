-- =====================================================
-- 数据库清理脚本：删除所有非前程无忧的爬虫历史数据
-- 用途：彻底清理数据库中残留的拉勾网、智联招聘、BOSS直聘等历史爬虫数据
-- 适用场景：代码已迁移至仅支持前程无忧后，执行此脚本清理旧数据
-- 数据库: employment_db
-- =====================================================

USE employment_db;

-- 1. 删除非前程无忧的爬虫任务记录
-- （包括拉勾网、智联招聘、BOSS直聘、猎聘网、看准网的历史任务）
DELETE FROM `spider_task` WHERE `source_code` != '51job';
SELECT CONCAT('已删除 spider_task 表中非前程无忧记录，当前剩余记录数: ', COUNT(*)) AS result FROM `spider_task`;

-- 2. 删除非前程无忧的爬虫采集数据
-- （训练池中的拉勾网等历史脏数据）
DELETE FROM `spider_collected_data` WHERE `source_code` != '51job';
SELECT CONCAT('已删除 spider_collected_data 表中非前程无忧记录，当前剩余记录数: ', COUNT(*)) AS result FROM `spider_collected_data`;

-- 3. 删除非前程无忧的爬虫执行记录
-- （crawler_job_record 表中残留的拉勾网等任务执行轨迹）
DELETE FROM `crawler_job_record` WHERE `source_code` != '51job';
SELECT CONCAT('已删除 crawler_job_record 表中非前程无忧记录，当前剩余记录数: ', COUNT(*)) AS result FROM `crawler_job_record`;

-- 4. 重置所有爬虫任务状态为 pending（清理历史运行状态）
UPDATE `spider_task` SET `status` = 'pending', `progress` = 0, `collected_count` = 0, `last_error` = NULL;
SELECT '已将所有前程无忧任务状态重置为 pending' AS result;

-- 5. 验证清理结果
SELECT '--- 清理验证 ---' AS '';
SELECT 'spider_task 中的数据源分布:' AS '';
SELECT `source_code`, COUNT(*) AS count FROM `spider_task` GROUP BY `source_code`;
SELECT 'spider_collected_data 中的数据源分布:' AS '';
SELECT `source_code`, COUNT(*) AS count FROM `spider_collected_data` GROUP BY `source_code`;
SELECT 'crawler_job_record 中的数据源分布:' AS '';
SELECT `source_code`, COUNT(*) AS count FROM `crawler_job_record` GROUP BY `source_code`;

-- =====================================================
-- 执行说明：
-- 1. 使用 Navicat / MySQL Workbench / 命令行连接数据库
-- 2. 选择 employment_db 数据库
-- 3. 选中并执行本脚本
-- 4. 执行完成后，重新启动 Spring Boot 后端服务
-- 5. 在前端刷新爬虫管理页面，数据源将仅显示"前程无忧"
-- =====================================================
