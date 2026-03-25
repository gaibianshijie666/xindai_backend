CREATE INDEX idx_loan_app_user_status ON loan_application(user_id, status);
CREATE INDEX idx_repayment_contract_status ON repayment_plan(contract_id, status);
CREATE INDEX idx_risk_user_created ON risk_assessment(user_id, created_at DESC);
CREATE INDEX idx_notification_user_read ON notification(user_id, is_read, created_at DESC);
