-- ============================================
-- 高校就业信息管理系统 - 完整数据库部署脚本
-- 数据库名: employment_db
--
-- 使用说明：
--   1. 直接执行本文件即可完成所有表结构创建和初始数据导入
--   2. 所有表均为 DROP TABLE IF EXISTS 方式，支持幂等重复执行
--   3. 执行顺序：表结构 -> 基础数据 -> 测试账号
--
-- 版本历史：
--   - init.sql      : 基础表结构 + 核心测试账号
--   - fix_work_province.sql : 就业记录省份数据修复
--   - quick_login_test.sql  : 快速登录闭环测试数据
--   - v2.0          : 新增 notice_read_record 表（公告已读追踪）
--
-- ============================================

CREATE DATABASE IF NOT EXISTS `employment_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `employment_db`;

-- ============================================
-- 1. sys_user 用户表
-- ============================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(200) NOT NULL COMMENT '密码',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `avatar` VARCHAR(500) COMMENT '头像',
    `email` VARCHAR(100) COMMENT '邮箱',
    `phone` VARCHAR(20) COMMENT '手机号',
    `gender` VARCHAR(10) COMMENT '性别',
    `id_card` VARCHAR(20) COMMENT '身份证号',
    `status` VARCHAR(10) DEFAULT '0' COMMENT '状态 0正常 1禁用',
    `dept_id` BIGINT COMMENT '院系ID',
    `major_id` BIGINT COMMENT '专业ID',
    `class_name` VARCHAR(50) COMMENT '班级名称',
    `class_id` BIGINT COMMENT '班级ID（关联sys_class）',
    `student_no` VARCHAR(30) COMMENT '学号',
    `graduation_year` INT COMMENT '毕业年份',
    `last_login_ip` VARCHAR(50) COMMENT '最后登录IP',
    `last_login_time` VARCHAR(50) COMMENT '最后登录时间',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ============================================
-- 2. sys_role 角色表
-- ============================================
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `role_key` VARCHAR(50) NOT NULL COMMENT '角色标识',
    `role_sort` INT COMMENT '显示顺序',
    `status` VARCHAR(10) DEFAULT '0' COMMENT '状态 0正常 1禁用',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

INSERT INTO `sys_role` (`role_name`, `role_key`, `role_sort`, `status`, `remark`) VALUES
    ('学生', 'student', 1, '0', '学生角色'),
    ('班级老师', 'class_teacher', 2, '0', '班级老师角色'),
    ('院级老师', 'dept_teacher', 3, '0', '院级老师角色'),
    ('校级管理员', 'admin', 4, '0', '校级管理员角色'),
    ('用人单位', 'company', 5, '0', '用人单位角色'),
    ('数据分析工程师', 'employment_staff', 6, '0', '数据分析工程师角色');

-- ============================================
-- 3. sys_user_role 用户角色关联表
-- ============================================
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ============================================
-- 4. sys_permission 权限表
-- ============================================
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
    `permission_key` VARCHAR(100) NOT NULL COMMENT '权限标识',
    `permission_type` VARCHAR(20) COMMENT '权限类型 menu button',
    `parent_id` BIGINT COMMENT '父权限ID',
    `path` VARCHAR(200) COMMENT '路由路径',
    `icon` VARCHAR(100) COMMENT '图标',
    `component` VARCHAR(200) COMMENT '组件路径',
    `sort` INT COMMENT '排序',
    `status` VARCHAR(10) DEFAULT '0' COMMENT '状态 0正常 1禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统权限表';

-- ============================================
-- 5. sys_role_permission 角色权限关联表
-- ============================================
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ============================================
-- 6. sys_dept 院系表
-- ============================================
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dept_name` VARCHAR(100) NOT NULL COMMENT '院系名称',
    `dept_code` VARCHAR(50) COMMENT '院系代码',
    `parent_id` BIGINT COMMENT '父院系ID',
    `is_top_level` VARCHAR(10) COMMENT '是否一流学科 0否 1是',
    `sort` INT COMMENT '排序',
    `status` VARCHAR(10) DEFAULT '0' COMMENT '状态 0正常 1禁用',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='院系表';

-- ============================================
-- 7. sys_major 专业表
-- ============================================
DROP TABLE IF EXISTS `sys_major`;
CREATE TABLE `sys_major` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `major_name` VARCHAR(100) NOT NULL COMMENT '专业名称',
    `major_code` VARCHAR(50) COMMENT '专业代码',
    `dept_id` BIGINT NOT NULL COMMENT '院系ID',
    `degree_type` VARCHAR(20) COMMENT '学位类型',
    `is_top_level` VARCHAR(10) COMMENT '是否一流专业 0否 1是',
    `short_name` VARCHAR(50) COMMENT '专业简称，用于班级名生成',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_major_code` (`major_code`),
    KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专业表';

-- ============================================
-- 8. sys_class 班级表
-- ============================================
DROP TABLE IF EXISTS `sys_class`;
CREATE TABLE `sys_class` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `class_name` VARCHAR(50) NOT NULL COMMENT '班级名称',
    `major_id` BIGINT NOT NULL COMMENT '专业ID',
    `dept_id` BIGINT NOT NULL COMMENT '院系ID',
    `grade` VARCHAR(10) COMMENT '年级',
    `advisor` VARCHAR(50) COMMENT '班主任姓名',
    `advisor_id` BIGINT COMMENT '班主任用户ID',
    `student_count` INT DEFAULT 0 COMMENT '学生人数',
    `status` VARCHAR(10) DEFAULT '0' COMMENT '状态 0正常 1禁用',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_major_id` (`major_id`),
    KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';

-- ============================================
-- 9. sys_invitation_code 注册码表
-- ============================================
DROP TABLE IF EXISTS `sys_invitation_code`;
CREATE TABLE `sys_invitation_code` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code` VARCHAR(20) NOT NULL COMMENT '注册码',
    `role_key` VARCHAR(50) COMMENT '角色标识',
    `dept_id` BIGINT COMMENT '学院ID',
    `class_id` BIGINT COMMENT '班级ID（可选，班主任注册码可绑定班级）',
    `used_by` BIGINT COMMENT '使用者用户ID',
    `used_username` VARCHAR(50) COMMENT '使用者用户名',
    `used_time` DATETIME COMMENT '使用时间',
    `expires_time` DATETIME COMMENT '过期时间',
    `status` VARCHAR(10) DEFAULT 'unused' COMMENT '状态 unused未使用 used已使用',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_role_key` (`role_key`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='注册码表';

-- ============================================
-- 10. student_info 学生信息表
-- ============================================
DROP TABLE IF EXISTS `student_info`;
CREATE TABLE `student_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `student_no` VARCHAR(30) NOT NULL COMMENT '学号',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `gender` VARCHAR(10) COMMENT '性别',
    `birth_date` VARCHAR(50) COMMENT '出生日期',
    `id_card` VARCHAR(20) COMMENT '身份证号',
    `nation` VARCHAR(50) COMMENT '民族',
    `politics_status` VARCHAR(50) COMMENT '政治面貌',
    `phone` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `province` VARCHAR(50) COMMENT '省份',
    `city` VARCHAR(50) COMMENT '城市',
    `address` VARCHAR(200) COMMENT '详细地址',
    `dept_id` BIGINT COMMENT '院系ID',
    `dept_name` VARCHAR(100) COMMENT '院系名称',
    `major_id` BIGINT COMMENT '专业ID',
    `major_name` VARCHAR(100) COMMENT '专业名称',
    `class_name` VARCHAR(50) COMMENT '班级名称',
    `class_id` BIGINT COMMENT '班级ID（关联sys_class）',
    `graduation_year` INT COMMENT '毕业年份',
    `study_type` VARCHAR(20) COMMENT '培养类型',
    `dormitory` VARCHAR(100) COMMENT '宿舍',
    `emergency_contact` VARCHAR(50) COMMENT '紧急联系人',
    `emergency_phone` VARCHAR(20) COMMENT '紧急联系电话',
    `avatar` VARCHAR(500) COMMENT '头像',
    `status` VARCHAR(20) DEFAULT 'studying' COMMENT '状态 studying在读 graduated已毕业',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_student_no` (`student_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生信息表';

-- ============================================
-- 11. student_resume 学生简历表
-- ============================================
DROP TABLE IF EXISTS `student_resume`;
CREATE TABLE `student_resume` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `resume_name` VARCHAR(100) NOT NULL COMMENT '简历名称',
    `is_default` VARCHAR(10) DEFAULT '0' COMMENT '是否默认 0否 1是',
    `personal_summary` TEXT COMMENT '个人简介',
    `education_experience` TEXT COMMENT '教育经历',
    `project_experience` TEXT COMMENT '项目经验',
    `work_experience` TEXT COMMENT '工作经历',
    `skill_certificates` TEXT COMMENT '技能证书',
    `awards_honors` TEXT COMMENT '获奖荣誉',
    `self_evaluation` TEXT COMMENT '自我评价',
    `expected_salary_min` INT COMMENT '期望薪资最低',
    `expected_salary_max` INT COMMENT '期望薪资最高',
    `expected_city` VARCHAR(100) COMMENT '期望城市',
    `expected_position` VARCHAR(100) COMMENT '期望职位',
    `expected_industry` VARCHAR(100) COMMENT '期望行业',
    `file_path` VARCHAR(500) COMMENT '附件路径',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生简历表';

-- ============================================
-- 12. company_info 企业信息表
-- ============================================
DROP TABLE IF EXISTS `company_info`;
CREATE TABLE `company_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT COMMENT '用户ID',
    `company_name` VARCHAR(200) NOT NULL COMMENT '企业名称',
    `company_code` VARCHAR(50) COMMENT '企业代码',
    `unified_credit_code` VARCHAR(50) COMMENT '统一社会信用代码',
    `legal_person` VARCHAR(50) COMMENT '法人代表',
    `contact_person` VARCHAR(50) COMMENT '联系人',
    `contact_phone` VARCHAR(20) COMMENT '联系电话',
    `contact_email` VARCHAR(100) COMMENT '联系邮箱',
    `province` VARCHAR(50) COMMENT '省份',
    `city` VARCHAR(50) COMMENT '城市',
    `district` VARCHAR(50) COMMENT '区县',
    `address` VARCHAR(200) COMMENT '详细地址',
    `latitude` VARCHAR(50) COMMENT '纬度',
    `longitude` VARCHAR(50) COMMENT '经度',
    `industry` VARCHAR(100) COMMENT '所属行业',
    `scale` VARCHAR(50) COMMENT '企业规模',
    `nature` VARCHAR(50) COMMENT '企业性质',
    `website` VARCHAR(200) COMMENT '官网',
    `introduction` TEXT COMMENT '企业简介',
    `logo` VARCHAR(500) COMMENT 'LOGO',
    `business_license` VARCHAR(500) COMMENT '营业执照',
    `auth_status` VARCHAR(20) DEFAULT 'pending' COMMENT '认证状态 pending待审核 approved已认证 rejected已拒绝',
    `status` VARCHAR(10) DEFAULT '0' COMMENT '状态 0正常 1禁用',
    `dept_id` BIGINT COMMENT '入驻学院ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_company_code` (`company_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业信息表';

-- ============================================
-- 13. company_auth 企业资质认证表
-- ============================================
DROP TABLE IF EXISTS `company_auth`;
CREATE TABLE `company_auth` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `company_id` BIGINT NOT NULL COMMENT '企业ID',
    `auth_type` VARCHAR(50) COMMENT '认证类型',
    `auth_name` VARCHAR(100) COMMENT '认证名称',
    `file_path` VARCHAR(500) COMMENT '文件路径',
    `audit_status` VARCHAR(20) DEFAULT 'pending' COMMENT '审核状态 pending待审核 approved已通过 rejected已拒绝',
    `audit_user_id` BIGINT COMMENT '审核人ID',
    `audit_time` VARCHAR(50) COMMENT '审核时间',
    `audit_remark` VARCHAR(500) COMMENT '审核备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业资质认证表';

-- ============================================
-- 14. job_position 职位表
-- ============================================
DROP TABLE IF EXISTS `job_position`;
CREATE TABLE `job_position` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `company_id` BIGINT NOT NULL COMMENT '企业ID',
    `company_name` VARCHAR(200) COMMENT '企业名称',
    `job_name` VARCHAR(100) NOT NULL COMMENT '职位名称',
    `job_category` VARCHAR(100) COMMENT '职位类别',
    `job_type` VARCHAR(50) COMMENT '工作性质',
    `work_city` VARCHAR(50) COMMENT '工作城市',
    `work_address` VARCHAR(200) COMMENT '工作地址',
    `salary_min` INT COMMENT '最低薪资',
    `salary_max` INT COMMENT '最高薪资',
    `salary_months` VARCHAR(20) COMMENT '薪资月份',
    `recruit_number` INT COMMENT '招聘人数',
    `requirement` TEXT COMMENT '职位要求',
    `responsibility` TEXT COMMENT '岗位职责',
    `benefits` TEXT COMMENT '福利待遇',
    `education_required` VARCHAR(50) COMMENT '学历要求',
    `experience_required` VARCHAR(50) COMMENT '经验要求',
    `skill_required` TEXT COMMENT '技能要求',
    `is_remote` VARCHAR(10) DEFAULT '0' COMMENT '是否远程 0否 1是',
    `is_high_salary` VARCHAR(10) DEFAULT '0' COMMENT '是否高薪 0否 1是',
    `is_deleted` VARCHAR(10) DEFAULT '0' COMMENT '是否删除 0否 1是',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `apply_count` INT DEFAULT 0 COMMENT '申请次数',
    `status` VARCHAR(20) DEFAULT 'published' COMMENT '状态 published已发布 closed已关闭 draft草稿',
    `publish_time` VARCHAR(50) COMMENT '发布时间',
    `deadline` VARCHAR(50) COMMENT '截止时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_company_id` (`company_id`),
    KEY `idx_job_name` (`job_name`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位表';

-- ============================================
-- 15. job_application 职位申请表
-- ============================================
DROP TABLE IF EXISTS `job_application`;
CREATE TABLE `job_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_id` BIGINT NOT NULL COMMENT '职位ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `company_id` BIGINT NOT NULL COMMENT '企业ID',
    `resume_id` BIGINT COMMENT '简历ID',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态 pending待处理 reviewing查看中 accepted已接受 rejected已拒绝 withdrawn已撤回',
    `read_status` VARCHAR(10) DEFAULT '0' COMMENT '阅读状态 0未读 1已读',
    `apply_letter` TEXT COMMENT '申请信',
    `company_remark` VARCHAR(500) COMMENT '企业备注',
    `interview_status` VARCHAR(20) COMMENT '面试状态',
    `offer_status` VARCHAR(20) COMMENT 'offer状态',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_company_id` (`company_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位申请表';

-- ============================================
-- 16. employment_record 就业记录表
-- ============================================
DROP TABLE IF EXISTS `employment_record`;
CREATE TABLE `employment_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `employment_type` VARCHAR(50) NOT NULL COMMENT '就业类型',
    `company_name` VARCHAR(200) COMMENT '公司名称',
    `company_code` VARCHAR(50) COMMENT '公司代码',
    `company_scale` VARCHAR(50) COMMENT '公司规模',
    `company_industry` VARCHAR(100) COMMENT '公司行业',
    `position_name` VARCHAR(100) COMMENT '岗位名称',
    `position_category` VARCHAR(100) COMMENT '岗位类别',
    `work_city` VARCHAR(50) COMMENT '工作城市',
    `work_province` VARCHAR(50) COMMENT '工作省份',
    `salary` VARCHAR(50) COMMENT '薪资',
    `is_three_party_signed` VARCHAR(10) DEFAULT '0' COMMENT '是否签署三方 0否 1是',
    `three_party_no` VARCHAR(100) COMMENT '三方协议号',
    `contract_start_date` VARCHAR(50) COMMENT '合同开始日期',
    `contract_end_date` VARCHAR(50) COMMENT '合同结束日期',
    `probation_salary` VARCHAR(50) COMMENT '试用期薪资',
    `audit_status` VARCHAR(20) DEFAULT 'pending' COMMENT '审核状态 pending待审核 approved已通过 rejected已拒绝',
    `audit_remark` VARCHAR(500) COMMENT '审核备注',
    `audit_user_id` BIGINT COMMENT '审核人ID',
    `audit_time` VARCHAR(50) COMMENT '审核时间',
    `remark` TEXT COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='就业记录表';

-- ============================================
-- 17. interview_invitation 面试邀请表
-- ============================================
DROP TABLE IF EXISTS `interview_invitation`;
CREATE TABLE `interview_invitation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `application_id` BIGINT NOT NULL COMMENT '申请ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `company_id` BIGINT NOT NULL COMMENT '企业ID',
    `job_id` BIGINT NOT NULL COMMENT '职位ID',
    `interview_time` VARCHAR(50) COMMENT '面试时间',
    `interview_address` VARCHAR(200) COMMENT '面试地址',
    `interview_type` VARCHAR(50) COMMENT '面试类型',
    `contact_person` VARCHAR(50) COMMENT '联系人',
    `contact_phone` VARCHAR(20) COMMENT '联系电话',
    `remark` TEXT COMMENT '备注',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态 pending待确认 confirmed已确认 cancelled已取消 completed已完成',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_application_id` (`application_id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试邀请表';

-- ============================================
-- 18. interview_record 面试记录表
-- ============================================
DROP TABLE IF EXISTS `interview_record`;
CREATE TABLE `interview_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `invitation_id` BIGINT NOT NULL COMMENT '邀请ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `company_id` BIGINT NOT NULL COMMENT '企业ID',
    `interview_result` VARCHAR(50) COMMENT '面试结果',
    `interview_feedback` TEXT COMMENT '面试反馈',
    `score` INT COMMENT '评分',
    `company_remark` VARCHAR(500) COMMENT '企业备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_invitation_id` (`invitation_id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试记录表';

-- ============================================
-- 19. offer_letter offer表
-- ============================================
DROP TABLE IF EXISTS `offer_letter`;
CREATE TABLE `offer_letter` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `application_id` BIGINT NOT NULL COMMENT '申请ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `company_id` BIGINT NOT NULL COMMENT '企业ID',
    `job_id` BIGINT NOT NULL COMMENT '职位ID',
    `position_name` VARCHAR(100) COMMENT '岗位名称',
    `salary` VARCHAR(50) COMMENT '薪资',
    `work_city` VARCHAR(50) COMMENT '工作城市',
    `start_date` VARCHAR(50) COMMENT '入职日期',
    `probation_period` VARCHAR(50) COMMENT '试用期',
    `probation_salary` VARCHAR(50) COMMENT '试用期薪资',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态 pending待确认 accepted已接受 declined已拒绝 withdrawn已撤回',
    `response_deadline` VARCHAR(50) COMMENT '回复截止时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_application_id` (`application_id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Offer表';

-- ============================================
-- 20. tripartite_agreement 三方协议表
-- ============================================
DROP TABLE IF EXISTS `tripartite_agreement`;
CREATE TABLE `tripartite_agreement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `company_id` BIGINT NOT NULL COMMENT '企业ID',
    `employment_record_id` BIGINT COMMENT '就业记录ID',
    `agreement_no` VARCHAR(100) COMMENT '协议编号',
    `student_sign_time` VARCHAR(50) COMMENT '学生签署时间',
    `company_sign_time` VARCHAR(50) COMMENT '企业签署时间',
    `school_sign_time` VARCHAR(50) COMMENT '学校签署时间',
    `file_path` VARCHAR(500) COMMENT '协议文件路径',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态 pending待签署 student_signed学生已签 company_signed企业已签 completed已完成',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_company_id` (`company_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='三方协议表';

-- ============================================
-- 21. employment_attachment 就业附件表
-- ============================================
DROP TABLE IF EXISTS `employment_attachment`;
CREATE TABLE `employment_attachment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employment_id` BIGINT NOT NULL COMMENT '就业记录ID',
    `attachment_type` VARCHAR(50) COMMENT '附件类型',
    `attachment_name` VARCHAR(200) COMMENT '附件名称',
    `file_path` VARCHAR(500) COMMENT '文件路径',
    `file_size` BIGINT COMMENT '文件大小',
    `upload_status` VARCHAR(20) DEFAULT 'pending' COMMENT '上传状态 pending待上传 uploaded已上传 failed失败',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_employment_id` (`employment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='就业附件表';

-- ============================================
-- 22. job_favorite 职位收藏表
-- ============================================
DROP TABLE IF EXISTS `job_favorite`;
CREATE TABLE `job_favorite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `job_id` BIGINT NOT NULL COMMENT '职位ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `notes` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_student_id` (`student_id`),
    UNIQUE KEY `uk_job_student` (`job_id`, `student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位收藏表';

-- ============================================
-- 23. sys_notice 系统公告表
-- ============================================
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `notice_type` VARCHAR(50) COMMENT '公告类型',
    `target_roles` VARCHAR(200) COMMENT '目标角色，多个用逗号分隔，为空或all表示全部用户可见',
    `publisher_id` BIGINT COMMENT '发布人ID',
    `publisher_name` VARCHAR(50) COMMENT '发布人姓名',
    `publish_time` VARCHAR(50) COMMENT '发布时间',
    `top_status` VARCHAR(10) DEFAULT '0' COMMENT '置顶状态 0否 1是',
    `status` VARCHAR(20) DEFAULT 'published' COMMENT '状态 published已发布 draft草稿',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告表';

-- ============================================
-- 24. notice_read_record 公告已读记录表（v2.0 新增）
-- 用途：记录每个用户对每条公告的已读状态，实现精准的未读计数
-- ============================================
DROP TABLE IF EXISTS `notice_read_record`;
CREATE TABLE `notice_read_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `notice_id` BIGINT NOT NULL COMMENT '公告ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `read_time` VARCHAR(50) COMMENT '阅读时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notice_user` (`notice_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告已读记录表';

-- ============================================
-- 25. notification 通知消息表
-- ============================================
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `type` VARCHAR(50) COMMENT '类型',
    `category` VARCHAR(50) COMMENT '分类 interview/offer/agreement/application/system',
    `sender_id` BIGINT COMMENT '发送者ID',
    `sender_name` VARCHAR(50) COMMENT '发送者姓名',
    `related_id` BIGINT COMMENT '关联ID',
    `related_type` VARCHAR(50) COMMENT '关联类型',
    `is_read` VARCHAR(10) DEFAULT '0' COMMENT '是否已读 0否 1是',
    `read_time` VARCHAR(50) COMMENT '阅读时间',
    `priority` VARCHAR(20) DEFAULT 'normal' COMMENT '优先级 low一般 normal普通 high重要 urgent紧急',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知消息表';

-- ============================================
-- 26. conversation_record 谈话记录表
-- ============================================
DROP TABLE IF EXISTS `conversation_record`;
CREATE TABLE `conversation_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `teacher_id` BIGINT NOT NULL COMMENT '指导老师ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `conversation_time` VARCHAR(50) COMMENT '谈话时间',
    `conversation_type` VARCHAR(50) COMMENT '谈话类型',
    `conversation_place` VARCHAR(200) COMMENT '谈话地点',
    `topic` VARCHAR(200) COMMENT '主题',
    `content` TEXT COMMENT '内容',
    `result` TEXT COMMENT '结果',
    `next_plan` TEXT COMMENT '后续计划',
    `attachment_path` VARCHAR(500) COMMENT '附件路径',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='谈话记录表';

-- ============================================
-- 27. consultation_booking 咨询预约表
-- ============================================
DROP TABLE IF EXISTS `consultation_booking`;
CREATE TABLE `consultation_booking` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `consultant_id` BIGINT COMMENT '咨询师ID',
    `booking_date` VARCHAR(50) COMMENT '预约日期',
    `booking_time` VARCHAR(50) COMMENT '预约时间',
    `consultation_type` VARCHAR(50) COMMENT '咨询类型',
    `topic` VARCHAR(200) COMMENT '主题',
    `description` TEXT COMMENT '详细描述',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态 pending待确认 confirmed已确认 completed已完成 cancelled已取消',
    `result` TEXT COMMENT '咨询结果',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_consultant_id` (`consultant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='咨询预约表';

-- ============================================
-- 28. career_activity 就业活动表
-- ============================================
DROP TABLE IF EXISTS `career_activity`;
CREATE TABLE `career_activity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `activity_name` VARCHAR(200) NOT NULL COMMENT '活动名称',
    `activity_type` VARCHAR(50) COMMENT '活动类型',
    `organizer` VARCHAR(200) COMMENT '主办方',
    `speaker` VARCHAR(100) COMMENT '主讲人',
    `start_time` VARCHAR(50) COMMENT '开始时间',
    `end_time` VARCHAR(50) COMMENT '结束时间',
    `location` VARCHAR(200) COMMENT '活动地点',
    `max_participants` INT COMMENT '最大参与人数',
    `current_participants` INT DEFAULT 0 COMMENT '当前报名人数',
    `content` TEXT COMMENT '活动内容',
    `poster` VARCHAR(500) COMMENT '海报',
    `status` VARCHAR(20) DEFAULT 'published' COMMENT '状态 published已发布 cancelled已取消 ended已结束',
    `publish_time` VARCHAR(50) COMMENT '发布时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='就业活动表';

-- ============================================
-- 29. activity_registration 活动报名表
-- ============================================
DROP TABLE IF EXISTS `activity_registration`;
CREATE TABLE `activity_registration` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `registration_time` VARCHAR(50) COMMENT '报名时间',
    `status` VARCHAR(20) DEFAULT 'registered' COMMENT '状态 registered已报名 cancelled已取消 checked_in已签到 absent缺席',
    `check_in_time` VARCHAR(50) COMMENT '签到时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_activity_id` (`activity_id`),
    KEY `idx_student_id` (`student_id`),
    UNIQUE KEY `uk_activity_student` (`activity_id`, `student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动报名表';

-- ============================================
-- 30. data_permission_request 数据权限申请表
-- ============================================
DROP TABLE IF EXISTS `data_permission_request`;
CREATE TABLE `data_permission_request` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `dept_id` BIGINT COMMENT '院系ID',
    `major_id` BIGINT COMMENT '专业ID',
    `request_type` VARCHAR(50) COMMENT '申请类型',
    `year_from` INT COMMENT '年份范围开始',
    `year_to` INT COMMENT '年份范围结束',
    `reason` TEXT COMMENT '申请理由',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态 pending待审核 approved已通过 rejected已拒绝',
    `audit_user_id` BIGINT COMMENT '审核人ID',
    `audit_time` VARCHAR(50) COMMENT '审核时间',
    `audit_remark` VARCHAR(500) COMMENT '审核备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据权限申请表';

-- ============================================
-- 31. class_employment_reminder 班级就业提醒表（新增）
-- 用途：存储系统自动生成和教师手动发送的就业提醒
-- ============================================
DROP TABLE IF EXISTS `class_employment_reminder`;
CREATE TABLE `class_employment_reminder` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sender_id` BIGINT COMMENT '发送者ID',
    `sender_name` VARCHAR(50) COMMENT '发送者姓名',
    `receiver_id` BIGINT COMMENT '接收者ID（班主任）',
    `receiver_name` VARCHAR(50) COMMENT '接收者姓名',
    `class_id` BIGINT COMMENT '班级ID',
    `class_name` VARCHAR(50) COMMENT '班级名称',
    `title` VARCHAR(200) COMMENT '提醒标题',
    `content` TEXT COMMENT '提醒内容',
    `employment_rate` VARCHAR(20) COMMENT '班级就业率',
    `total_students` INT COMMENT '班级总人数',
    `employed_students` INT COMMENT '已就业人数',
    `unemployed_students` INT COMMENT '未就业人数',
    `status` VARCHAR(20) DEFAULT 'unread' COMMENT '状态 unread未读 read已读',
    `is_read` VARCHAR(10) DEFAULT '0' COMMENT '是否已读 0否 1是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_receiver_id` (`receiver_id`),
    KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级就业提醒表';

-- ============================================
-- 32. invitation_code 邀请码表（备用）
-- ============================================
DROP TABLE IF EXISTS `invitation_code`;
CREATE TABLE `invitation_code` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code` VARCHAR(20) NOT NULL COMMENT '邀请码',
    `role_key` VARCHAR(50) COMMENT '角色标识',
    `dept_id` BIGINT COMMENT '学院ID',
    `class_id` BIGINT COMMENT '班级ID',
    `used_by` BIGINT COMMENT '使用者用户ID',
    `used_username` VARCHAR(50) COMMENT '使用者用户名',
    `used_time` DATETIME COMMENT '使用时间',
    `expires_time` DATETIME COMMENT '过期时间',
    `status` VARCHAR(10) DEFAULT 'unused' COMMENT '状态 unused未使用 used已使用',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请码表';

-- ============================================
-- 33. sys_log 系统日志表
-- ============================================
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT COMMENT '用户ID',
    `username` VARCHAR(50) COMMENT '操作用户',
    `operation` VARCHAR(100) COMMENT '操作描述',
    `module` VARCHAR(100) COMMENT '操作模块',
    `description` VARCHAR(500) COMMENT '详细描述',
    `method` VARCHAR(200) COMMENT '请求方法',
    `params` TEXT COMMENT '请求参数',
    `ip` VARCHAR(50) COMMENT 'IP地址',
    `user_agent` VARCHAR(500) COMMENT '用户代理',
    `execution_time` BIGINT COMMENT '执行时长(ms)',
    `log_type` VARCHAR(20) COMMENT '日志类型 login登录 operation操作',
    `status` VARCHAR(10) DEFAULT '0' COMMENT '状态 0成功 1异常',
    `error_msg` TEXT COMMENT '错误信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_username` (`username`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统日志表';

-- ============================================
-- 复合索引（性能优化）
-- ============================================
CREATE INDEX idx_student_dept_year ON `student_info`(`dept_id`, `graduation_year`);
CREATE INDEX idx_employment_student_year ON `employment_record`(`student_id`, `employment_type`);
CREATE INDEX idx_job_company_status ON `job_position`(`company_id`, `status`);
CREATE INDEX idx_application_student_status ON `job_application`(`student_id`, `status`);


-- ============================================
-- ============================================
-- 基础数据（角色由本文件插入，院系、专业、班级、企业由本文件创建）
-- 注意：本文件不包含任何测试账号，账号需通过系统注册功能创建
-- ============================================

-- 注意：账号数据已迁移至系统注册功能，无需在此预置


-- ============================================
-- ============================================
-- 数据修复：就业记录省份字段补全（fix_work_province.sql）
-- ============================================

-- 1. 修复 继续深造
UPDATE employment_record
SET work_province = '湖北省'
WHERE employment_type = '继续深造' AND (work_province IS NULL OR work_province = '');

-- 2. 修复 应征入伍
UPDATE employment_record
SET work_province = '湖北省'
WHERE employment_type = '应征入伍' AND (work_province IS NULL OR work_province = '');

-- 3. 修复 暂未就业
UPDATE employment_record
SET work_province = '湖北省'
WHERE employment_type = '暂未就业' AND (work_province IS NULL OR work_province = '');

-- 4. 修复 出国出境
UPDATE employment_record
SET work_province = '境外'
WHERE employment_type = '出国出境' AND (work_province IS NULL OR work_province = '');

-- 5. 修复 其他
UPDATE employment_record
SET work_province = '湖北省'
WHERE employment_type = '其他' AND (work_province IS NULL OR work_province = '');

-- 6. 修复 自主创业/签订劳动合同等（城市反推省份，每行一个城市，避免批处理截断）
UPDATE employment_record SET work_province = CASE work_city
    WHEN '北京市' THEN '北京市' WHEN '天津市' THEN '天津市' WHEN '上海市' THEN '上海市' WHEN '重庆市' THEN '重庆市'
    WHEN '石家庄市' THEN '河北省' WHEN '唐山市' THEN '河北省' WHEN '保定市' THEN '河北省' WHEN '邯郸市' THEN '河北省'
    WHEN '秦皇岛市' THEN '河北省' WHEN '沧州市' THEN '河北省' WHEN '廊坊市' THEN '河北省' WHEN '邢台市' THEN '河北省'
    WHEN '太原市' THEN '山西省' WHEN '大同市' THEN '山西省' WHEN '长治市' THEN '山西省' WHEN '晋城市' THEN '山西省'
    WHEN '呼和浩特市' THEN '内蒙古自治区' WHEN '包头市' THEN '内蒙古自治区' WHEN '鄂尔多斯市' THEN '内蒙古自治区'
    WHEN '沈阳市' THEN '辽宁省' WHEN '大连市' THEN '辽宁省' WHEN '鞍山市' THEN '辽宁省' WHEN '锦州市' THEN '辽宁省'
    WHEN '丹东市' THEN '辽宁省' WHEN '抚顺市' THEN '辽宁省' WHEN '本溪市' THEN '辽宁省'
    WHEN '长春市' THEN '吉林省' WHEN '吉林市' THEN '吉林省' WHEN '四平市' THEN '吉林省'
    WHEN '哈尔滨市' THEN '黑龙江省' WHEN '大庆市' THEN '黑龙江省' WHEN '齐齐哈尔市' THEN '黑龙江省' WHEN '牡丹江市' THEN '黑龙江省'
    WHEN '南京市' THEN '江苏省' WHEN '苏州市' THEN '江苏省' WHEN '无锡市' THEN '江苏省' WHEN '常州市' THEN '江苏省'
    WHEN '南通市' THEN '江苏省' WHEN '徐州市' THEN '江苏省' WHEN '扬州市' THEN '江苏省' WHEN '盐城市' THEN '江苏省'
    WHEN '连云港市' THEN '江苏省' WHEN '泰州市' THEN '江苏省' WHEN '镇江市' THEN '江苏省' WHEN '淮安市' THEN '江苏省'
    WHEN '宿迁市' THEN '江苏省' WHEN '扬州市' THEN '江苏省'
    WHEN '杭州市' THEN '浙江省' WHEN '宁波市' THEN '浙江省' WHEN '温州市' THEN '浙江省' WHEN '嘉兴市' THEN '浙江省'
    WHEN '绍兴市' THEN '浙江省' WHEN '金华市' THEN '浙江省' WHEN '湖州市' THEN '浙江省' WHEN '台州市' THEN '浙江省'
    WHEN '舟山市' THEN '浙江省' WHEN '衢州市' THEN '浙江省' WHEN '丽水市' THEN '浙江省'
    WHEN '合肥市' THEN '安徽省' WHEN '芜湖市' THEN '安徽省' WHEN '蚌埠市' THEN '安徽省' WHEN '淮南市' THEN '安徽省'
    WHEN '马鞍山市' THEN '安徽省' WHEN '安庆市' THEN '安徽省' WHEN '阜阳市' THEN '安徽省' WHEN '滁州市' THEN '安徽省'
    WHEN '宿州市' THEN '安徽省' WHEN '六安市' THEN '安徽省' WHEN '铜陵市' THEN '安徽省' WHEN '池州市' THEN '安徽省'
    WHEN '宣城市' THEN '安徽省' WHEN '黄山市' THEN '安徽省' WHEN '淮北市' THEN '安徽省'
    WHEN '福州市' THEN '福建省' WHEN '厦门市' THEN '福建省' WHEN '泉州市' THEN '福建省' WHEN '漳州市' THEN '福建省'
    WHEN '莆田市' THEN '福建省' WHEN '宁德市' THEN '福建省' WHEN '龙岩市' THEN '福建省' WHEN '三明市' THEN '福建省' WHEN '南平市' THEN '福建省'
    WHEN '南昌市' THEN '江西省' WHEN '赣州市' THEN '江西省' WHEN '九江市' THEN '江西省' WHEN '上饶市' THEN '江西省'
    WHEN '宜春市' THEN '江西省' WHEN '吉安市' THEN '江西省' WHEN '抚州市' THEN '江西省' WHEN '景德镇市' THEN '江西省'
    WHEN '萍乡市' THEN '江西省' WHEN '新余市' THEN '江西省' WHEN '鹰潭市' THEN '江西省'
    WHEN '济南市' THEN '山东省' WHEN '青岛市' THEN '山东省' WHEN '烟台市' THEN '山东省' WHEN '潍坊市' THEN '山东省'
    WHEN '临沂市' THEN '山东省' WHEN '淄博市' THEN '山东省' WHEN '威海市' THEN '山东省' WHEN '济宁市' THEN '山东省'
    WHEN '泰安市' THEN '山东省' WHEN '德州市' THEN '山东省' WHEN '聊城市' THEN '山东省' WHEN '滨州市' THEN '山东省'
    WHEN '菏泽市' THEN '山东省' WHEN '枣庄市' THEN '山东省' WHEN '日照市' THEN '山东省'
    WHEN '武汉市' THEN '湖北省' WHEN '宜昌市' THEN '湖北省' WHEN '襄阳市' THEN '湖北省' WHEN '荆州市' THEN '湖北省'
    WHEN '黄石市' THEN '湖北省' WHEN '十堰市' THEN '湖北省' WHEN '孝感市' THEN '湖北省' WHEN '黄冈市' THEN '湖北省'
    WHEN '咸宁市' THEN '湖北省' WHEN '随州市' THEN '湖北省' WHEN '恩施市' THEN '湖北省' WHEN '荆门市' THEN '湖北省'
    WHEN '鄂州市' THEN '湖北省' WHEN '仙桃市' THEN '湖北省' WHEN '潜江市' THEN '湖北省' WHEN '天门市' THEN '湖北省' WHEN '神农架林区' THEN '湖北省'
    WHEN '长沙市' THEN '湖南省' WHEN '株洲市' THEN '湖南省' WHEN '湘潭市' THEN '湖南省' WHEN '衡阳市' THEN '湖南省'
    WHEN '岳阳市' THEN '湖南省' WHEN '常德市' THEN '湖南省' WHEN '益阳市' THEN '湖南省' WHEN '郴州市' THEN '湖南省'
    WHEN '永州市' THEN '湖南省' WHEN '邵阳市' THEN '湖南省' WHEN '怀化市' THEN '湖南省' WHEN '娄底市' THEN '湖南省'
    WHEN '张家界市' THEN '湖南省' WHEN '湘西土家族苗族自治州' THEN '湖南省'
    WHEN '郑州市' THEN '河南省' WHEN '洛阳市' THEN '河南省' WHEN '开封市' THEN '河南省' WHEN '新乡市' THEN '河南省'
    WHEN '南阳市' THEN '河南省' WHEN '许昌市' THEN '河南省' WHEN '安阳市' THEN '河南省' WHEN '平顶山市' THEN '河南省'
    WHEN '商丘市' THEN '河南省' WHEN '周口市' THEN '河南省' WHEN '信阳市' THEN '河南省' WHEN '驻马店市' THEN '河南省'
    WHEN '焦作市' THEN '河南省' WHEN '濮阳市' THEN '河南省' WHEN '三门峡市' THEN '河南省'
    WHEN '广州市' THEN '广东省' WHEN '深圳市' THEN '广东省' WHEN '佛山市' THEN '广东省' WHEN '东莞市' THEN '广东省'
    WHEN '珠海市' THEN '广东省' WHEN '中山市' THEN '广东省' WHEN '惠州市' THEN '广东省' WHEN '江门市' THEN '广东省'
    WHEN '湛江市' THEN '广东省' WHEN '茂名市' THEN '广东省' WHEN '肇庆市' THEN '广东省' WHEN '汕头市' THEN '广东省'
    WHEN '韶关市' THEN '广东省' WHEN '清远市' THEN '广东省' WHEN '梅州市' THEN '广东省' WHEN '汕尾市' THEN '广东省'
    WHEN '河源市' THEN '广东省' WHEN '阳江市' THEN '广东省' WHEN '潮州市' THEN '广东省' WHEN '揭阳市' THEN '广东省' WHEN '云浮市' THEN '广东省'
    WHEN '南宁市' THEN '广西壮族自治区' WHEN '柳州市' THEN '广西壮族自治区' WHEN '桂林市' THEN '广西壮族自治区' WHEN '梧州市' THEN '广西壮族自治区'
    WHEN '北海市' THEN '广西壮族自治区' WHEN '贵港市' THEN '广西壮族自治区' WHEN '玉林市' THEN '广西壮族自治区' WHEN '百色市' THEN '广西壮族自治区'
    WHEN '河池市' THEN '广西壮族自治区' WHEN '钦州市' THEN '广西壮族自治区' WHEN '防城港市' THEN '广西壮族自治区' WHEN '贺州市' THEN '广西壮族自治区'
    WHEN '来宾市' THEN '广西壮族自治区' WHEN '崇左市' THEN '广西壮族自治区'
    WHEN '海口市' THEN '海南省' WHEN '三亚市' THEN '海南省' WHEN '三沙市' THEN '海南省' WHEN '儋州市' THEN '海南省'
    WHEN '成都市' THEN '四川省' WHEN '绵阳市' THEN '四川省' WHEN '德阳市' THEN '四川省' WHEN '宜宾市' THEN '四川省'
    WHEN '南充市' THEN '四川省' WHEN '泸州市' THEN '四川省' WHEN '达州市' THEN '四川省' WHEN '乐山市' THEN '四川省'
    WHEN '内江市' THEN '四川省' WHEN '自贡市' THEN '四川省' WHEN '遂宁市' THEN '四川省' WHEN '广安市' THEN '四川省'
    WHEN '资阳市' THEN '四川省' WHEN '眉山市' THEN '四川省' WHEN '雅安市' THEN '四川省' WHEN '广元市' THEN '四川省'
    WHEN '巴中市' THEN '四川省' WHEN '攀枝花市' THEN '四川省'
    WHEN '贵阳市' THEN '贵州省' WHEN '遵义市' THEN '贵州省' WHEN '六盘水市' THEN '贵州省' WHEN '安顺市' THEN '贵州省'
    WHEN '毕节市' THEN '贵州省' WHEN '铜仁市' THEN '贵州省' WHEN '黔西南布依族苗族自治州' THEN '贵州省' WHEN '黔东南苗族侗族自治州' THEN '贵州省' WHEN '黔南布依族苗族自治州' THEN '贵州省'
    WHEN '昆明市' THEN '云南省' WHEN '曲靖市' THEN '云南省' WHEN '玉溪市' THEN '云南省' WHEN '保山市' THEN '云南省'
    WHEN '昭通市' THEN '云南省' WHEN '丽江市' THEN '云南省' WHEN '普洱市' THEN '云南省' WHEN '临沧市' THEN '云南省'
    WHEN '楚雄彝族自治州' THEN '云南省' WHEN '红河哈尼族彝族自治州' THEN '云南省' WHEN '文山壮族苗族自治州' THEN '云南省'
    WHEN '西双版纳傣族自治州' THEN '云南省' WHEN '大理白族自治州' THEN '云南省' WHEN '德宏傣族景颇族自治州' THEN '云南省'
    WHEN '怒江傈僳族自治州' THEN '云南省' WHEN '迪庆藏族自治州' THEN '云南省'
    WHEN '拉萨市' THEN '西藏自治区' WHEN '日喀则市' THEN '西藏自治区' WHEN '昌都市' THEN '西藏自治区' WHEN '林芝市' THEN '西藏自治区'
    WHEN '山南市' THEN '西藏自治区' WHEN '那曲市' THEN '西藏自治区' WHEN '阿里地区' THEN '西藏自治区'
    WHEN '西安市' THEN '陕西省' WHEN '宝鸡市' THEN '陕西省' WHEN '咸阳市' THEN '陕西省' WHEN '铜川市' THEN '陕西省'
    WHEN '渭南市' THEN '陕西省' WHEN '延安市' THEN '陕西省' WHEN '榆林市' THEN '陕西省' WHEN '汉中市' THEN '陕西省'
    WHEN '安康市' THEN '陕西省' WHEN '商洛市' THEN '陕西省'
    WHEN '兰州市' THEN '甘肃省' WHEN '嘉峪关市' THEN '甘肃省' WHEN '金昌市' THEN '甘肃省' WHEN '白银市' THEN '甘肃省'
    WHEN '天水市' THEN '甘肃省' WHEN '武威市' THEN '甘肃省' WHEN '张掖市' THEN '甘肃省' WHEN '平凉市' THEN '甘肃省'
    WHEN '酒泉市' THEN '甘肃省' WHEN '庆阳市' THEN '甘肃省' WHEN '定西市' THEN '甘肃省' WHEN '陇南市' THEN '甘肃省'
    WHEN '临夏回族自治州' THEN '甘肃省' WHEN '甘南藏族自治州' THEN '甘肃省'
    WHEN '西宁市' THEN '青海省' WHEN '海东市' THEN '青海省' WHEN '海北藏族自治州' THEN '青海省' WHEN '黄南藏族自治州' THEN '青海省'
    WHEN '海南藏族自治州' THEN '青海省' WHEN '果洛藏族自治州' THEN '青海省' WHEN '玉树藏族自治州' THEN '青海省' WHEN '海西蒙古族藏族自治州' THEN '青海省'
    WHEN '银川市' THEN '宁夏回族自治区' WHEN '石嘴山市' THEN '宁夏回族自治区' WHEN '吴忠市' THEN '宁夏回族自治区' WHEN '固原市' THEN '宁夏回族自治区' WHEN '中卫市' THEN '宁夏回族自治区'
    WHEN '乌鲁木齐市' THEN '新疆维吾尔自治区' WHEN '克拉玛依市' THEN '新疆维吾尔自治区' WHEN '吐鲁番市' THEN '新疆维吾尔自治区' WHEN '哈密市' THEN '新疆维吾尔自治区'
    WHEN '阿克苏市' THEN '新疆维吾尔自治区' WHEN '喀什市' THEN '新疆维吾尔自治区' WHEN '和田市' THEN '新疆维吾尔自治区' WHEN '昌吉回族自治州' THEN '新疆维吾尔自治区'
    WHEN '博尔塔拉蒙古自治州' THEN '新疆维吾尔自治区' WHEN '巴音郭楞蒙古自治州' THEN '新疆维吾尔自治区' WHEN '伊犁哈萨克自治州' THEN '新疆维吾尔自治区'
    WHEN '境外' THEN '境外'
END WHERE (work_province IS NULL OR work_province = '') AND work_city IS NOT NULL;

-- 7. 修复 自由职业
UPDATE employment_record
SET work_province = '湖北省'
WHERE employment_type = '自由职业' AND (work_province IS NULL OR work_province = '');



-- ============================================
-- 部署完成
-- ============================================
-- 数据由 init.sql（表结构+基础数据）导入。
-- 账号需通过系统注册功能创建。
-- ============================================
