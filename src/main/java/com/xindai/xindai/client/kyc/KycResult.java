package com.xindai.xindai.client.kyc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KYC认证结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KycResult {

    /**
     * 认证是否成功
     */
    private boolean success;

    /**
     * 认证结果消息
     */
    private String message;
}
