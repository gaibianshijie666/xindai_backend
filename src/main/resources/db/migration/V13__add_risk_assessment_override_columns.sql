-- ============================================================
-- V13: 添加风险评估覆盖决策相关字段
-- ============================================================

-- 添加风险评估覆盖决策字段
ALTER TABLE `risk_assessment`
ADD COLUMN IF NOT EXISTS `overridden` TINYINT(1) DEFAULT 0 COMMENT '是否已被覆盖',
ADD COLUMN IF NOT EXISTS `override_decision` VARCHAR(20) COMMENT '覆盖决策: APPROVE, REJECT',
ADD COLUMN IF NOT EXISTS `override_reason` VARCHAR(500) COMMENT '覆盖原因',
ADD COLUMN IF NOT EXISTS `override_by` BIGINT COMMENT '覆盖人ID',
ADD COLUMN IF NOT EXISTS `override_at` DATETIME COMMENT '覆盖时间',
ADD INDEX IF NOT EXISTS `idx_overridden` (`overridden`);
