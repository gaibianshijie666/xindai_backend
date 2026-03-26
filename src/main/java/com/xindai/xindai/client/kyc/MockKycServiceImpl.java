package com.xindai.xindai.client.kyc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Mock KYC实名认证服务实现
 * 用于开发和测试环境
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "kyc.mock.enabled", havingValue = "true", matchIfMissing = true)
public class MockKycServiceImpl implements KycService {

    private static final String ID_CARD_PATTERN = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$";
    private static final Random RANDOM = new Random();

    @Override
    public KycResult verify(String realName, String idCard) {
        log.info("Mock KYC verification: realName={}, idCard={}****{}", realName, idCard.substring(0, 4), idCard.substring(idCard.length() - 4));

        // 验证姓名长度
        if (realName == null || realName.isBlank()) {
            log.warn("KYC verification failed: empty realName");
            return new KycResult(false, "姓名不能为空");
        }
        if (realName.length() < 2) {
            log.warn("KYC verification failed: realName too short");
            return new KycResult(false, "姓名长度不能少于2个字符");
        }

        // 验证身份证号格式（18位）
        if (idCard == null || !idCard.matches(ID_CARD_PATTERN)) {
            log.warn("KYC verification failed: invalid idCard format");
            return new KycResult(false, "身份证号格式不正确");
        }

        // 90% 成功率模拟
        boolean success = RANDOM.nextInt(100) < 90;
        if (success) {
            log.info("Mock KYC verification passed for realName={}", realName);
            return new KycResult(true, "实名认证通过");
        } else {
            log.info("Mock KYC verification failed for realName={}", realName);
            return new KycResult(false, "实名认证失败：姓名与身份证号不匹配");
        }
    }
}
