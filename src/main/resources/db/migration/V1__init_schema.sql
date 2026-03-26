-- Flyway V1: 初始数据库Schema
-- 从 schema.sql 迁移

-- 用户基础信息表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `phone` VARCHAR(11) NOT NULL UNIQUE COMMENT '手机号',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `id_card` VARCHAR(18) UNIQUE COMMENT '身份证号',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_phone` (`phone`),
    INDEX `idx_id_card` (`id_card`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户基础信息表';

-- 用户画像表
CREATE TABLE IF NOT EXISTS `user_profile` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE,
    `credit_score` INT COMMENT '综合信用分 (300-850)',
    `credit_grade` CHAR(1) COMMENT '信用等级 (A-G)',
    `annual_income` DECIMAL(12,2) COMMENT '年收入',
    `dti` DECIMAL(5,4) COMMENT '债务收入比 (0-1)',
    `employment_years` INT COMMENT '就业年限',
    `risk_score` DECIMAL(5,2) COMMENT '风险评分 (0-100)',
    `risk_level` TINYINT COMMENT '风险等级: 1-低 2-中 3-高',
    `behavior_features` JSON COMMENT '行为特征',
    `social_features` JSON COMMENT '社交特征',
    `credit_features` JSON COMMENT '征信特征',
    `profile_updated_at` DATETIME COMMENT '画像更新时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户画像表';

-- 额度表
CREATE TABLE IF NOT EXISTS `credit_limit` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `total_limit` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '总额度',
    `used_limit` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '已用额度',
    `available_limit` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '可用额度',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-冻结 1-正常',
    `expire_at` DATETIME COMMENT '额度到期时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='额度表';

-- 借贷申请表
CREATE TABLE IF NOT EXISTS `loan_application` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `application_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '申请单号',
    `user_id` BIGINT NOT NULL,
    `amount` DECIMAL(12,2) NOT NULL COMMENT '申请金额',
    `term` INT NOT NULL COMMENT '借款期限(月)',
    `purpose` VARCHAR(100) COMMENT '借款用途',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待审核 1-审核中 2-通过 3-拒绝 4-取消',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `reviewed_at` DATETIME COMMENT '审核时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='借贷申请表';

-- 借贷合同表
CREATE TABLE IF NOT EXISTS `loan_contract` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `contract_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '合同编号',
    `application_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `principal` DECIMAL(12,2) NOT NULL COMMENT '本金',
    `interest_rate` DECIMAL(5,4) NOT NULL COMMENT '年利率',
    `total_repayment` DECIMAL(12,2) NOT NULL COMMENT '总还款额',
    `term` INT NOT NULL COMMENT '期限(月)',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待放款 1-还款中 2-已结清 3-逾期',
    `disbursed_at` DATETIME COMMENT '放款时间',
    `due_date` DATE COMMENT '到期日',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='借贷合同表';

-- 还款计划表
CREATE TABLE IF NOT EXISTS `repayment_plan` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `contract_id` BIGINT NOT NULL,
    `period` INT NOT NULL COMMENT '期数',
    `due_date` DATE NOT NULL COMMENT '应还日期',
    `principal` DECIMAL(12,2) NOT NULL COMMENT '应还本金',
    `interest` DECIMAL(12,2) NOT NULL COMMENT '应还利息',
    `total_amount` DECIMAL(12,2) NOT NULL COMMENT '应还总额',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待还 1-已还 2-逾期',
    `paid_at` DATETIME COMMENT '实际还款时间',
    INDEX `idx_contract_id` (`contract_id`),
    INDEX `idx_due_date` (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='还款计划表';

-- 风险评估记录表
CREATE TABLE IF NOT EXISTS `risk_assessment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `assessment_no` VARCHAR(32) NOT NULL UNIQUE,
    `user_id` BIGINT NOT NULL,
    `application_id` BIGINT COMMENT '关联的借贷申请',
    `assessment_type` TINYINT NOT NULL COMMENT '类型: 1-额度评估 2-借款评估 3-贷后监控',
    `risk_score` DECIMAL(5,2) NOT NULL COMMENT '风险评分 (0-100)',
    `risk_level` TINYINT NOT NULL COMMENT '风险等级: 1-低 2-中 3-高',
    `decision` VARCHAR(20) NOT NULL COMMENT '决策结果',
    `model_version` VARCHAR(50) COMMENT '模型版本',
    `feature_snapshot` JSON COMMENT '特征快照',
    `factors` JSON COMMENT '风险因子分析',
    `processing_time_ms` INT COMMENT '处理耗时(毫秒)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_application_id` (`application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风险评估记录表';

-- 黑名单表
CREATE TABLE IF NOT EXISTS `blacklist` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `type` TINYINT NOT NULL COMMENT '类型: 1-手机号 2-身份证 3-设备ID',
    `value` VARCHAR(100) NOT NULL,
    `reason` VARCHAR(255),
    `expire_at` DATETIME COMMENT '过期时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_type_value` (`type`, `value`),
    INDEX `idx_value` (`value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='黑名单表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `config_key` VARCHAR(100) NOT NULL UNIQUE,
    `config_value` TEXT,
    `description` VARCHAR(255),
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 插入默认配置
INSERT IGNORE INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('default_interest_rate', '0.12', '默认年利率'),
('min_loan_amount', '1000', '最小借款金额'),
('max_loan_amount', '100000', '最大借款金额'),
('min_loan_term', '3', '最小借款期限(月)'),
('max_loan_term', '24', '最大借款期限(月)');

-- 数据采集记录表
CREATE TABLE IF NOT EXISTS `data_collection_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `source_type` VARCHAR(50) NOT NULL COMMENT '数据源类型',
    `request_id` VARCHAR(64) COMMENT '请求ID',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-请求中 1-成功 2-失败',
    `raw_data` JSON COMMENT '原始数据',
    `parsed_data` JSON COMMENT '解析后数据',
    `error_message` TEXT COMMENT '错误信息',
    `processing_time_ms` INT COMMENT '处理耗时(毫秒)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_source_type` (`source_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据采集记录表';

-- 管理员用户表
CREATE TABLE IF NOT EXISTS `admin_user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `phone` VARCHAR(11) COMMENT '手机号',
    `role` VARCHAR(20) DEFAULT 'ADMIN' COMMENT '角色: ADMIN-管理员 SUPER_ADMIN-超级管理员',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员用户表';

-- 插入默认管理员账号（密码: admin123）
INSERT IGNORE INTO `admin_user` (`username`, `password_hash`, `real_name`, `role`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z9qA2Qv.C3p4Xl.j5O7WPBKS', '超级管理员', 'SUPER_ADMIN', 1);
