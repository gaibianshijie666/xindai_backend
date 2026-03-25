CREATE TABLE IF NOT EXISTS collection_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    overdue_amount DECIMAL(12,2) NOT NULL,
    overdue_days INT NOT NULL,
    collector_id BIGINT COMMENT '催收员(管理员)ID',
    status TINYINT DEFAULT 0 COMMENT '0=待分配,1=已分配,2=处理中,3=已完成,4=已关闭',
    priority TINYINT DEFAULT 1 COMMENT '1=低,2=中,3=高',
    deadline DATETIME COMMENT '催收截止日',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_collection_contract (contract_id),
    INDEX idx_collection_status (status, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS collection_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    collector_id BIGINT NOT NULL,
    method VARCHAR(20) NOT NULL COMMENT 'phone/sms/visit/legal',
    content TEXT COMMENT '催收内容',
    result VARCHAR(50) COMMENT 'promise_pay/refused/unreachable/other',
    next_follow_up_date DATE COMMENT '下次跟进日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_record_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
