-- =====================================================
-- 数据分析师模块新增表 DDL（完整版）
-- 数据库: employment_db
-- =====================================================

USE employment_db;

-- 1. 推荐历史记录表
CREATE TABLE IF NOT EXISTS `recommend_history` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT COMMENT '推荐用户ID',
  `user_type` VARCHAR(20) COMMENT '用户类型',
  `recommend_type` VARCHAR(20) COMMENT '推荐类型: job-职位, student-学生',
  `target_id` BIGINT COMMENT '推荐对象ID',
  `target_name` VARCHAR(200) COMMENT '推荐对象名称',
  `target_info` VARCHAR(500) COMMENT '推荐对象详细信息',
  `match_score` INT COMMENT '匹配度分数(0-100)',
  `algorithm_type` VARCHAR(50) COMMENT '算法类型: cf/cb/hybrid',
  `feedback` VARCHAR(20) COMMENT '反馈: positive/negative/neutral',
  `feedback_reason` VARCHAR(500) COMMENT '反馈原因',
  `industry` VARCHAR(100) COMMENT '所属行业',
  `city` VARCHAR(50) COMMENT '城市',
  `salary` VARCHAR(50) COMMENT '薪资范围',
  `source` VARCHAR(50) COMMENT '来源',
  `is_viewed` VARCHAR(10) DEFAULT '0' COMMENT '是否已查看: 0-否, 1-是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` BIGINT,
  `update_by` BIGINT,
  `is_deleted` VARCHAR(10) DEFAULT '0',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_recommend_type` (`recommend_type`),
  INDEX `idx_target_id` (`target_id`),
  INDEX `idx_feedback` (`feedback`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐历史记录表';

-- 2. 爬虫任务表
CREATE TABLE IF NOT EXISTS `spider_task` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `task_name` VARCHAR(200) NOT NULL COMMENT '任务名称',
  `source_code` VARCHAR(50) COMMENT '数据源代码: 51job (前程无忧)',
  `source_name` VARCHAR(100) COMMENT '数据源名称',
  `target_url` VARCHAR(500) COMMENT '目标采集URL',
  `data_types` VARCHAR(200) COMMENT '采集数据类型: job/salary/company/industry',
  `depth` INT DEFAULT 1 COMMENT '爬取深度(1-5)',
  `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending/running/success/failed/paused',
  `progress` INT DEFAULT 0 COMMENT '采集进度(0-100)',
  `collected_count` INT DEFAULT 0 COMMENT '已采集数量',
  `success_rate` INT DEFAULT 0 COMMENT '成功率(%)',
  `is_scheduled` VARCHAR(10) DEFAULT '0' COMMENT '是否定时任务',
  `cron_expression` VARCHAR(100) COMMENT 'Cron表达式',
  `last_run_time` VARCHAR(50) COMMENT '最近运行时间',
  `last_error` TEXT COMMENT '最近错误信息',
  `remark` TEXT COMMENT '备注',
  `created_by` BIGINT,
  `update_by` BIGINT,
  `is_deleted` VARCHAR(10) DEFAULT '0',
  INDEX `idx_source_code` (`source_code`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爬虫任务表';

-- 3. 爬虫采集数据表（训练数据集）
CREATE TABLE IF NOT EXISTS `spider_collected_data` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `data_type` VARCHAR(30) COMMENT '数据类型: job/salary/company/industry',
  `source_code` VARCHAR(50) COMMENT '数据源代码',
  `major_name` VARCHAR(100) COMMENT '关联专业名称（用于训练集标注）',
  `industry_keyword` VARCHAR(100) COMMENT '行业关键词',
  `job_name` VARCHAR(200) COMMENT '职位名称',
  `company_name` VARCHAR(200) COMMENT '公司名称',
  `salary` VARCHAR(50) COMMENT '薪资范围',
  `city` VARCHAR(50) COMMENT '工作城市',
  `industry` VARCHAR(100) COMMENT '所属行业',
  `company_scale` VARCHAR(50) COMMENT '公司规模',
  `education` VARCHAR(50) COMMENT '学历要求',
  `experience` VARCHAR(50) COMMENT '经验要求',
  `skills` TEXT COMMENT '技能要求(逗号分隔)',
  `responsibility` TEXT COMMENT '岗位职责',
  `raw_data` LONGTEXT COMMENT '原始HTML数据',
  `collect_time` VARCHAR(50) COMMENT '采集时间',
  `detail_url` TEXT COMMENT '详情页URL（两阶段爬取时使用）',
  `is_synced` VARCHAR(10) DEFAULT '0' COMMENT '是否已同步到训练池: 0-待清洗, 1-已加入训练池',
  `sync_time` VARCHAR(50) COMMENT '同步时间',
  `synced_record_id` BIGINT COMMENT '同步后的主表记录ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` BIGINT,
  `update_by` BIGINT,
  `is_deleted` VARCHAR(10) DEFAULT '0',
  INDEX `idx_data_type` (`data_type`),
  INDEX `idx_source_code` (`source_code`),
  INDEX `idx_major_name` (`major_name`),
  INDEX `idx_is_synced` (`is_synced`),
  INDEX `idx_collect_time` (`collect_time`),
  INDEX `idx_city` (`city`),
  INDEX `idx_industry` (`industry`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爬虫采集数据表（训练数据集）';

-- 4. 爬虫任务执行记录表（抓取进度追踪）
CREATE TABLE IF NOT EXISTS `crawler_job_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `task_id` BIGINT COMMENT '所属任务ID',
  `source_code` VARCHAR(50) COMMENT '数据源代码',
  `major_name` VARCHAR(100) COMMENT '当前爬取专业',
  `industry_keyword` VARCHAR(100) COMMENT '当前行业关键词',
  `search_url` VARCHAR(500) COMMENT '当前抓取的搜索URL',
  `page_num` INT COMMENT '当前页码',
  `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending/running/success/failed',
  `parsed_count` INT DEFAULT 0 COMMENT '该页解析出的职位数',
  `error_message` TEXT COMMENT '错误信息',
  `started_time` VARCHAR(50) COMMENT '该页开始时间',
  `finished_time` VARCHAR(50) COMMENT '该页完成时间',
  `is_complete` VARCHAR(10) DEFAULT '0' COMMENT '是否已完成: 0-未完成, 1-已完成',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` BIGINT,
  `update_by` BIGINT,
  `is_deleted` VARCHAR(10) DEFAULT '0',
  INDEX `idx_task_id` (`task_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_is_complete` (`is_complete`),
  INDEX `idx_major_name` (`major_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爬虫任务执行记录表';

-- 5. 爬虫运行日志表
CREATE TABLE IF NOT EXISTS `spider_log` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `task_id` BIGINT COMMENT '关联任务ID',
  `task_name` VARCHAR(200) COMMENT '任务名称',
  `level` VARCHAR(10) DEFAULT 'INFO' COMMENT '日志级别: INFO/WARN/ERROR',
  `message` TEXT COMMENT '日志消息',
  `source_code` VARCHAR(50) COMMENT '数据源代码',
  `log_time` VARCHAR(50) COMMENT '日志时间',
  `extra_data` TEXT COMMENT '额外数据',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` BIGINT,
  `update_by` BIGINT,
  `is_deleted` VARCHAR(10) DEFAULT '0',
  INDEX `idx_task_id` (`task_id`),
  INDEX `idx_level` (`level`),
  INDEX `idx_log_time` (`log_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爬虫运行日志表';

-- =====================================================
-- 清理旧数据源的任务和数据（仅保留前程无忧）
-- =====================================================
-- 删除非前程无忧的爬虫任务记录
DELETE FROM `spider_task` WHERE `source_code` != '51job';

-- 删除非前程无忧的爬虫采集数据
DELETE FROM `spider_collected_data` WHERE `source_code` != '51job';

-- 删除非前程无忧的爬虫执行记录
DELETE FROM `crawler_job_record` WHERE `source_code` != '51job';

-- =====================================================
-- 初始化爬虫任务示例数据（仅前程无忧）
-- =====================================================
INSERT INTO `spider_task` (`task_name`, `source_code`, `source_name`, `target_url`, `data_types`, `depth`, `is_scheduled`, `status`, `remark`) VALUES
('前程无忧-全方向专业关键词爬取', '51job', '前程无忧', 'https://www.51job.com', 'job', 3, '0', 'pending', '基于学院42个专业的关键词，从前程无忧爬取全国热门职位数据');

-- =====================================================
-- 说明文档
-- =====================================================
-- 训练集构建流程:
-- 1. 数据分析师在爬虫管理页面点击"开启爬虫"
-- 2. 系统自动按学院42个专业+行业关键词生成爬取任务
-- 3. 爬取的数据进入 spider_collected_data (is_synced='0')
-- 4. 点击"数据清洗(ETL)"，系统过滤脏数据（验证码/空职位/无效数据）
-- 5. 清洗后的数据 is_synced='1'，正式进入推荐算法训练池
-- 6. 推荐算法基于训练池中的职位数据，结合学生简历进行智能推荐
-- 7. 爬取的全国就业统计数据显示在就业率分析页面
