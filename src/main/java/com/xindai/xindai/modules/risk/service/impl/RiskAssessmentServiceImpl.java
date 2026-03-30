package com.xindai.xindai.modules.risk.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.client.model.ModelServiceClient;
import com.xindai.xindai.client.model.dto.PredictResponse;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.config.CreditLimitProperties;
import com.xindai.xindai.modules.risk.feature.FeatureAggregationService;
import com.xindai.xindai.modules.risk.entity.Blacklist;
import com.xindai.xindai.modules.risk.entity.RiskAssessment;
import com.xindai.xindai.modules.risk.mapper.BlacklistMapper;
import com.xindai.xindai.modules.risk.mapper.RiskAssessmentMapper;
import com.xindai.xindai.modules.risk.dto.RiskAssessmentVO;
import com.xindai.xindai.modules.risk.service.RiskAssessmentService;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.entity.UserProfile;
import com.xindai.xindai.modules.user.service.UserProfileDetailService;
import com.xindai.xindai.modules.user.service.UserProfileService;
import com.xindai.xindai.modules.loan.entity.LoanApplication;
import com.xindai.xindai.modules.loan.mapper.LoanApplicationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAssessmentServiceImpl implements RiskAssessmentService {

    private final ModelServiceClient modelServiceClient;
    private final RiskAssessmentMapper riskAssessmentMapper;
    private final BlacklistMapper blacklistMapper;
    private final UserProfileService userProfileService;
    private final UserProfileDetailService userProfileDetailService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final FeatureAggregationService featureAggregationService;
    private final LoanApplicationMapper loanApplicationMapper;
    private final CreditLimitProperties creditLimitProperties;

    private static final String RISK_LOCK_PREFIX = "lock:risk:";
    private static final long LOCK_TIMEOUT = 30;

    /**
     * Lua script for atomic lock release: only the lock holder can release
     */
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    // ========================================================================
    // assess - Main risk assessment entry point
    // ========================================================================

    @Override
    public RiskAssessment assess(Long userId, Long applicationId, Integer assessmentType) {
        String lockKey = RISK_LOCK_PREFIX + userId;
        String lockValue = IdUtil.fastSimpleUUID();
        Boolean locked = false;

        try {
            // 1. Acquire distributed lock
            locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, LOCK_TIMEOUT, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(locked)) {
                throw new BusinessException("风险评估正在进行中，请稍后重试");
            }

            // 2. Fetch user
            User user = userProfileService.getById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }

            // 3. Check blacklist
            if (checkBlacklist(user)) {
                return createRejectAssessment(userId, applicationId, assessmentType, "用户在黑名单中");
            }

            // 4. Collect features (base features from aggregation service)
            UserProfile profile = userProfileDetailService.getUserProfileByUserId(userId);
            Map<String, Float> features = featureAggregationService.collectFeatures(userId, user, profile);

            // 5. Inject loan application features (loan_amount, loan_term, loan_interest_rate)
            injectLoanFeatures(features, applicationId, profile);

            // 6. Call model service
            long startTime = System.currentTimeMillis();
            PredictResponse response = modelServiceClient.predict(userId.toString(), features);
            long processingTime = System.currentTimeMillis() - startTime;

            // 7. Null-safe extraction of model response fields
            if (response == null) {
                throw new BusinessException("模型服务响应异常");
            }

            Double riskScore = response.getRiskScore();
            Integer riskLevel = response.getRiskLevel();
            String decision = response.getDecision();
            Double confidence = response.getConfidence();

            if (riskScore == null || riskLevel == null || decision == null) {
                log.warn("Model response missing critical fields for user {}: score={}, level={}, decision={}",
                        userId, riskScore, riskLevel, decision);
                riskScore = 50.0;
                riskLevel = 2;
                decision = "MANUAL_REVIEW";
            }

            // 8. Build and persist assessment
            RiskAssessment assessment = new RiskAssessment();
            assessment.setAssessmentNo(generateAssessmentNo());
            assessment.setUserId(userId);
            assessment.setApplicationId(applicationId);
            assessment.setAssessmentType(assessmentType);
            assessment.setRiskScore(BigDecimal.valueOf(riskScore));
            assessment.setRiskLevel(riskLevel);
            assessment.setDecision(decision);
            assessment.setModelVersion(response.getModelVersion());
            assessment.setProcessingTimeMs((int) processingTime);
            assessment.setCreatedAt(LocalDateTime.now());

            // Persist confidence from model response
            if (confidence != null) {
                assessment.setConfidence(BigDecimal.valueOf(confidence));
            }

            // Store feature snapshot
            assessment.setFeatureSnapshot(convertFeaturesToSnapshot(features));

            // Store factors if available
            List<Map<String, Object>> factors = response.getFactors();
            if (factors != null && !factors.isEmpty()) {
                Map<String, Object> factorsMap = new HashMap<>();
                factorsMap.put("factors", factors);
                assessment.setFactors(factorsMap);
            }

            riskAssessmentMapper.insert(assessment);
            log.info("Risk assessment completed for user {}: score={}, level={}, decision={}, confidence={}",
                    userId, riskScore, riskLevel, decision, confidence);

            return assessment;

        } finally {
            // Always release lock
            if (Boolean.TRUE.equals(locked)) {
                releaseLock(lockKey, lockValue);
            }
        }
    }

    // ========================================================================
    // Query methods
    // ========================================================================

    @Override
    public Page<RiskAssessment> getAssessmentHistory(Long userId, int page, int size) {
        Page<RiskAssessment> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<RiskAssessment>()
                .eq(RiskAssessment::getUserId, userId)
                .orderByDesc(RiskAssessment::getCreatedAt);
        return riskAssessmentMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public RiskAssessment getByAssessmentNo(String assessmentNo) {
        LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<RiskAssessment>()
                .eq(RiskAssessment::getAssessmentNo, assessmentNo);
        return riskAssessmentMapper.selectOne(wrapper);
    }

    @Override
    public List<RiskAssessment> getAssessmentsSince(LocalDateTime since) {
        LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<RiskAssessment>()
                .ge(RiskAssessment::getCreatedAt, since)
                .orderByDesc(RiskAssessment::getCreatedAt);
        return riskAssessmentMapper.selectList(wrapper);
    }

    // ========================================================================
    // VO conversion
    // ========================================================================

    @Override
    public RiskAssessmentVO toVO(RiskAssessment assessment) {
        if (assessment == null) {
            return null;
        }
        RiskAssessmentVO vo = new RiskAssessmentVO();
        vo.setId(assessment.getId());
        vo.setAssessmentNo(assessment.getAssessmentNo());
        vo.setUserId(assessment.getUserId());
        vo.setApplicationId(assessment.getApplicationId());
        vo.setAssessmentType(assessment.getAssessmentType());
        vo.setRiskScore(assessment.getRiskScore());
        vo.setRiskLevel(assessment.getRiskLevel());
        vo.setDecision(assessment.getDecision());
        vo.setModelVersion(assessment.getModelVersion());
        vo.setFactors(assessment.getFactors());
        vo.setProcessingTimeMs(assessment.getProcessingTimeMs());
        vo.setCreatedAt(assessment.getCreatedAt());
        return vo;
    }

    @Override
    public List<RiskAssessmentVO> toVOList(List<RiskAssessment> assessments) {
        if (assessments == null) {
            return List.of();
        }
        return assessments.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // Internal helpers
    // ========================================================================

    /**
     * Check if user is on the blacklist (by phone or ID card).
     */
    private boolean checkBlacklist(User user) {
        // Check phone
        if (user.getPhone() != null) {
            LambdaQueryWrapper<Blacklist> phoneQuery = new LambdaQueryWrapper<Blacklist>()
                    .eq(Blacklist::getType, 1)
                    .eq(Blacklist::getValue, user.getPhone());
            if (blacklistMapper.selectOne(phoneQuery) != null) {
                return true;
            }
        }

        // Check ID card
        if (user.getIdCard() != null) {
            LambdaQueryWrapper<Blacklist> idCardQuery = new LambdaQueryWrapper<Blacklist>()
                    .eq(Blacklist::getType, 2)
                    .eq(Blacklist::getValue, user.getIdCard());
            return blacklistMapper.selectOne(idCardQuery) != null;
        }

        return false;
    }

    /**
     * Create a rejection assessment for blacklisted users.
     */
    private RiskAssessment createRejectAssessment(Long userId, Long applicationId,
                                                   Integer assessmentType, String reason) {
        RiskAssessment assessment = new RiskAssessment();
        assessment.setAssessmentNo(generateAssessmentNo());
        assessment.setUserId(userId);
        assessment.setApplicationId(applicationId);
        assessment.setAssessmentType(assessmentType);
        assessment.setRiskScore(new BigDecimal("100"));
        assessment.setRiskLevel(3);
        assessment.setDecision("REJECT");
        assessment.setModelVersion("rule");
        assessment.setProcessingTimeMs(0);
        assessment.setConfidence(BigDecimal.ONE);
        assessment.setCreatedAt(LocalDateTime.now());

        Map<String, Object> factorsMap = new HashMap<>();
        factorsMap.put("reason", reason);
        assessment.setFactors(factorsMap);

        riskAssessmentMapper.insert(assessment);
        log.info("Risk assessment auto-reject for user {}: {}", userId, reason);
        return assessment;
    }

    /**
     * Inject loan application features (loan_amount, loan_term, loan_interest_rate)
     * into the feature map for risk model input.
     */
    private void injectLoanFeatures(Map<String, Float> features, Long applicationId, UserProfile profile) {
        if (applicationId == null) {
            return;
        }

        LoanApplication application = loanApplicationMapper.selectById(applicationId);
        if (application == null) {
            log.warn("Loan application not found for id={}, skipping loan feature injection", applicationId);
            return;
        }

        if (application.getAmount() != null) {
            features.put("loan_amount", application.getAmount().floatValue());
        }
        if (application.getTerm() != null) {
            features.put("loan_term", (float) application.getTerm());
        }

        // Derive interest rate from credit grade (LoanApplication entity has no rate field)
        String creditGrade = (profile != null && profile.getCreditGrade() != null)
                ? profile.getCreditGrade() : "C";
        BigDecimal interestRate = creditLimitProperties.getInterestRate(creditGrade);
        features.put("loan_interest_rate", interestRate.floatValue());

        log.debug("Injected loan features: amount={}, term={}, rate={} for application={}",
                application.getAmount(), application.getTerm(), interestRate, applicationId);
    }

    /**
     * Release the distributed lock atomically via Lua script.
     */
    private void releaseLock(String lockKey, String lockValue) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
            redisTemplate.execute(script, List.of(lockKey), lockValue);
        } catch (Exception e) {
            log.error("Failed to release lock for key {}: {}", lockKey, e.getMessage());
        }
    }

    /**
     * Generate a unique assessment number.
     */
    private String generateAssessmentNo() {
        return "RA" + IdUtil.getSnowflakeNextIdStr();
    }

    /**
     * Convert feature map to a snapshot suitable for JSON storage.
     */
    private Map<String, Object> convertFeaturesToSnapshot(Map<String, Float> features) {
        if (features == null) {
            return Map.of();
        }
        Map<String, Object> snapshot = new HashMap<>(features.size());
        features.forEach((k, v) -> snapshot.put(k, v));
        return snapshot;
    }
}
