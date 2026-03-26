package com.xindai.xindai.modules.risk.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.client.model.ModelServiceClient;
import com.xindai.xindai.client.model.dto.PredictResponse;
import com.xindai.xindai.common.exception.BusinessException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    private static final String RISK_LOCK_PREFIX = "lock:risk:";
    private static final long LOCK_TIMEOUT = 30;

    /**
     * Lua脚本实现原子性的锁释放：只有持有锁的线程才能释放
     */
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    @Override
    public RiskAssessment assess(Long userId, Long applicationId, Integer assessmentType) {
        String lockKey = RISK_LOCK_PREFIX + userId;
        // 使用唯一值标识锁持有者，防止误删其他线程/进程的锁
        String lockValue = IdUtil.fastSimpleUUID();
        Boolean locked = false;

        try {
            // 获取分布式锁，防止并发评估
            locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, LOCK_TIMEOUT, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(locked)) {
                throw new BusinessException("风险评估正在进行中，请稍后重试");
            }

            // 1. 获取用户信息
            User user = userProfileService.getById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }

            // 2. 检查黑名单
            if (checkBlacklist(user)) {
                return createRejectAssessment(userId, applicationId, assessmentType, "用户在黑名单中");
            }

            // 3. 收集特征
            Map<String, Float> features = collectFeatures(userId, user);

            // 4. 调用模型服务
            PredictResponse response = modelServiceClient.predict(userId.toString(), features);

            // 5. 保存评估结果
            RiskAssessment assessment = new RiskAssessment();
            assessment.setAssessmentNo(generateAssessmentNo());
            assessment.setUserId(userId);
            assessment.setApplicationId(applicationId);
            assessment.setAssessmentType(assessmentType);
            assessment.setRiskScore(BigDecimal.valueOf(response.getRiskScore()));
            assessment.setRiskLevel(response.getRiskLevel());
            assessment.setDecision(response.getDecision());
            assessment.setModelVersion(response.getModelVersion());
            assessment.setFeatureSnapshot(new HashMap<>(features));
            assessment.setProcessingTimeMs(response.getProcessingTimeMs());
            assessment.setCreatedAt(LocalDateTime.now());

            // 转换 factors
            List<Map<String, Object>> factors = response.getFactors();
            if (factors != null) {
                Map<String, Object> factorsMap = new HashMap<>();
                factorsMap.put("items", factors);
                assessment.setFactors(factorsMap);
            }

            riskAssessmentMapper.insert(assessment);

            log.info("Risk assessment completed: userId={}, score={}, decision={}",
                    userId, assessment.getRiskScore(), assessment.getDecision());

            return assessment;

        } finally {
            // 只有成功获取锁时才释放，使用Lua脚本保证原子性
            if (Boolean.TRUE.equals(locked)) {
                releaseLock(lockKey, lockValue);
            }
        }
    }

    /**
     * 使用Lua脚本原子性释放分布式锁，确保只有锁的持有者才能释放
     *
     * @param lockKey   锁的Redis key
     * @param lockValue 锁的唯一标识值
     * @return true表示锁释放成功，false表示锁已被其他线程持有或已过期
     */
    private boolean releaseLock(String lockKey, String lockValue) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(lockKey), lockValue);
        return Long.valueOf(1L).equals(result);
    }

    /**
     * 检查黑名单
     */
    private boolean checkBlacklist(User user) {
        // 检查手机号
        Blacklist phoneBlacklist = blacklistMapper.selectOne(
                new LambdaQueryWrapper<Blacklist>()
                        .eq(Blacklist::getType, 1)
                        .eq(Blacklist::getValue, user.getPhone())
        );
        if (phoneBlacklist != null) {
            return true;
        }

        // 检查身份证
        if (user.getIdCard() != null) {
            Blacklist idCardBlacklist = blacklistMapper.selectOne(
                    new LambdaQueryWrapper<Blacklist>()
                            .eq(Blacklist::getType, 2)
                            .eq(Blacklist::getValue, user.getIdCard())
            );
            return idCardBlacklist != null;
        }

        return false;
    }

    /**
     * 收集特征 - 使用策略模式从用户画像获取
     */
    private Map<String, Float> collectFeatures(Long userId, User user) {
        // 获取用户画像数据
        UserProfile profile = userProfileDetailService.getUserProfileByUserId(userId);

        // 使用聚合服务收集特征
        return featureAggregationService.collectFeatures(userId, user, profile);
    }

    /**
     * 创建拒绝评估结果
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
        assessment.setCreatedAt(LocalDateTime.now());

        Map<String, Object> factors = new HashMap<>();
        factors.put("reason", reason);
        assessment.setFactors(factors);

        riskAssessmentMapper.insert(assessment);
        return assessment;
    }

    @Override
    public Page<RiskAssessment> getAssessmentHistory(Long userId, int page, int size) {
        return riskAssessmentMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<RiskAssessment>()
                        .eq(RiskAssessment::getUserId, userId)
                        .orderByDesc(RiskAssessment::getCreatedAt)
        );
    }

    @Override
    public RiskAssessment getByAssessmentNo(String assessmentNo) {
        return riskAssessmentMapper.selectOne(
                new LambdaQueryWrapper<RiskAssessment>()
                        .eq(RiskAssessment::getAssessmentNo, assessmentNo)
        );
    }

    @Override
    public RiskAssessmentVO toVO(RiskAssessment assessment) {
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
        return assessments.stream()
                .map(this::toVO)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<RiskAssessment> getAssessmentsSince(LocalDateTime since) {
        return riskAssessmentMapper.selectList(
                new LambdaQueryWrapper<RiskAssessment>()
                        .ge(RiskAssessment::getCreatedAt, since)
        );
    }

    private String generateAssessmentNo() {
        return "RA" + IdUtil.getSnowflakeNextIdStr();
    }
}
