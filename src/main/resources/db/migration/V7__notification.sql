CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '接收人ID',
    user_type VARCHAR(20) COMMENT 'USER/ADMIN/ENTERPRISE',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    type VARCHAR(30) COMMENT 'SYSTEM/RISK/LOAN/PAYMENT',
    is_read TINYINT DEFAULT 0 COMMENT '0=未读,1=已读',
    related_id VARCHAR(50) COMMENT '关联业务ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME COMMENT '阅读时间',
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知';
