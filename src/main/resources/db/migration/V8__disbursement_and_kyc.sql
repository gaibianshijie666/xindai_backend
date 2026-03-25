-- V8: Disbursement tables + KYC fields on user_profile
ALTER TABLE user_profile ADD COLUMN identity_status TINYINT DEFAULT 0 COMMENT '0=未认证,1=认证中,2=已认证,3=认证失败';
ALTER TABLE user_profile ADD COLUMN identity_verified_at DATETIME NULL;

CREATE TABLE IF NOT EXISTS bank_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    bank_name VARCHAR(50) NOT NULL COMMENT '银行名称',
    account_no VARCHAR(30) NOT NULL COMMENT '银行卡号',
    account_name VARCHAR(50) NOT NULL COMMENT '账户名',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认 0=否 1=是',
    status TINYINT DEFAULT 1 COMMENT '0=禁用 1=正常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bank_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS disbursement_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    bank_account_id BIGINT NOT NULL,
    status TINYINT DEFAULT 0 COMMENT '0=待放款,1=放款中,2=已放款,3=放款失败',
    transaction_no VARCHAR(64) COMMENT '交易流水号',
    completed_at DATETIME,
    failed_reason VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_disbursement_contract (contract_id),
    INDEX idx_disbursement_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
