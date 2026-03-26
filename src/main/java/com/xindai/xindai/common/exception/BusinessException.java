package com.xindai.xindai.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;
    private final String message;
    private final HttpStatus httpStatus;

    /**
     * 使用 ErrorCode 枚举创建异常（推荐）
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.httpStatus = mapCodeToHttpStatus(errorCode.getCode());
    }

    /**
     * 使用 ErrorCode + 详细信息创建异常
     */
    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + "：" + detail);
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage() + "：" + detail;
        this.httpStatus = mapCodeToHttpStatus(errorCode.getCode());
    }

    /**
     * 直接使用消息创建异常（简单场景）
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * 使用 code + message 创建异常
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
        this.httpStatus = mapCodeToHttpStatus(code);
    }

    /**
     * 使用 message + HttpStatus 创建异常
     */
    public BusinessException(String message, HttpStatus httpStatus) {
        super(message);
        this.code = httpStatus.value();
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * 根据业务错误码映射 HTTP 状态码
     */
    private static HttpStatus mapCodeToHttpStatus(Integer code) {
        if (code == null) return HttpStatus.INTERNAL_SERVER_ERROR;

        // HTTP 标准状态码直接返回
        if (code >= 400 && code < 500) {
            return HttpStatus.valueOf(code);
        }

        // 业务错误码映射
        return switch (code / 1000) {
            case 1 -> HttpStatus.BAD_REQUEST;      // 用户模块错误
            case 2 -> HttpStatus.BAD_REQUEST;      // 借贷模块错误
            case 3 -> HttpStatus.BAD_REQUEST;      // 风控模块错误
            case 4 -> HttpStatus.BAD_REQUEST;      // 企业模块错误
            case 5 -> HttpStatus.INTERNAL_SERVER_ERROR; // AI助手模块错误
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
