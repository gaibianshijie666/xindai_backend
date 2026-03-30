-- ============================================================
-- 信贷系统 完整数据库Schema
-- 合并自 V1~V12 所有迁移文件
-- ============================================================

-- -----------------------------------------------------------
-- 1. 用户基础信息表
-- -----------------------------------------------------------
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

-- -----------------------------------------------------------
-- 2. 用户画像表 (含V3/V8追加字段)
-- -----------------------------------------------------------
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
    `identity_status` TINYINT DEFAULT 0 COMMENT '实名认证: 0=未认证,1=认证中,2=已认证,3=认证失败',
    `identity_verified_at` DATETIME NULL COMMENT '认证时间',
    `profile_updated_at` DATETIME COMMENT '画像更新时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_credit_grade` (`credit_grade`),
    INDEX `idx_risk_score` (`risk_score`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户画像表';

-- -----------------------------------------------------------
-- 3. 额度表
-- -----------------------------------------------------------
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

-- -----------------------------------------------------------
-- 4. 借贷申请表 (含V2/V6追加字段)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `loan_application` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `application_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '申请单号',
    `user_id` BIGINT NOT NULL,
    `enterprise_id` BIGINT COMMENT '企业ID',
    `enterprise_customer_id` BIGINT COMMENT '企业客户ID',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '申请金额',
    `term` INT NOT NULL COMMENT '借款期限(月)',
    `purpose` VARCHAR(100) COMMENT '借款用途',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待审核 1-审核中 2-通过 3-拒绝 4-取消',
    `reviewer_id` BIGINT COMMENT '审核人ID',
    `review_note` TEXT COMMENT '审核备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `reviewed_at` DATETIME COMMENT '审核时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_enterprise_id` (`enterprise_id`),
    INDEX `idx_loan_app_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='借贷申请表';

-- -----------------------------------------------------------
-- 5. 借贷合同表
-- -----------------------------------------------------------
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

-- -----------------------------------------------------------
-- 6. 还款计划表 (含V5逾期字段)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `repayment_plan` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `contract_id` BIGINT NOT NULL,
    `period` INT NOT NULL COMMENT '期数',
    `due_date` DATE NOT NULL COMMENT '应还日期',
    `principal` DECIMAL(12,2) NOT NULL COMMENT '应还本金',
    `interest` DECIMAL(12,2) NOT NULL COMMENT '应还利息',
    `total_amount` DECIMAL(12,2) NOT NULL COMMENT '应还总额',
    `penalty_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '罚息金额',
    `overdue_days` INT DEFAULT 0 COMMENT '逾期天数',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待还 1-已还 2-逾期',
    `paid_at` DATETIME COMMENT '实际还款时间',
    INDEX `idx_contract_id` (`contract_id`),
    INDEX `idx_due_date` (`due_date`),
    INDEX `idx_repayment_contract_status` (`contract_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='还款计划表';

-- -----------------------------------------------------------
-- 7. 风险评估记录表 (含V11追加字段)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `risk_assessment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `assessment_no` VARCHAR(32) NOT NULL UNIQUE,
    `user_id` BIGINT NOT NULL,
    `application_id` BIGINT COMMENT '关联的借贷申请',
    `assessment_type` TINYINT NOT NULL COMMENT '类型: 1-额度评估 2-借款评估 3-贷后监控',
    `risk_score` DECIMAL(5,2) NOT NULL COMMENT '风险评分 (0-100)',
    `risk_level` TINYINT NOT NULL COMMENT '风险等级: 1-低 2-中 3-高',
    `decision` VARCHAR(20) NOT NULL COMMENT '决策结果',
    `confidence` DOUBLE PRECISION COMMENT '置信度',
    `model_version` VARCHAR(50) COMMENT '模型版本',
    `feature_snapshot` JSON COMMENT '特征快照',
    `factors` JSON COMMENT '风险因子分析',
    `processing_time_ms` BIGINT COMMENT '处理耗时(毫秒)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_application_id` (`application_id`),
    INDEX `idx_risk_user_created` (`user_id`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风险评估记录表';

-- -----------------------------------------------------------
-- 8. 黑名单表
-- -----------------------------------------------------------
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

-- -----------------------------------------------------------
-- 9. 系统配置表 + 默认数据
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `config_key` VARCHAR(100) NOT NULL UNIQUE,
    `config_value` TEXT,
    `description` VARCHAR(255),
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

INSERT IGNORE INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('default_interest_rate', '0.12', '默认年利率'),
('min_loan_amount', '1000', '最小借款金额'),
('max_loan_amount', '100000', '最大借款金额'),
('min_loan_term', '3', '最小借款期限(月)'),
('max_loan_term', '24', '最大借款期限(月)');

-- -----------------------------------------------------------
-- 10. 数据采集记录表
-- -----------------------------------------------------------
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

-- -----------------------------------------------------------
-- 11. 管理员用户表 + 默认账号 (密码: admin123)
-- -----------------------------------------------------------
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

INSERT IGNORE INTO `admin_user` (`username`, `password_hash`, `real_name`, `role`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z9qA2Qv.C3p4Xl.j5O7WPBKS', '超级管理员', 'SUPER_ADMIN', 1);

-- -----------------------------------------------------------
-- 12. 操作审计日志表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT COMMENT '操作人ID',
    `user_type` VARCHAR(20) COMMENT '操作人类型: USER/ADMIN/ENTERPRISE',
    `username` VARCHAR(50) COMMENT '操作人用户名',
    `module` VARCHAR(50) COMMENT '业务模块',
    `operation` VARCHAR(100) COMMENT '操作类型',
    `target_type` VARCHAR(50) COMMENT '目标类型',
    `target_id` VARCHAR(50) COMMENT '目标ID',
    `detail` TEXT COMMENT '操作详情',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `trace_id` VARCHAR(32) COMMENT '链路追踪ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_module` (`module`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';

-- -----------------------------------------------------------
-- 13. 企业信息表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `enterprise` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `enterprise_no` VARCHAR(32) NOT NULL UNIQUE COMMENT '企业编号',
    `name` VARCHAR(100) NOT NULL COMMENT '企业名称',
    `unified_social_credit_code` VARCHAR(18) NOT NULL UNIQUE COMMENT '统一社会信用代码',
    `legal_person` VARCHAR(50) COMMENT '法人代表',
    `contact_phone` VARCHAR(20) COMMENT '联系电话',
    `enterprise_type` TINYINT DEFAULT 0 COMMENT '企业类型: 0-银行 1-小贷公司 2-其他',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-正常 1-禁用',
    `credit_limit` DECIMAL(15,2) DEFAULT 0 COMMENT '授信额度',
    `used_limit` DECIMAL(15,2) DEFAULT 0 COMMENT '已用额度',
    `api_key` VARCHAR(64) COMMENT 'API密钥',
    `expire_at` DATETIME COMMENT '过期时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_enterprise_no` (`enterprise_no`),
    INDEX `idx_credit_code` (`unified_social_credit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业信息表';

-- -----------------------------------------------------------
-- 14. 企业用户表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `enterprise_user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `enterprise_id` BIGINT NOT NULL COMMENT '企业ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `phone` VARCHAR(20) COMMENT '手机号',
    `role` TINYINT DEFAULT 0 COMMENT '角色: 0-操作员 1-管理员',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-正常 1-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_enterprise_username` (`enterprise_id`, `username`),
    INDEX `idx_enterprise_id` (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业用户表';

-- -----------------------------------------------------------
-- 15. 企业客户表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `enterprise_customer` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `enterprise_id` BIGINT NOT NULL COMMENT '企业ID',
    `customer_no` VARCHAR(32) NOT NULL COMMENT '客户编号',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `id_card` VARCHAR(18) NOT NULL COMMENT '身份证号',
    `phone` VARCHAR(20) COMMENT '手机号',
    `credit_score` INT COMMENT '信用评分',
    `risk_level` TINYINT COMMENT '风险等级: 0-低 1-中 2-高',
    `total_loan_count` INT DEFAULT 0 COMMENT '累计借款次数',
    `total_loan_amount` DECIMAL(15,2) DEFAULT 0 COMMENT '累计借款金额',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-正常 1-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_enterprise_customer` (`enterprise_id`, `id_card`),
    INDEX `idx_enterprise_id` (`enterprise_id`),
    INDEX `idx_customer_no` (`customer_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业客户表';

-- -----------------------------------------------------------
-- 16. 企业操作日志表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `enterprise_operation_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `enterprise_id` BIGINT NOT NULL COMMENT '企业ID',
    `user_id` BIGINT NOT NULL COMMENT '操作用户ID',
    `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `target_type` VARCHAR(50) COMMENT '目标类型',
    `target_id` BIGINT COMMENT '目标ID',
    `detail` TEXT COMMENT '操作详情JSON',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_enterprise_id` (`enterprise_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业操作日志表';

-- -----------------------------------------------------------
-- 17. 消息通知表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT COMMENT '接收人ID',
    `user_type` VARCHAR(20) COMMENT 'USER/ADMIN/ENTERPRISE',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` TEXT COMMENT '通知内容',
    `type` VARCHAR(30) COMMENT 'SYSTEM/RISK/LOAN/PAYMENT',
    `is_read` TINYINT DEFAULT 0 COMMENT '0=未读,1=已读',
    `related_id` VARCHAR(50) COMMENT '关联业务ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `read_at` DATETIME COMMENT '阅读时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_is_read` (`is_read`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_notification_user_read` (`user_id`, `is_read`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知';

-- -----------------------------------------------------------
-- 18. 银行账户表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `bank_account` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `bank_name` VARCHAR(50) NOT NULL COMMENT '银行名称',
    `account_no` VARCHAR(30) NOT NULL COMMENT '银行卡号',
    `account_name` VARCHAR(50) NOT NULL COMMENT '账户名',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认 0=否 1=是',
    `status` TINYINT DEFAULT 1 COMMENT '0=禁用 1=正常',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_bank_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行账户表';

-- -----------------------------------------------------------
-- 19. 放款记录表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `disbursement_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `contract_id` BIGINT NOT NULL,
    `application_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `amount` DECIMAL(12,2) NOT NULL,
    `bank_account_id` BIGINT NOT NULL,
    `status` TINYINT DEFAULT 0 COMMENT '0=待放款,1=放款中,2=已放款,3=放款失败',
    `transaction_no` VARCHAR(64) COMMENT '交易流水号',
    `completed_at` DATETIME,
    `failed_reason` VARCHAR(500),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_disbursement_contract` (`contract_id`),
    INDEX `idx_disbursement_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='放款记录表';

-- -----------------------------------------------------------
-- 20. 催收任务表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `collection_task` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `contract_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `overdue_amount` DECIMAL(12,2) NOT NULL,
    `overdue_days` INT NOT NULL,
    `collector_id` BIGINT COMMENT '催收员(管理员)ID',
    `status` TINYINT DEFAULT 0 COMMENT '0=待分配,1=已分配,2=处理中,3=已完成,4=已关闭',
    `priority` TINYINT DEFAULT 1 COMMENT '1=低,2=中,3=高',
    `deadline` DATETIME COMMENT '催收截止日',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_collection_contract` (`contract_id`),
    INDEX `idx_collection_status` (`status`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='催收任务表';

-- -----------------------------------------------------------
-- 21. 催收记录表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `collection_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL,
    `collector_id` BIGINT NOT NULL,
    `method` VARCHAR(20) NOT NULL COMMENT 'phone/sms/visit/legal',
    `content` TEXT COMMENT '催收内容',
    `result` VARCHAR(50) COMMENT 'promise_pay/refused/unreachable/other',
    `next_follow_up_date` DATE COMMENT '下次跟进日期',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_record_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='催收记录表';

-- -----------------------------------------------------------
-- 22. 合同模板表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `contract_template` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `type` VARCHAR(20) NOT NULL COMMENT 'personal/enterprise',
    `content` TEXT NOT NULL COMMENT '模板内容(HTML)',
    `version` VARCHAR(20) DEFAULT '1.0',
    `status` TINYINT DEFAULT 1 COMMENT '0=禁用 1=启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同模板表';

-- -----------------------------------------------------------
-- 企业测试数据
-- -----------------------------------------------------------
INSERT IGNORE INTO `enterprise` (`id`, `enterprise_no`, `name`, `unified_social_credit_code`, `legal_person`, `contact_phone`, `enterprise_type`, `status`, `credit_limit`, `used_limit`, `api_key`)
VALUES (1, 'ENT001', '测试企业有限公司', '91110000MA00ABCD1X', '张三', '13800138000', 0, 0, 1000000.00, 0, 'test_api_key_12345');

INSERT IGNORE INTO `enterprise_user` (`id`, `enterprise_id`, `username`, `password_hash`, `real_name`, `phone`, `role`, `status`)
VALUES (1, 1, 'admin', '$2a$10$EqKcp1WFKVQISheBxkV3FeYMmM8sOBfKXv6CKqTWFHdBqOQN3YVX2', '管理员', '13800138001', 1, 0);

INSERT IGNORE INTO `enterprise_user` (`id`, `enterprise_id`, `username`, `password_hash`, `real_name`, `phone`, `role`, `status`)
VALUES (2, 1, 'operator', '$2a$10$EqKcp1WFKVQISheBxkV3FeYMmM8sOBfKXv6CKqTWFHdBqOQN3YVX2', '操作员', '13800138002', 0, 0);
