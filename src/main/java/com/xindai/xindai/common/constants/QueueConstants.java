package com.xindai.xindai.common.constants;

public final class QueueConstants {

    public static final String EXCHANGE = "xindai.direct";
    public static final String DLX_EXCHANGE = "xindai.dlx";

    public static final String LOAN_APPLICATION_SUBMITTED = "loan.application.submitted";
    public static final String LOAN_APPLICATION_APPROVED = "loan.application.approved";
    public static final String LOAN_APPLICATION_REJECTED = "loan.application.rejected";
    public static final String LOAN_REPAYMENT_COMPLETED = "loan.repayment.completed";
    public static final String LOAN_OVERDUE_DETECTED = "loan.overdue.detected";
    public static final String RISK_ASSESSMENT_COMPLETED = "risk.assessment.completed";

    private QueueConstants() {
    }
}
