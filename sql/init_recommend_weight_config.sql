-- 推荐权重配置表
CREATE TABLE IF NOT EXISTS `recommend_weight_config` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `weight_key` VARCHAR(50) NOT NULL UNIQUE COMMENT '权重唯一标识',
  `weight_name` VARCHAR(100) NOT NULL COMMENT '权重名称',
  `weight_value` INT NOT NULL COMMENT '权重分值（满分）',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '满分条件描述',
  `enabled` VARCHAR(5) NOT NULL DEFAULT '1' COMMENT '是否启用：0-禁用 1-启用',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  UNIQUE KEY `uk_weight_key` (`weight_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐算法权重配置表';

-- 初始化五条权重数据
INSERT INTO `recommend_weight_config` (`weight_key`, `weight_name`, `weight_value`, `description`, `enabled`) VALUES
('major', '专业相关性', 25, '职位专业要求/行业关键词与学生专业名完全匹配', '1'),
('city', '城市偏好', 20, '职位城市与学生期望城市或历史投递城市一致', '1'),
('salary', '薪资匹配度', 20, '职位薪资落在学生简历填写的期望薪资区间内', '1'),
('skill', '技能关键词', 20, '职位描述/技能要求中匹配学生简历的技能/项目/证书关键词数量', '1'),
('education', '学历匹配', 15, '职位学历要求低于或等于学生最高学历', '1');
