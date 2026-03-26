package com.xindai.xindai.common.exception;

import lombok.Getter;

/**
 * 统一错误码枚举
 */
@Getter
public enum ErrorCode {

    // 成功
    SUCCESS(200, "操作成功"),

    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),

    // 系统错误 5xx
    INTERNAL_ERROR(500, "系统内部错误"),

    // 用户模块 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "用户已禁用"),
    PHONE_EXISTS(1003, "手机号已注册"),
    PASSWORD_ERROR(1004, "密码错误"),

    // 借贷模块 2xxx
    LOAN_NOT_FOUND(2001, "借款申请不存在"),
    LIMIT_INSUFFICIENT(2002, "可用额度不足"),
    APPLICATION_NOT_FOUND(2003, "申请不存在"),
    APPLICATION_ALREADY_REVIEWED(2004, "申请已审核"),
    CONTRACT_NOT_FOUND(2005, "合同不存在"),
    CONTRACT_ALREADY_SETTLED(2006, "合同已结清"),
    NO_PENDING_REPAYMENT(2007, "没有待还款项"),
    REPAYMENT_PLAN_NOT_FOUND(2008, "还款计划不存在"),
    REPAYMENT_ALREADY_PAID(2009, "该期已还款"),
    LIMIT_INCREASE_TOO_MUCH(2010, "提额申请金额过大"),
    LIMIT_INCREASE_REJECTED(2011, "提额申请被拒绝"),
    BANK_ACCOUNT_NOT_FOUND(2012, "银行卡不存在"),
    BANK_ACCOUNT_LIMIT_EXCEEDED(2013, "银行卡数量已达上限"),
    DISBURSEMENT_NOT_FOUND(2014, "放款记录不存在"),
    DISBURSEMENT_INVALID_STATUS(2015, "放款记录状态异常"),

    // 风控模块 3xxx
    RISK_ASSESSMENT_FAILED(3001, "风险评估失败"),
    IN_BLACKLIST(3002, "用户在黑名单中"),
    BLACKLIST_ALREADY_EXISTS(3003, "黑名单记录已存在"),
    BLACKLIST_NOT_FOUND(3004, "黑名单记录不存在"),
    KYC_VERIFICATION_FAILED(3005, "实名认证失败"),

    // 催收模块 6xxx
    COLLECTION_TASK_NOT_FOUND(6001, "催收任务不存在"),
    COLLECTION_TASK_STATUS_INVALID(6002, "催收任务状态不允许此操作"),

    // 企业模块 4xxx
    ENTERPRISE_NOT_FOUND(4001, "企业不存在"),
    ENTERPRISE_DISABLED(4002, "企业已禁用"),
    ENTERPRISE_USER_NOT_FOUND(4003, "企业用户不存在"),
    ENTERPRISE_CUSTOMER_NOT_FOUND(4004, "企业客户不存在"),
    ENTERPRISE_CREDIT_LIMIT_EXCEEDED(4005, "企业授信额度不足"),
    ENTERPRISE_CUSTOMER_EXISTS(4006, "企业客户已存在"),
    ENTERPRISE_CREDIT_APPLY_PENDING(4007, "存在待审批的额度申请"),
    ENTERPRISE_CREDIT_APPLY_REJECTED(4008, "额度申请被拒绝，请30天后重试"),

    // AI助手模块 5xxx
    AGENT_DISABLED(5001, "AI助手功能未启用"),
    AGENT_SERVICE_ERROR(5002, "AI服务调用失败"),
    AGENT_TIMEOUT(5003, "AI响应超时"),
    AGENT_RATE_LIMITED(5004, "AI请求过于频繁"),
    AGENT_CONTEXT_ERROR(5005, "AI上下文错误");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
