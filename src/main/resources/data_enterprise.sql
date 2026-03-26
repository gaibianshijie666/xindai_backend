-- 企业测试数据初始化
-- 密码: password123 (BCrypt加密: $2a$10$EqKcp1WFKVQISheBxkV3FeYMmM8sOBfKXv6CKqTWFHdBqOQN3YVX2)

-- 插入企业
INSERT INTO enterprise (id, enterprise_no, name, unified_social_credit_code, legal_person, contact_phone, enterprise_type, status, credit_limit, used_limit, api_key)
VALUES (1, 'ENT001', '测试企业有限公司', '91110000MA00ABCD1X', '张三', '13800138000', 0, 0, 1000000.00, 0, 'test_api_key_12345');

-- 插入企业管理员用户 (密码: password123)
INSERT INTO enterprise_user (id, enterprise_id, username, password_hash, real_name, phone, role, status)
VALUES (1, 1, 'admin', '$2a$10$EqKcp1WFKVQISheBxkV3FeYMmM8sOBfKXv6CKqTWFHdBqOQN3YVX2', '管理员', '13800138001', 1, 0);

-- 插入企业操作员用户 (密码: password123)
INSERT INTO enterprise_user (id, enterprise_id, username, password_hash, real_name, phone, role, status)
VALUES (2, 1, 'operator', '$2a$10$EqKcp1WFKVQISheBxkV3FeYMmM8sOBfKXv6CKqTWFHdBqOQN3YVX2', '操作员', '13800138002', 0, 0);
