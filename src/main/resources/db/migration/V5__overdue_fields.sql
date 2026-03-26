-- Add overdue-related columns to repayment_plan table

SET @dbname = DATABASE();
SET @tablename = 'repayment_plan';

-- Add penalty_amount column
SET @columnname = 'penalty_amount';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE repayment_plan ADD COLUMN penalty_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT ''罚息金额'''
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add overdue_days column
SET @columnname = 'overdue_days';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE repayment_plan ADD COLUMN overdue_days INT DEFAULT 0 COMMENT ''逾期天数'''
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
