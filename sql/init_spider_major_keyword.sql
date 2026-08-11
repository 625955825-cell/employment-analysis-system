-- 爬虫专业关键词配置表
CREATE TABLE IF NOT EXISTS `spider_major_keyword` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `major_name` VARCHAR(100) NOT NULL COMMENT '专业名称',
  `keyword` VARCHAR(100) NOT NULL COMMENT '关键词',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  UNIQUE KEY `uk_major_keyword` (`major_name`, `keyword`),
  KEY `idx_major_name` (`major_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爬虫专业关键词配置表';
