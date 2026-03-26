CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '操作人ID',
    user_type VARCHAR(20) COMMENT '操作人类型: USER/ADMIN/ENTERPRISE',
    username VARCHAR(50) COMMENT '操作人用户名',
    module VARCHAR(50) COMMENT '业务模块',
    operation VARCHAR(100) COMMENT '操作类型',
    target_type VARCHAR(50) COMMENT '目标类型',
    target_id VARCHAR(50) COMMENT '目标ID',
    detail TEXT COMMENT '操作详情',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    trace_id VARCHAR(32) COMMENT '链路追踪ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_module (module),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';
