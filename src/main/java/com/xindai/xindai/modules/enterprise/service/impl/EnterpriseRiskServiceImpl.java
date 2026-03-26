package com.xindai.xindai.modules.enterprise.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.client.model.ModelServiceClient;
import com.xindai.xindai.client.model.dto.PredictRequest;
import com.xindai.xindai.client.model.dto.PredictResponse;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseRiskQueryDTO;
import com.xindai.xindai.modules.enterprise.dto.RiskAssessResultVO;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseCustomer;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseCustomerMapper;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseOperationLogMapper;
import com.xindai.xindai.modules.enterprise.service.EnterpriseRiskService;
import com.xindai.xindai.modules.risk.entity.RiskAssessment;
import com.xindai.xindai.modules.risk.mapper.RiskAssessmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 企业风控评估服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseRiskServiceImpl implements EnterpriseRiskService {

    private final EnterpriseCustomerMapper customerMapper;
    private final RiskAssessmentMapper riskAssessmentMapper;
    private final ModelServiceClient modelServiceClient;
    private final EnterpriseOperationLogMapper logMapper;

    @Override
    public RiskAssessResultVO assessCustomer(Long enterpriseId, Long customerId) {
        // 1. 获取客户信息
        EnterpriseCustomer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<EnterpriseCustomer>()
                        .eq(EnterpriseCustomer::getEnterpriseId, enterpriseId)
                        .eq(EnterpriseCustomer::getId, customerId)
        );
        if (customer == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_CUSTOMER_NOT_FOUND);
        }

        // 2. 构建特征并调用风控模型服务
        Map<String, Float> features = buildRiskFeatures(customer);
        String userId = customer.getIdCard();

        PredictResponse modelResult = modelServiceClient.predict(userId, features);

        // 3. 计算风险等级和分数
        Integer riskScore = calculateRiskScore(modelResult);
        int riskLevel = calculateRiskLevel(riskScore);

        // 4. 更新客户风险信息
        customer.setCreditScore(riskScore);
        customer.setRiskLevel(riskLevel);
        customerMapper.updateById(customer);

        // 5. 保存评估记录
        RiskAssessment assessment = new RiskAssessment();
        assessment.setAssessmentNo(generateAssessmentNo());
        assessment.setUserId(customer.getId());
        assessment.setApplicationId(null);
        assessment.setAssessmentType(2); // 2-借款评估
        assessment.setRiskScore(java.math.BigDecimal.valueOf(riskScore));
        assessment.setRiskLevel(convertRiskLevel(riskLevel));
        assessment.setDecision(getDecision(riskLevel));
        assessment.setModelVersion(modelServiceClient.getModelVersion());

        // 保存特征快照
        Map<String, Object> featureSnapshot = new HashMap<>();
        featureSnapshot.put("idCard", customer.getIdCard());
        featureSnapshot.put("phone", customer.getPhone());
        featureSnapshot.put("realName", customer.getRealName());
        assessment.setFeatureSnapshot(featureSnapshot);

        // 保存风险因素
        Map<String, Object> factorsMap = new HashMap<>();
        List<Map<String, Object>> factorsList = modelResult.getFactors();
        if (factorsList != null) {
            factorsMap.put("factors", factorsList);
        }
        assessment.setFactors(factorsMap);

        riskAssessmentMapper.insert(assessment);

        // 6. 构建返回结果
        return buildResultVO(customer, modelResult, assessment);
    }

    @Override
    public List<RiskAssessResultVO> batchAssess(Long enterpriseId, List<Long> customerIds) {
        List<RiskAssessResultVO> results = new ArrayList<>();
        for (Long customerId : customerIds) {
            try {
                results.add(assessCustomer(enterpriseId, customerId));
            } catch (Exception e) {
                log.error("Failed to assess customer {}: {}", customerId, e.getMessage());
                // 记录失败但继续处理其他客户
            }
        }
        return results;
    }

    @Override
    public Page<RiskAssessResultVO> getAssessHistory(Long enterpriseId, Long customerId, EnterpriseRiskQueryDTO queryDTO) {
        // 查询客户信息
        EnterpriseCustomer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<EnterpriseCustomer>()
                        .eq(EnterpriseCustomer::getEnterpriseId, enterpriseId)
                        .eq(EnterpriseCustomer::getId, customerId)
        );
        if (customer == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_CUSTOMER_NOT_FOUND);
        }

        // 查询评估历史
        Page<RiskAssessment> pageParam = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        Page<RiskAssessment> assessmentPage = riskAssessmentMapper.selectPage(pageParam,
                new LambdaQueryWrapper<RiskAssessment>()
                        .eq(RiskAssessment::getUserId, customer.getId())
                        .orderByDesc(RiskAssessment::getCreatedAt)
        );

        // 转换为VO
        Page<RiskAssessResultVO> resultPage = new Page<>(assessmentPage.getCurrent(), assessmentPage.getSize(), assessmentPage.getTotal());
        List<RiskAssessResultVO> records = new ArrayList<>();
        for (RiskAssessment assessment : assessmentPage.getRecords()) {
            RiskAssessResultVO vo = new RiskAssessResultVO();
            vo.setCustomerId(customer.getId());
            vo.setCustomerName(customer.getRealName());
            vo.setRiskScore(assessment.getRiskScore() != null ? assessment.getRiskScore().intValue() : null);
            vo.setRiskLevel(convertRiskLevelToEnterprise(assessment.getRiskLevel()));
            vo.setRiskAdvice(getRiskAdvice(vo.getRiskLevel()));
            vo.setAssessTime(assessment.getCreatedAt());

            // 解析风险因素
            List<RiskAssessResultVO.RiskFactor> riskFactors = parseRiskFactors(assessment.getFactors());
            vo.setRiskFactors(riskFactors);

            records.add(vo);
        }
        resultPage.setRecords(records);

        return resultPage;
    }

    /**
     * 构建风控特征
     */
    private Map<String, Float> buildRiskFeatures(EnterpriseCustomer customer) {
        Map<String, Float> features = new HashMap<>();

        // 信用分数特征 (归一化)
        features.put("credit_score", customer.getCreditScore() != null ? customer.getCreditScore() / 1000.0f : 0.5f);

        // 借款次数特征 (归一化)
        features.put("loan_count", customer.getTotalLoanCount() != null ? Math.min(customer.getTotalLoanCount(), 50) / 50.0f : 0.0f);

        // 借款金额特征 (归一化)
        features.put("loan_amount", customer.getTotalLoanAmount() != null ?
                Math.min(customer.getTotalLoanAmount().floatValue(), 100000) / 100000.0f : 0.0f);

        // 状态特征
        features.put("customer_status", customer.getStatus() != null ? customer.getStatus() / 10.0f : 0.0f);

        // 风险等级特征
        features.put("existing_risk_level", customer.getRiskLevel() != null ? customer.getRiskLevel() / 2.0f : 1.0f);

        return features;
    }

    /**
     * 从模型结果计算风险分数
     */
    private Integer calculateRiskScore(PredictResponse modelResult) {
        Double riskScore = modelResult.getRiskScore();
        if (riskScore != null) {
            // 将模型输出 (0-1) 转换为信用分数 (300-850)
            return (int) (300 + riskScore * 550);
        }
        return 600; // 默认分数
    }

    /**
     * 计算风险等级
     */
    private int calculateRiskLevel(Integer score) {
        if (score == null) return 1;
        if (score >= 700) return 0; // 低风险
        if (score >= 500) return 1; // 中风险
        return 2; // 高风险
    }

    /**
     * 转换风险等级 (企业: 0-低 1-中 2-高 -> 风控: 1-低 2-中 3-高)
     */
    private Integer convertRiskLevel(int enterpriseRiskLevel) {
        return enterpriseRiskLevel + 1;
    }

    /**
     * 转换风险等级 (风控: 1-低 2-中 3-高 -> 企业: 0-低 1-中 2-高)
     */
    private int convertRiskLevelToEnterprise(Integer riskLevel) {
        if (riskLevel == null) return 1;
        return riskLevel - 1;
    }

    /**
     * 获取决策建议
     */
    private String getDecision(int riskLevel) {
        switch (riskLevel) {
            case 0: return "APPROVE";
            case 1: return "MANUAL_REVIEW";
            case 2: return "REJECT";
            default: return "MANUAL_REVIEW";
        }
    }

    /**
     * 获取风险建议
     */
    private String getRiskAdvice(int riskLevel) {
        switch (riskLevel) {
            case 0: return "建议快速审批";
            case 1: return "建议人工复核";
            case 2: return "建议谨慎审批或拒绝";
            default: return "";
        }
    }

    /**
     * 生成评估编号
     */
    private String generateAssessmentNo() {
        return "RA" + IdUtil.getSnowflake(1, 1).nextId();
    }

    /**
     * 构建返回结果VO
     */
    private RiskAssessResultVO buildResultVO(EnterpriseCustomer customer, PredictResponse modelResult, RiskAssessment assessment) {
        RiskAssessResultVO vo = new RiskAssessResultVO();
        vo.setCustomerId(customer.getId());
        vo.setCustomerName(customer.getRealName());
        vo.setRiskScore(customer.getCreditScore());
        vo.setRiskLevel(customer.getRiskLevel());
        vo.setRiskAdvice(getRiskAdvice(customer.getRiskLevel()));
        vo.setAssessTime(assessment.getCreatedAt());

        // 解析风险因素
        List<RiskAssessResultVO.RiskFactor> riskFactors = parseRiskFactors(assessment.getFactors());
        vo.setRiskFactors(riskFactors);

        return vo;
    }

    /**
     * 解析风险因素
     */
    private List<RiskAssessResultVO.RiskFactor> parseRiskFactors(Map<String, Object> factors) {
        List<RiskAssessResultVO.RiskFactor> result = new ArrayList<>();
        if (factors == null) return result;

        Object factorsObj = factors.get("factors");
        if (factorsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> factorsList = (List<Map<String, Object>>) factorsObj;
            for (Map<String, Object> factor : factorsList) {
                RiskAssessResultVO.RiskFactor rf = new RiskAssessResultVO.RiskFactor();
                rf.setName(String.valueOf(factor.get("name")));
                rf.setValue(String.valueOf(factor.get("value")));
                Object weight = factor.get("weight");
                rf.setWeight(weight != null ? ((Number) weight).intValue() : null);
                result.add(rf);
            }
        }

        return result;
    }
}
