package com.xindai.xindai.client.thirdparty.collector;

import cn.hutool.core.util.IdUtil;
import com.xindai.xindai.client.thirdparty.DataCollector;
import com.xindai.xindai.client.thirdparty.ThirdPartyConfig;
import com.xindai.xindai.client.thirdparty.dto.CarrierData;
import com.xindai.xindai.client.thirdparty.dto.ThirdPartyData;
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
public class CarrierDataCollector implements DataCollector {

    private final ThirdPartyConfig thirdPartyConfig;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "thirdparty:carrier:";
    private static final long CACHE_TTL = 72;  // 72小时缓存

    @Override
    public String getSourceType() {
        return "CARRIER";
    }

    @Override
    public ThirdPartyData collect(Long userId, String phone, String idCard) {
        long startTime = System.currentTimeMillis();
        ThirdPartyData result = new ThirdPartyData();
        result.setSourceType(getSourceType());
        result.setCollectTime(LocalDateTime.now());

        if (!thirdPartyConfig.getCarrier().isEnabled()) {
            result.setSuccess(false);
            result.setErrorMessage("运营商服务未启用");
            return result;
        }

        String cacheKey = CACHE_PREFIX + phone;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (ThirdPartyData) cached;
        }

        try {
            ThirdPartyData data = callCarrierApi(phone);
            data.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            redisTemplate.opsForValue().set(cacheKey, data, CACHE_TTL, TimeUnit.HOURS);
            return data;

        } catch (Exception e) {
            log.error("Failed to collect carrier data: {}", e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    @Override
    public boolean isAvailable() {
        return thirdPartyConfig.getCarrier().isEnabled();
    }

    private ThirdPartyData callCarrierApi(String phone) {
        // 模拟运营商 API 调用
        ThirdPartyData result = new ThirdPartyData();
        result.setSourceType(getSourceType());
        result.setSuccess(true);

        CarrierData carrierData = new CarrierData();
        carrierData.setPhone(phone);
        carrierData.setCarrier("中国移动");
        carrierData.setNetworkAge(36 + (int) (Math.random() * 60));
        carrierData.setNetworkAgeScore(new BigDecimal("0.85"));
        carrierData.setCallCount30d(100 + (int) (Math.random() * 200));
        carrierData.setLocationStability(new BigDecimal("0.75"));
        carrierData.setContactCount(200 + (int) (Math.random() * 300));

        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("networkAge", carrierData.getNetworkAge());
        parsedData.put("networkAgeScore", carrierData.getNetworkAgeScore());
        parsedData.put("callCount30d", carrierData.getCallCount30d());
        parsedData.put("locationStability", carrierData.getLocationStability());

        result.setParsedData(parsedData);
        result.setCollectTime(LocalDateTime.now());

        return result;
    }
}
