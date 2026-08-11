-- 爬虫数据链路打通：添加 is_valid 字段（MySQL 兼容版）
-- Step 1 已执行成功：ALTER TABLE + UPDATE（共 377 行）

-- Step 2: 创建索引（分拆语句，兼容各 MySQL 版本）

-- 先删除旧索引（如果存在）
DROP INDEX idx_spider_data_valid_synced ON spider_collected_data;

-- 再创建索引
CREATE INDEX idx_spider_data_valid_synced ON spider_collected_data(is_valid, is_synced);

-- 同上
DROP INDEX idx_spider_data_major ON spider_collected_data;

CREATE INDEX idx_spider_data_major ON spider_collected_data(major_name, is_valid);
