package com.xindai.xindai.client.thirdparty.collector;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xindai.xindai.client.thirdparty.DataCollector;
import com.xindai.xindai.client.thirdparty.ThirdPartyConfig;
import com.xindai.xindai.client.thirdparty.dto.CreditReportData;
import com.xindai.xindai.client.thirdparty.dto.ThirdPartyData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreditDataCollector implements DataCollector {

    private final ThirdPartyConfig thirdPartyConfig;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "thirdparty:credit:";
    private static final long CACHE_TTL = 24;  // 24小时缓存

    @Override
    public String getSourceType() {
        return "CREDIT_BUREAU";
    }

    @Override
    public ThirdPartyData collect(Long userId, String phone, String idCard) {
        long startTime = System.currentTimeMillis();
        ThirdPartyData result = new ThirdPartyData();
        result.setSourceType(getSourceType());
        result.setCollectTime(LocalDateTime.now());

        // 检查是否启用
        if (!thirdPartyConfig.getCredit().isEnabled()) {
            result.setSuccess(false);
            result.setErrorMessage("征信服务未启用");
            return result;
        }

        // 检查缓存
        String cacheKey = CACHE_PREFIX + idCard;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("Using cached credit data for idCard: {}", maskIdCard(idCard));
            return (ThirdPartyData) cached;
        }

        try {
            // 调用征信 API (模拟)
            ThirdPartyData data = callCreditApi(phone, idCard);
            data.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            // 缓存结果
            redisTemplate.opsForValue().set(cacheKey, data, CACHE_TTL, TimeUnit.HOURS);

            return data;

        } catch (Exception e) {
            log.error("Failed to collect credit data: {}", e.getMessage());
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return result;
        }
    }

    @Override
    public boolean isAvailable() {
        if (!thirdPartyConfig.getCredit().isEnabled()) {
            return false;
        }
        // TODO: 实际调用健康检查接口
        return true;
    }

    private ThirdPartyData callCreditApi(String phone, String idCard) {
        ThirdPartyConfig.CreditConfig config = thirdPartyConfig.getCredit();

        // 构建请求
        Map<String, Object> request = new HashMap<>();
        request.put("requestId", IdUtil.fastSimpleUUID());
        request.put("idCard", idCard);
        request.put("phone", phone);
        request.put("timestamp", System.currentTimeMillis());

        // 模拟 API 调用
        // 实际项目中应该调用真实的征信 API
        ThirdPartyData result = new ThirdPartyData();
        result.setSourceType(getSourceType());
        result.setSuccess(true);

        // 模拟解析后的征信数据
        CreditReportData creditData = new CreditReportData();
        creditData.setReportNo("CR" + IdUtil.getSnowflakeNextIdStr());
        creditData.setCreditScore(680 + (int) (Math.random() * 100));
        creditData.setCreditLevel("B");
        creditData.setTotalDebt(new java.math.BigDecimal("50000"));
        creditData.setDebtRatio(new java.math.BigDecimal("0.35"));
        creditData.setAccountCount(3);
        creditData.setOverdueCount(0);
        creditData.setInquiryCount6m(2);

        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("creditScore", creditData.getCreditScore());
        parsedData.put("creditLevel", creditData.getCreditLevel());
        parsedData.put("debtRatio", creditData.getDebtRatio());
        parsedData.put("overdueCount", creditData.getOverdueCount());

        result.setParsedData(parsedData);
        result.setCollectTime(LocalDateTime.now());

        return result;
    }

    /**
     * 身份证号脱敏
     */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return "***";
        }
        // 保留前3位和后4位，中间用*代替
        return idCard.substring(0, 3) + "********" + idCard.substring(idCard.length() - 4);
    }
}
