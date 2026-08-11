-- 修复 spider_collected_data 表 raw_data 字段长度不足的问题
-- 该字段存储爬虫采集的原始HTML数据，单页HTML可能超过65535字节
ALTER TABLE spider_collected_data
MODIFY COLUMN raw_data LONGTEXT COMMENT '原始HTML数据';

-- 将 detail_url 字段改为 TEXT（URL 带 JSON 参数可能超过 500 字符）
ALTER TABLE spider_collected_data
MODIFY COLUMN detail_url TEXT COMMENT '详情页URL（两阶段爬取时使用）';
