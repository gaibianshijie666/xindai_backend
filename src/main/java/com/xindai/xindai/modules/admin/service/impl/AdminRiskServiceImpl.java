package com.xindai.xindai.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.admin.dto.RiskAssessmentQueryDTO;
import com.xindai.xindai.modules.admin.dto.RiskOverrideDTO;
import com.xindai.xindai.modules.admin.service.AdminRiskService;
import com.xindai.xindai.modules.admin.vo.AdminRiskAssessmentVO;
import com.xindai.xindai.modules.admin.vo.AdminRiskAssessmentDetailVO;
import com.xindai.xindai.modules.admin.vo.RiskRuleVO;
import com.xindai.xindai.modules.loan.entity.LoanApplication;
import com.xindai.xindai.modules.loan.mapper.LoanApplicationMapper;
import com.xindai.xindai.modules.risk.entity.RiskAssessment;
import com.xindai.xindai.modules.risk.mapper.RiskAssessmentMapper;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端风险评估服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRiskServiceImpl implements AdminRiskService {

    private final RiskAssessmentMapper riskAssessmentMapper;
    private final UserMapper userMapper;
    private final LoanApplicationMapper loanApplicationMapper;

    // 默认风险规则配置 (读取自系统配置或application.yml)
    private static final int AUTO_APPROVE_THRESHOLD = 30;
    private static final int AUTO_REJECT_THRESHOLD = 70;
    private static final int MANUAL_REVIEW_MIN_SCORE = 30;
    private static final int MANUAL_REVIEW_MAX_SCORE = 70;
    private static final double CONFIDENCE_THRESHOLD = 0.7;

    @Override
    public Page<AdminRiskAssessmentVO> getAssessmentList(RiskAssessmentQueryDTO queryDTO) {
        Page<RiskAssessment> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        LambdaQueryWrapper<RiskAssessment> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getUserId() != null) {
            wrapper.eq(RiskAssessment::getUserId, queryDTO.getUserId());
        }
        if (queryDTO.getRiskLevel() != null) {
            wrapper.eq(RiskAssessment::getRiskLevel, queryDTO.getRiskLevel());
        }
        if (queryDTO.getDecision() != null) {
            wrapper.eq(RiskAssessment::getDecision, queryDTO.getDecision());
        }
        if (queryDTO.getOverridden() != null) {
            wrapper.eq(RiskAssessment::getOverridden, queryDTO.getOverridden());
        }
        if (queryDTO.getStartDate() != null) {
            wrapper.ge(RiskAssessment::getCreatedAt, queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            wrapper.le(RiskAssessment::getCreatedAt, queryDTO.getEndDate());
        }
        wrapper.orderByDesc(RiskAssessment::getCreatedAt);

        Page<RiskAssessment> assessmentPage = riskAssessmentMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<AdminRiskAssessmentVO> voPage = new Page<>(
                assessmentPage.getCurrent(),
                assessmentPage.getSize(),
                assessmentPage.getTotal()
        );
        voPage.setRecords(assessmentPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));

        return voPage;
    }

    @Override
    public AdminRiskAssessmentDetailVO getAssessmentDetail(Long id) {
        RiskAssessment assessment = riskAssessmentMapper.selectById(id);
        if (assessment == null) {
            throw new BusinessException(ErrorCode.RISK_ASSESSMENT_NOT_FOUND);
        }
        return convertToDetailVO(assessment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void overrideDecision(Long id, RiskOverrideDTO overrideDTO, Long adminId) {
        RiskAssessment assessment = riskAssessmentMapper.selectById(id);
        if (assessment == null) {
            throw new BusinessException(ErrorCode.RISK_ASSESSMENT_NOT_FOUND);
        }

        // 更新风险评估的覆盖信息
        assessment.setOverridden(true);
        assessment.setOverrideDecision(overrideDTO.getNewDecision());
        assessment.setOverrideReason(overrideDTO.getReason());
        assessment.setOverrideBy(adminId);
        assessment.setOverrideAt(LocalDateTime.now());
        riskAssessmentMapper.updateById(assessment);

        // 如果关联了借款申请，更新申请状态
        if (assessment.getApplicationId() != null) {
            LoanApplication application = loanApplicationMapper.selectById(assessment.getApplicationId());
            if (application != null) {
                Integer newStatus = "APPROVE".equals(overrideDTO.getNewDecision()) ? 2 : 3;
                application.setStatus(newStatus);
                application.setReviewedAt(LocalDateTime.now());
                application.setReviewerId(adminId);
                application.setReviewNote("风控决策覆盖: " + overrideDTO.getReason());
                loanApplicationMapper.updateById(application);
            }
        }

        log.info("风控决策覆盖: assessmentId={}, newDecision={}, adminId={}, reason={}",
                id, overrideDTO.getNewDecision(), adminId, overrideDTO.getReason());
    }

    @Override
    public RiskRuleVO getRiskRules() {
        RiskRuleVO ruleVO = new RiskRuleVO();
        ruleVO.setAutoApproveThreshold(AUTO_APPROVE_THRESHOLD);
        ruleVO.setAutoRejectThreshold(AUTO_REJECT_THRESHOLD);

        RiskRuleVO.ManualReviewRange range = new RiskRuleVO.ManualReviewRange();
        range.setMinScore(MANUAL_REVIEW_MIN_SCORE);
        range.setMaxScore(MANUAL_REVIEW_MAX_SCORE);
        ruleVO.setManualReviewRange(range);

        ruleVO.setModelVersion("v1.0");
        ruleVO.setConfidenceThreshold(CONFIDENCE_THRESHOLD);

        // 评分权重配置
        Map<String, Double> weights = new HashMap<>();
        weights.put("creditScore", 0.3);
        weights.put("riskScore", 0.25);
        weights.put("dti", 0.2);
        weights.put("employmentYears", 0.1);
        weights.put("income", 0.1);
        weights.put("socialFeatures", 0.05);
        ruleVO.setScoringWeights(weights);

        return ruleVO;
    }

    @Override
    public void updateRiskRules(RiskRuleVO ruleVO) {
        // 当前只读,保留接口
        // 后续可扩展为写入系统配置表
        log.info("风险规则更新请求 (当前只读): {}", ruleVO);
    }

    private AdminRiskAssessmentVO convertToVO(RiskAssessment assessment) {
        AdminRiskAssessmentVO vo = new AdminRiskAssessmentVO();
        vo.setId(assessment.getId());
        vo.setAssessmentNo(assessment.getAssessmentNo());
        vo.setUserId(assessment.getUserId());
        vo.setApplicationId(assessment.getApplicationId());
        vo.setAssessmentType(assessment.getAssessmentType());
        vo.setRiskScore(assessment.getRiskScore());
        vo.setRiskLevel(assessment.getRiskLevel());
        vo.setDecision(assessment.getDecision());
        vo.setOverridden(assessment.getOverridden());
        vo.setOverrideDecision(assessment.getOverrideDecision());
        vo.setModelVersion(assessment.getModelVersion());
        vo.setProcessingTimeMs(assessment.getProcessingTimeMs());
        vo.setConfidence(assessment.getConfidence());
        vo.setCreatedAt(assessment.getCreatedAt());
        vo.setOverrideAt(assessment.getOverrideAt());
        vo.setOverrideBy(assessment.getOverrideBy());

        // 获取用户信息
        User user = userMapper.selectById(assessment.getUserId());
        if (user != null) {
            vo.setUsername(user.getRealName());
            vo.setUserPhone(user.getPhone());
        }

        return vo;
    }

    private AdminRiskAssessmentDetailVO convertToDetailVO(RiskAssessment assessment) {
        AdminRiskAssessmentDetailVO vo = new AdminRiskAssessmentDetailVO();
        vo.setId(assessment.getId());
        vo.setAssessmentNo(assessment.getAssessmentNo());
        vo.setUserId(assessment.getUserId());
        vo.setApplicationId(assessment.getApplicationId());
        vo.setAssessmentType(assessment.getAssessmentType());
        vo.setRiskScore(assessment.getRiskScore());
        vo.setRiskLevel(assessment.getRiskLevel());
        vo.setDecision(assessment.getDecision());
        vo.setOverridden(assessment.getOverridden());
        vo.setOverrideDecision(assessment.getOverrideDecision());
        vo.setOverrideReason(assessment.getOverrideReason());
        vo.setOverrideAt(assessment.getOverrideAt());
        vo.setOverrideBy(assessment.getOverrideBy());
        vo.setModelVersion(assessment.getModelVersion());
        vo.setProcessingTimeMs(assessment.getProcessingTimeMs());
        vo.setConfidence(assessment.getConfidence());
        vo.setFeatureSnapshot(assessment.getFeatureSnapshot());
        vo.setFactors(assessment.getFactors());
        vo.setCreatedAt(assessment.getCreatedAt());

        // 获取用户信息
        User user = userMapper.selectById(assessment.getUserId());
        if (user != null) {
            vo.setUsername(user.getRealName());
            vo.setUserPhone(user.getPhone());
            vo.setUserIdCard(user.getIdCard());
        }

        // 获取申请信息
        if (assessment.getApplicationId() != null) {
            LoanApplication application = loanApplicationMapper.selectById(assessment.getApplicationId());
            if (application != null) {
                vo.setApplicationNo(application.getApplicationNo());
                vo.setApplicationAmount(application.getAmount());
                vo.setApplicationTerm(application.getTerm());
            }
        }

        // 获取覆盖人信息
        if (assessment.getOverrideBy() != null) {
            User overrideByUser = userMapper.selectById(assessment.getOverrideBy());
            if (overrideByUser != null) {
                vo.setOverrideByName(overrideByUser.getRealName());
            }
        }

        return vo;
    }
}
