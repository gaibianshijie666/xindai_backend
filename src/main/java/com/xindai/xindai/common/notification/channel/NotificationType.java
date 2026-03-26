package com.xindai.xindai.common.notification.channel;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {

    APPLICATION_SUBMITTED("APPLICATION_SUBMITTED", "申请已提交"),
    RISK_COMPLETED("RISK_COMPLETED", "风控评估完成"),
    LOAN_APPROVED("LOAN_APPROVED", "贷款审批通过"),
    LOAN_REJECTED("LOAN_REJECTED", "贷款审批拒绝"),
    DISBURSEMENT_COMPLETED("DISBURSEMENT_COMPLETED", "放款完成"),
    REPAYMENT_REMINDER("REPAYMENT_REMINDER", "还款提醒"),
    REPAYMENT_COMPLETED("REPAYMENT_COMPLETED", "还款完成"),
    OVERDUE_NOTICE("OVERDUE_NOTICE", "逾期通知"),
    COLLECTION_NOTICE("COLLECTION_NOTICE", "催收通知"),
    CONTRACT_GENERATED("CONTRACT_GENERATED", "合同生成");

    private final String code;
    private final String desc;
}
