package com.xindai.xindai.client.kyc;

/**
 * KYC实名认证服务接口
 */
public interface KycService {

    /**
     * 实名认证验证
     *
     * @param realName 真实姓名
     * @param idCard   身份证号
     * @return 认证结果
     */
    KycResult verify(String realName, String idCard);
}
