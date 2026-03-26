-- Flyway V2: 企业模块Schema
-- 从 schema_enterprise.sql 和 data_enterprise.sql 迁移

-- 企业信息表
CREATE TABLE IF NOT EXISTS enterprise (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_no VARCHAR(32) NOT NULL UNIQUE COMMENT '企业编号',
    name VARCHAR(100) NOT NULL COMMENT '企业名称',
    unified_social_credit_code VARCHAR(18) NOT NULL UNIQUE COMMENT '统一社会信用代码',
    legal_person VARCHAR(50) COMMENT '法人代表',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    enterprise_type TINYINT DEFAULT 0 COMMENT '企业类型: 0-银行 1-小贷公司 2-其他',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-正常 1-禁用',
    credit_limit DECIMAL(15,2) DEFAULT 0 COMMENT '授信额度',
    used_limit DECIMAL(15,2) DEFAULT 0 COMMENT '已用额度',
    api_key VARCHAR(64) COMMENT 'API密钥',
    expire_at DATETIME COMMENT '过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_enterprise_no (enterprise_no),
    INDEX idx_credit_code (unified_social_credit_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业信息表';

-- 企业用户表 (企业管理员/操作员)
CREATE TABLE IF NOT EXISTS enterprise_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT NOT NULL COMMENT '企业ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    real_name VARCHAR(50) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    role TINYINT DEFAULT 0 COMMENT '角色: 0-操作员 1-管理员',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-正常 1-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_enterprise_username (enterprise_id, username),
    INDEX idx_enterprise_id (enterprise_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业用户表';

-- 企业客户表 (企业的借款客户)
CREATE TABLE IF NOT EXISTS enterprise_customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT NOT NULL COMMENT '企业ID',
    customer_no VARCHAR(32) NOT NULL COMMENT '客户编号',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    id_card VARCHAR(18) NOT NULL COMMENT '身份证号',
    phone VARCHAR(20) COMMENT '手机号',
    credit_score INT COMMENT '信用评分',
    risk_level TINYINT COMMENT '风险等级: 0-低 1-中 2-高',
    total_loan_count INT DEFAULT 0 COMMENT '累计借款次数',
    total_loan_amount DECIMAL(15,2) DEFAULT 0 COMMENT '累计借款金额',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-正常 1-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_enterprise_customer (enterprise_id, id_card),
    INDEX idx_enterprise_id (enterprise_id),
    INDEX idx_customer_no (customer_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业客户表';

-- 企业操作日志表
CREATE TABLE IF NOT EXISTS enterprise_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT NOT NULL COMMENT '企业ID',
    user_id BIGINT NOT NULL COMMENT '操作用户ID',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(50) COMMENT '目标类型',
    target_id BIGINT COMMENT '目标ID',
    detail TEXT COMMENT '操作详情JSON',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_enterprise_id (enterprise_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业操作日志表';

-- 扩展loan_application表（使用存储过程安全添加列）
SET @dbname = DATABASE();
SET @tablename = 'loan_application';
SET @columnname = 'enterprise_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_schema = @dbname)
      AND (table_name = @tablename)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @columnname, '` BIGINT COMMENT ''企业ID'' AFTER user_id')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @columnname = 'enterprise_customer_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_schema = @dbname)
      AND (table_name = @tablename)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @columnname, '` BIGINT COMMENT ''企业客户ID'' AFTER enterprise_id')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @indexname = 'idx_enterprise_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_schema = @dbname)
      AND (table_name = @tablename)
      AND (index_name = @indexname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE `', @tablename, '` ADD INDEX `', @indexname, '` (`enterprise_id`)')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 企业测试数据初始化
INSERT IGNORE INTO enterprise (id, enterprise_no, name, unified_social_credit_code, legal_person, contact_phone, enterprise_type, status, credit_limit, used_limit, api_key)
VALUES (1, 'ENT001', '测试企业有限公司', '91110000MA00ABCD1X', '张三', '13800138000', 0, 0, 1000000.00, 0, 'test_api_key_12345');

INSERT IGNORE INTO enterprise_user (id, enterprise_id, username, password_hash, real_name, phone, role, status)
VALUES (1, 1, 'admin', '$2a$10$EqKcp1WFKVQISheBxkV3FeYMmM8sOBfKXv6CKqTWFHdBqOQN3YVX2', '管理员', '13800138001', 1, 0);

INSERT IGNORE INTO enterprise_user (id, enterprise_id, username, password_hash, real_name, phone, role, status)
VALUES (2, 1, 'operator', '$2a$10$EqKcp1WFKVQISheBxkV3FeYMmM8sOBfKXv6CKqTWFHdBqOQN3YVX2', '操作员', '13800138002', 0, 0);
