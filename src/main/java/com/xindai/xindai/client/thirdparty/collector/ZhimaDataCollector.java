package com.xindai.xindai.client.thirdparty.collector;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.xindai.xindai.client.thirdparty.DataCollector;
import com.xindai.xindai.client.thirdparty.ThirdPartyConfig;
import com.xindai.xindai.client.thirdparty.dto.ThirdPartyData;
import com.xindai.xindai.client.thirdparty.dto.ZhimaCreditData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZhimaDataCollector implements DataCollector {

    private final ThirdPartyConfig thirdPartyConfig;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "thirdparty:zhima:";
    private static final long CACHE_TTL = 12;  // 12小时缓存

    @Override
    public String getSourceType() {
        return "ZHIMA_CREDIT";
    }

    @Override
    public ThirdPartyData collect(Long userId, String phone, String idCard) {
        long startTime = System.currentTimeMillis();
        ThirdPartyData result = new ThirdPartyData();
        result.setSourceType(getSourceType());
        result.setCollectTime(LocalDateTime.now());

        if (!thirdPartyConfig.getZhima().isEnabled()) {
            result.setSuccess(false);
            result.setErrorMessage("芝麻信用服务未启用");
            return result;
        }

        String cacheKey = CACHE_PREFIX + idCard;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (ThirdPartyData) cached;
        }

        try {
            ThirdPartyData data = callZhimaApi(phone, idCard);
            data.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            redisTemplate.opsForValue().set(cacheKey, data, CACHE_TTL, TimeUnit.HOURS);
            return data;

        } catch (Exception e) {
            log.error("Failed to collect zhima data: {}", e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    @Override
    public boolean isAvailable() {
        return thirdPartyConfig.getZhima().isEnabled();
    }

    private ThirdPartyData callZhimaApi(String phone, String idCard) {
        // 模拟芝麻信用 API 调用
        ThirdPartyData result = new ThirdPartyData();
        result.setSourceType(getSourceType());
        result.setSuccess(true);

        ZhimaCreditData zhimaData = new ZhimaCreditData();
        zhimaData.setZhimaScore(650 + (int) (Math.random() * 150));
        zhimaData.setCreditLevel("B");
        zhimaData.setIdVerified(true);
        zhimaData.setPhoneVerified(true);
        zhimaData.setBankCardVerified(true);
        zhimaData.setFraudRisk(false);
        zhimaData.setFraudScore(new BigDecimal("0.15"));
        zhimaData.setBehaviorScore(new BigDecimal("0.75"));
        zhimaData.setStabilityScore(new BigDecimal("0.80"));

        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("zhimaScore", zhimaData.getZhimaScore());
        parsedData.put("creditLevel", zhimaData.getCreditLevel());
        parsedData.put("idVerified", zhimaData.getIdVerified());
        parsedData.put("fraudRisk", zhimaData.getFraudRisk());
        parsedData.put("behaviorScore", zhimaData.getBehaviorScore());

        result.setParsedData(parsedData);
        result.setCollectTime(LocalDateTime.now());

        return result;
    }
}
