-- 用户画像表新增字段迁移脚本
-- 用于支持智能信用额度计算

-- 添加信用等级字段
ALTER TABLE `user_profile` ADD COLUMN `credit_grade` CHAR(1) COMMENT '信用等级 (A-G)' AFTER `credit_score`;

-- 添加年收入字段
ALTER TABLE `user_profile` ADD COLUMN `annual_income` DECIMAL(12,2) COMMENT '年收入' AFTER `credit_grade`;

-- 添加债务收入比字段
ALTER TABLE `user_profile` ADD COLUMN `dti` DECIMAL(5,4) COMMENT '债务收入比 (0-1)' AFTER `annual_income`;

-- 添加就业年限字段
ALTER TABLE `user_profile` ADD COLUMN `employment_years` INT COMMENT '就业年限' AFTER `dti`;

-- 添加风险评分字段
ALTER TABLE `user_profile` ADD COLUMN `risk_score` DECIMAL(5,2) COMMENT '风险评分 (0-100)' AFTER `employment_years`;

-- 为新字段添加索引
CREATE INDEX `idx_credit_grade` ON `user_profile` (`credit_grade`);
CREATE INDEX `idx_risk_score` ON `user_profile` (`risk_score`);
