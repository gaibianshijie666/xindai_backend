package com.xindai.xindai.modules.loan.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.client.model.CreditLimitPredictionClient;
import com.xindai.xindai.common.constants.CreditLimitConstants;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.config.CreditLimitProperties;
import com.xindai.xindai.modules.loan.dto.*;
import com.xindai.xindai.modules.loan.entity.CreditLimit;
import com.xindai.xindai.modules.loan.entity.LoanApplication;
import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.entity.RepaymentPlan;
import com.xindai.xindai.modules.loan.enums.ApplicationStatus;
import com.xindai.xindai.modules.loan.enums.ContractStatus;
import com.xindai.xindai.modules.loan.enums.RepaymentStatus;
import com.xindai.xindai.modules.loan.mapper.CreditLimitMapper;
import com.xindai.xindai.modules.loan.mapper.LoanApplicationMapper;
import com.xindai.xindai.modules.loan.mapper.LoanContractMapper;
import com.xindai.xindai.modules.loan.mapper.RepaymentPlanMapper;
import com.xindai.xindai.modules.loan.service.CreditLimitCalculator;
import com.xindai.xindai.modules.loan.service.LoanService;
import com.xindai.xindai.modules.notification.service.NotificationService;
import com.xindai.xindai.modules.risk.entity.RiskAssessment;
import com.xindai.xindai.modules.risk.service.RiskAssessmentService;
import com.xindai.xindai.modules.user.entity.UserProfile;
import com.xindai.xindai.modules.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final CreditLimitMapper creditLimitMapper;
    private final LoanApplicationMapper loanApplicationMapper;
    private final LoanContractMapper loanContractMapper;
    private final RepaymentPlanMapper repaymentPlanMapper;
    private final UserProfileMapper userProfileMapper;
    private final RiskAssessmentService riskAssessmentService;
    private final CreditLimitCalculator creditLimitCalculator;
    private final CreditLimitPredictionClient limitPredictionClient;
    private final CacheManager cacheManager;
    private final CreditLimitProperties creditLimitProperties;
    private final NotificationService notificationService;

    @Override
    @Cacheable(value = "creditLimit", key = "#userId", unless = "#result == null")
    public CreditLimitVO getCreditLimit(Long userId) {
        CreditLimit limit = getOrCreateCreditLimit(userId);
        // 复用已查询的UserProfile，避免重复查询
        UserProfile profile = getUserProfile(userId);
        return buildCreditLimitVO(limit, profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "creditLimit", key = "#userId")
    public LoanApplicationVO apply(Long userId, LoanApplyDTO dto) {
        CreditLimit limit = getOrCreateCreditLimit(userId);

        // 检查额度是否足够
        if (limit.getAvailableLimit().compareTo(dto.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.LIMIT_INSUFFICIENT,
                "当前可用额度：" + limit.getAvailableLimit());
        }

        // 创建借款申请
        LoanApplication application = new LoanApplication();
        application.setApplicationNo(generateApplicationNo());
        application.setUserId(userId);
        application.setAmount(dto.getAmount());
        application.setTerm(dto.getTerm());
        application.setPurpose(dto.getPurpose());
        application.setStatus(ApplicationStatus.PENDING.getCode()); // 待审批
        loanApplicationMapper.insert(application);

        // 调用风控评估
        try {
            RiskAssessment assessment = riskAssessmentService.assess(userId, application.getId(), 1);

            // 根据评估结果自动决策
            if ("APPROVE".equals(assessment.getDecision())) {
                application.setStatus(ApplicationStatus.APPROVED.getCode()); // 自动通过
                log.info("Loan application auto-approved: applicationId={}, riskScore={}",
                        application.getId(), assessment.getRiskScore());
            } else if ("REJECT".equals(assessment.getDecision())) {
                application.setStatus(ApplicationStatus.REJECTED.getCode()); // 自动拒绝
                log.info("Loan application auto-rejected: applicationId={}, riskScore={}",
                        application.getId(), assessment.getRiskScore());
            } else {
                // MANUAL_REVIEW 或其他情况转为审批中状态
                application.setStatus(ApplicationStatus.REVIEWING.getCode()); // 需要人工审核
                log.info("Loan application requires manual review: applicationId={}, riskScore={}",
                        application.getId(), assessment.getRiskScore());
            }

            loanApplicationMapper.updateById(application);

            // 根据风险评分动态调整用户额度
            updateCreditLimitByRiskScore(userId, assessment.getRiskScore().doubleValue());

        } catch (Exception e) {
            log.error("Risk assessment failed for application: applicationId={}", application.getId(), e);
            // 风控评估失败时保持待审核状态，等待人工处理
        }

        notificationService.send(userId, "USER", "贷款申请提交成功",
                "您的贷款申请已提交，申请编号：" + application.getApplicationNo() + "，金额：" + dto.getAmount() + "元，请耐心等待审核。",
                "LOAN", application.getId().toString());

        return buildLoanApplicationVO(application);
    }

    @Override
    public List<LoanApplicationVO> getApplications(Long userId) {
        List<LoanApplication> applications = loanApplicationMapper.selectList(
                new LambdaQueryWrapper<LoanApplication>()
                        .eq(LoanApplication::getUserId, userId)
                        .orderByDesc(LoanApplication::getCreatedAt)
        );
        return applications.stream()
                .map(this::buildLoanApplicationVO)
                .collect(Collectors.toList());
    }

    @Override
    public LoanApplicationVO getApplicationDetail(Long userId, Long applicationId) {
        LoanApplication application = loanApplicationMapper.selectOne(
                new LambdaQueryWrapper<LoanApplication>()
                        .eq(LoanApplication::getId, applicationId)
                        .eq(LoanApplication::getUserId, userId)
        );
        if (application == null) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND,
                "id=" + applicationId);
        }
        return buildLoanApplicationVO(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreditLimit getOrCreateCreditLimit(Long userId) {
        CreditLimit limit = creditLimitMapper.selectOne(
                new LambdaQueryWrapper<CreditLimit>().eq(CreditLimit::getUserId, userId)
        );

        if (limit == null) {
            // 使用智能额度计算器计算额度
            limit = calculateInitialCreditLimit(userId);
            creditLimitMapper.insert(limit);
            log.info("Created new credit limit for user {}: {}", userId, limit.getTotalLimit());
        } else {
            // 检查是否需要重新计算额度（用户画像更新后）
            UserProfile profile = userProfileMapper.selectOne(
                    new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
            );
            if (profile != null && profile.getRiskScore() != null) {
                // 如果用户有风险评分，动态调整额度
                BigDecimal adjustedLimit = creditLimitCalculator.adjustByRiskScore(
                        limit.getTotalLimit(), profile.getRiskScore());
                if (adjustedLimit.compareTo(limit.getTotalLimit()) != 0) {
                    limit.setTotalLimit(adjustedLimit);
                    limit.setAvailableLimit(adjustedLimit.subtract(limit.getUsedLimit()));
                    creditLimitMapper.updateById(limit);
                    log.info("Adjusted credit limit for user {}: {} -> {}",
                            userId, limit.getTotalLimit(), adjustedLimit);
                }
            }
        }
        return limit;
    }

    /**
     * 计算初始信用额度
     * 优先使用XGBoost模型，降级时使用规则引擎
     */
    private CreditLimit calculateInitialCreditLimit(Long userId) {
        UserProfile profile = getUserProfile(userId);
        CreditLimit limit = createBaseCreditLimit(userId);

        BigDecimal predictedLimit = predictLimitWithFallback(userId, profile);
        limit.setTotalLimit(predictedLimit);
        limit.setAvailableLimit(predictedLimit);

        return limit;
    }

    private UserProfile getUserProfile(Long userId) {
        return userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
        );
    }

    private CreditLimit createBaseCreditLimit(Long userId) {
        CreditLimit limit = new CreditLimit();
        limit.setUserId(userId);
        limit.setTotalLimit(BigDecimal.ZERO);
        limit.setUsedLimit(BigDecimal.ZERO);
        limit.setAvailableLimit(BigDecimal.ZERO);
        limit.setCreatedAt(LocalDateTime.now());
        limit.setStatus(1);
        return limit;
    }

    private BigDecimal predictLimitWithFallback(Long userId, UserProfile profile) {
        if (profile == null || profile.getAnnualIncome() == null) {
            return creditLimitProperties.getDefaultLimit();
        }

        try {
            String creditGrade = profile.getCreditGrade() != null ? profile.getCreditGrade() : "C";
            BigDecimal dti = profile.getDti() != null ? profile.getDti() : new BigDecimal("20");
            BigDecimal interestRate = estimateInterestRate(creditGrade);

            BigDecimal predictedLimit = limitPredictionClient.predictLimitSimple(
                    userId,
                    profile.getAnnualIncome(),
                    creditGrade,
                    dti,
                    interestRate
            );

            log.info("XGBoost predicted limit for user {}: {}", userId, predictedLimit);
            return predictedLimit;
        } catch (Exception e) {
            log.warn("Model prediction failed for user {}, using rule-based calculation: {}",
                     userId, e.getMessage());
            return calculateLimitByRules(profile);
        }
    }

    private BigDecimal calculateLimitByRules(UserProfile profile) {
        double riskScore = profile.getRiskScore() != null ? profile.getRiskScore() : 50.0;
        String creditGrade = profile.getCreditGrade() != null ? profile.getCreditGrade() : "C";
        CreditLimit calculatedLimit = creditLimitCalculator.calculate(profile, riskScore, creditGrade);
        return calculatedLimit.getTotalLimit();
    }

    /**
     * 根据信用等级估算利率
     */
    private BigDecimal estimateInterestRate(String grade) {
        return switch (grade != null ? grade.toUpperCase() : "C") {
            case "A" -> new BigDecimal("8");
            case "B" -> new BigDecimal("11");
            case "C" -> new BigDecimal("14");
            case "D" -> new BigDecimal("18");
            case "E" -> new BigDecimal("22");
            case "F" -> new BigDecimal("26");
            case "G" -> new BigDecimal("30");
            default -> new BigDecimal("15");
        };
    }

    /**
     * 根据风险评分更新额度
     */
    private void updateCreditLimitByRiskScore(Long userId, double riskScore) {
        CreditLimit limit = creditLimitMapper.selectOne(
                new LambdaQueryWrapper<CreditLimit>().eq(CreditLimit::getUserId, userId)
        );

        if (limit != null) {
            BigDecimal adjustedLimit = creditLimitCalculator.adjustByRiskScore(
                    limit.getTotalLimit(), riskScore);

            if (adjustedLimit.compareTo(limit.getTotalLimit()) != 0) {
                BigDecimal oldLimit = limit.getTotalLimit();
                limit.setTotalLimit(adjustedLimit);
                // 重新计算可用额度
                BigDecimal newAvailable = adjustedLimit.subtract(limit.getUsedLimit());
                if (newAvailable.compareTo(BigDecimal.ZERO) < 0) {
                    newAvailable = BigDecimal.ZERO;
                }
                limit.setAvailableLimit(newAvailable);
                creditLimitMapper.updateById(limit);

                // 清除缓存
                evictCreditLimitCache(userId);

                log.info("Updated credit limit after risk assessment for user {}: {} -> {}",
                        userId, oldLimit, adjustedLimit);
            }
        }
    }

    /**
     * 清除用户额度缓存
     */
    private void evictCreditLimitCache(Long userId) {
        var cache = cacheManager.getCache("creditLimit");
        if (cache != null) {
            cache.evict(userId);
            log.debug("Evicted credit limit cache for user {}", userId);
        }
    }

    private String generateApplicationNo() {
        return "LA" + IdUtil.getSnowflakeNextIdStr();
    }

    private CreditLimitVO buildCreditLimitVO(CreditLimit limit, UserProfile profile) {
        CreditLimitVO vo = new CreditLimitVO();
        vo.setId(limit.getId());
        vo.setUserId(limit.getUserId());
        vo.setTotalLimit(limit.getTotalLimit());
        vo.setUsedLimit(limit.getUsedLimit());
        vo.setAvailableLimit(limit.getAvailableLimit());
        vo.setStatus(limit.getStatus());
        if (limit.getExpireAt() != null) {
            vo.setExpireAt(limit.getExpireAt().toString());
        }

        // 使用传入的UserProfile，避免重复查询
        if (profile != null && profile.getAnnualIncome() != null) {
            double riskScore = profile.getRiskScore() != null ? profile.getRiskScore() : 50.0;
            String creditGrade = profile.getCreditGrade() != null ? profile.getCreditGrade() : "C";

            try {
                CreditLimitCalculator.CreditLimitDetail detail =
                        creditLimitCalculator.getCalculationDetail(profile, riskScore, creditGrade);

                vo.setBaseLimit(detail.baseLimit());
                vo.setIncomeMultiplier(detail.incomeMultiplier());
                vo.setGradeFactor(detail.gradeFactor());
                vo.setEmploymentFactor(detail.employmentFactor());
                vo.setDtiFactor(detail.dtiFactor());
                vo.setRiskAdjustFactor(detail.riskAdjustFactor());
                vo.setExplanation(detail.explanation());
            } catch (Exception e) {
                log.warn("Failed to get credit limit detail for user {}: {}", limit.getUserId(), e.getMessage());
            }
        }

        return vo;
    }

    private LoanApplicationVO buildLoanApplicationVO(LoanApplication application) {
        LoanApplicationVO vo = new LoanApplicationVO();
        vo.setId(application.getId());
        vo.setApplicationNo(application.getApplicationNo());
        vo.setUserId(application.getUserId());
        vo.setAmount(application.getAmount());
        vo.setTerm(application.getTerm());
        vo.setPurpose(application.getPurpose());
        vo.setStatus(application.getStatus());
        if (application.getCreatedAt() != null) {
            vo.setCreatedAt(application.getCreatedAt().toString());
        }
        if (application.getReviewedAt() != null) {
            vo.setReviewedAt(application.getReviewedAt().toString());
        }
        return vo;
    }

    @Override
    public List<RepaymentPlanVO> getPendingRepayment(Long userId) {
        // 获取用户的所有合同
        List<Long> contractIds = loanContractMapper.selectList(
                new LambdaQueryWrapper<LoanContract>()
                        .eq(LoanContract::getUserId, userId)
                        .eq(LoanContract::getStatus, ContractStatus.REPAYING.getCode()) // 进行中的合同
        ).stream().map(LoanContract::getId).toList();

        if (contractIds.isEmpty()) {
            return List.of();
        }

        // 获取待还款的还款计划
        List<RepaymentPlan> plans = repaymentPlanMapper.selectList(
                new LambdaQueryWrapper<RepaymentPlan>()
                        .in(RepaymentPlan::getContractId, contractIds)
                        .eq(RepaymentPlan::getStatus, RepaymentStatus.PENDING.getCode()) // 待还款
                        .orderByAsc(RepaymentPlan::getDueDate)
        );

        return plans.stream().map(plan -> {
            RepaymentPlanVO vo = new RepaymentPlanVO();
            vo.setId(plan.getId());
            vo.setContractId(plan.getContractId());
            vo.setPeriod(plan.getPeriod());
            vo.setDueDate(plan.getDueDate());
            vo.setPrincipal(plan.getPrincipal());
            vo.setInterest(plan.getInterest());
            vo.setTotalAmount(plan.getTotalAmount());
            vo.setStatus(plan.getStatus());
            if (plan.getPaidAt() != null) {
                vo.setPaidAt(plan.getPaidAt().toString());
            }

            // 获取合同编号
            LoanContract contract = loanContractMapper.selectById(plan.getContractId());
            if (contract != null) {
                vo.setContractNo(contract.getContractNo());
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<LoanContractVO> getContracts(Long userId) {
        List<LoanContract> contracts = loanContractMapper.selectList(
                new LambdaQueryWrapper<LoanContract>()
                        .eq(LoanContract::getUserId, userId)
                        .orderByDesc(LoanContract::getCreatedAt)
        );

        return contracts.stream().map(contract -> {
            LoanContractVO vo = new LoanContractVO();
            vo.setId(contract.getId());
            vo.setContractNo(contract.getContractNo());
            vo.setApplicationId(contract.getApplicationId());
            vo.setPrincipal(contract.getPrincipal());
            vo.setInterestRate(contract.getInterestRate());
            vo.setTotalRepayment(contract.getTotalRepayment());
            vo.setTerm(contract.getTerm());
            vo.setStatus(contract.getStatus());
            vo.setDueDate(contract.getDueDate());
            if (contract.getDisbursedAt() != null) {
                vo.setDisbursedAt(contract.getDisbursedAt().toString());
            }
            if (contract.getCreatedAt() != null) {
                vo.setCreatedAt(contract.getCreatedAt().toString());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public LoanContractVO getContractDetail(Long userId, Long contractId) {
        LoanContract contract = loanContractMapper.selectOne(
                new LambdaQueryWrapper<LoanContract>()
                        .eq(LoanContract::getId, contractId)
                        .eq(LoanContract::getUserId, userId)
        );

        if (contract == null) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_FOUND, "id=" + contractId);
        }

        LoanContractVO vo = new LoanContractVO();
        vo.setId(contract.getId());
        vo.setContractNo(contract.getContractNo());
        vo.setApplicationId(contract.getApplicationId());
        vo.setPrincipal(contract.getPrincipal());
        vo.setInterestRate(contract.getInterestRate());
        vo.setTotalRepayment(contract.getTotalRepayment());
        vo.setTerm(contract.getTerm());
        vo.setStatus(contract.getStatus());
        vo.setDueDate(contract.getDueDate());
        if (contract.getDisbursedAt() != null) {
            vo.setDisbursedAt(contract.getDisbursedAt().toString());
        }
        if (contract.getCreatedAt() != null) {
            vo.setCreatedAt(contract.getCreatedAt().toString());
        }
        return vo;
    }

    @Override
    public List<RepaymentPlanVO> getRepaymentPlansByContract(Long userId, Long contractId) {
        // 验证合同归属
        LoanContract contract = loanContractMapper.selectOne(
                new LambdaQueryWrapper<LoanContract>()
                        .eq(LoanContract::getId, contractId)
                        .eq(LoanContract::getUserId, userId)
        );

        if (contract == null) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_FOUND, "id=" + contractId);
        }

        List<RepaymentPlan> plans = repaymentPlanMapper.selectList(
                new LambdaQueryWrapper<RepaymentPlan>()
                        .eq(RepaymentPlan::getContractId, contractId)
                        .orderByAsc(RepaymentPlan::getPeriod)
        );

        return plans.stream().map(plan -> {
            RepaymentPlanVO vo = new RepaymentPlanVO();
            vo.setId(plan.getId());
            vo.setContractId(plan.getContractId());
            vo.setContractNo(contract.getContractNo());
            vo.setPeriod(plan.getPeriod());
            vo.setDueDate(plan.getDueDate());
            vo.setPrincipal(plan.getPrincipal());
            vo.setInterest(plan.getInterest());
            vo.setTotalAmount(plan.getTotalAmount());
            vo.setStatus(plan.getStatus());
            if (plan.getPaidAt() != null) {
                vo.setPaidAt(plan.getPaidAt().toString());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<RepaymentPlanVO> getAllRepaymentPlans(Long userId) {
        // 获取用户的所有合同
        List<LoanContract> contracts = loanContractMapper.selectList(
                new LambdaQueryWrapper<LoanContract>()
                        .eq(LoanContract::getUserId, userId)
        );

        if (contracts.isEmpty()) {
            return List.of();
        }

        List<Long> contractIds = contracts.stream().map(LoanContract::getId).toList();
        Map<Long, String> contractNoMap = contracts.stream()
                .collect(Collectors.toMap(LoanContract::getId, LoanContract::getContractNo));

        List<RepaymentPlan> plans = repaymentPlanMapper.selectList(
                new LambdaQueryWrapper<RepaymentPlan>()
                        .in(RepaymentPlan::getContractId, contractIds)
                        .orderByAsc(RepaymentPlan::getDueDate)
        );

        return plans.stream().map(plan -> {
            RepaymentPlanVO vo = new RepaymentPlanVO();
            vo.setId(plan.getId());
            vo.setContractId(plan.getContractId());
            vo.setContractNo(contractNoMap.get(plan.getContractId()));
            vo.setPeriod(plan.getPeriod());
            vo.setDueDate(plan.getDueDate());
            vo.setPrincipal(plan.getPrincipal());
            vo.setInterest(plan.getInterest());
            vo.setTotalAmount(plan.getTotalAmount());
            vo.setStatus(plan.getStatus());
            if (plan.getPaidAt() != null) {
                vo.setPaidAt(plan.getPaidAt().toString());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepaymentResultVO repay(Long userId, RepayDTO dto) {
        // 验证合同归属
        LoanContract contract = loanContractMapper.selectOne(
                new LambdaQueryWrapper<LoanContract>()
                        .eq(LoanContract::getId, dto.getContractId())
                        .eq(LoanContract::getUserId, userId)
        );

        if (contract == null) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_FOUND, "id=" + dto.getContractId());
        }

        if (contract.getStatus() == ContractStatus.SETTLED.getCode()) {
            throw new BusinessException(ErrorCode.CONTRACT_ALREADY_SETTLED);
        }

        // 获取待还款计划
        List<RepaymentPlan> pendingPlans = repaymentPlanMapper.selectList(
                new LambdaQueryWrapper<RepaymentPlan>()
                        .eq(RepaymentPlan::getContractId, dto.getContractId())
                        .eq(RepaymentPlan::getStatus, RepaymentStatus.PENDING.getCode())
                        .orderByAsc(RepaymentPlan::getPeriod)
        );

        if (pendingPlans.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_PENDING_REPAYMENT);
        }

        return executeRepayment(contract, pendingPlans, dto.getAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepaymentResultVO repayByPeriod(Long userId, Long contractId, Integer period) {
        // 验证合同归属
        LoanContract contract = loanContractMapper.selectOne(
                new LambdaQueryWrapper<LoanContract>()
                        .eq(LoanContract::getId, contractId)
                        .eq(LoanContract::getUserId, userId)
        );

        if (contract == null) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_FOUND, "id=" + contractId);
        }

        // 获取指定期数的还款计划
        RepaymentPlan plan = repaymentPlanMapper.selectOne(
                new LambdaQueryWrapper<RepaymentPlan>()
                        .eq(RepaymentPlan::getContractId, contractId)
                        .eq(RepaymentPlan::getPeriod, period)
        );

        if (plan == null) {
            throw new BusinessException(ErrorCode.REPAYMENT_PLAN_NOT_FOUND, "period=" + period);
        }

        if (plan.getStatus() == RepaymentStatus.PAID.getCode()) {
            throw new BusinessException(ErrorCode.REPAYMENT_ALREADY_PAID);
        }

        List<RepaymentPlan> plans = List.of(plan);
        return executeRepayment(contract, plans, plan.getTotalAmount());
    }

    private RepaymentResultVO executeRepayment(LoanContract contract, List<RepaymentPlan> pendingPlans, BigDecimal amount) {
        RepaymentResultVO result = new RepaymentResultVO();
        List<RepaymentResultVO.RepaidPeriod> repaidPeriods = new ArrayList<>();
        BigDecimal remainingAmount = amount;
        BigDecimal totalRepaid = BigDecimal.ZERO;

        for (RepaymentPlan plan : pendingPlans) {
            if (remainingAmount.compareTo(plan.getTotalAmount()) >= 0) {
                // 足够还清这一期
                plan.setStatus(RepaymentStatus.PAID.getCode()); // 已还款
                plan.setPaidAt(LocalDateTime.now());
                repaymentPlanMapper.updateById(plan);

                RepaymentResultVO.RepaidPeriod repaidPeriod = new RepaymentResultVO.RepaidPeriod();
                repaidPeriod.setPeriod(plan.getPeriod());
                repaidPeriod.setAmount(plan.getTotalAmount());
                repaidPeriod.setPaidAt(plan.getPaidAt().toString());
                repaidPeriods.add(repaidPeriod);

                remainingAmount = remainingAmount.subtract(plan.getTotalAmount());
                totalRepaid = totalRepaid.add(plan.getTotalAmount());
            } else {
                // 金额不足，停止还款
                break;
            }
        }

        // 检查合同是否已结清
        long remainingPendingPlans = repaymentPlanMapper.selectCount(
                new LambdaQueryWrapper<RepaymentPlan>()
                        .eq(RepaymentPlan::getContractId, contract.getId())
                        .eq(RepaymentPlan::getStatus, RepaymentStatus.PENDING.getCode())
        );

        boolean contractSettled = remainingPendingPlans == 0;
        if (contractSettled) {
            contract.setStatus(ContractStatus.SETTLED.getCode()); // 已结清
            loanContractMapper.updateById(contract);

            // 恢复额度
            recoverCreditLimit(contract.getUserId(), contract.getPrincipal());
        }

        // 计算剩余待还金额
        BigDecimal pendingAmount = repaymentPlanMapper.selectList(
                new LambdaQueryWrapper<RepaymentPlan>()
                        .eq(RepaymentPlan::getContractId, contract.getId())
                        .eq(RepaymentPlan::getStatus, RepaymentStatus.PENDING.getCode())
        ).stream().map(RepaymentPlan::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        result.setSuccess(true);
        result.setMessage("还款成功");
        result.setRepaidAmount(totalRepaid);
        result.setRepaidPeriods(repaidPeriods);
        result.setRemainingAmount(pendingAmount);
        result.setContractSettled(contractSettled);

        log.info("Repayment completed: contractId={}, amount={}, periods={}",
                contract.getId(), totalRepaid, repaidPeriods.size());

        notificationService.send(contract.getUserId(), "USER", "还款成功",
                "您的还款已成功处理，本次还款金额：" + totalRepaid + "元，合同编号：" + contract.getContractNo() + "。",
                "PAYMENT", contract.getId().toString());

        return result;
    }

    private void recoverCreditLimit(Long userId, BigDecimal amount) {
        CreditLimit limit = creditLimitMapper.selectOne(
                new LambdaQueryWrapper<CreditLimit>().eq(CreditLimit::getUserId, userId)
        );

        if (limit != null) {
            limit.setUsedLimit(limit.getUsedLimit().subtract(amount));
            if (limit.getUsedLimit().compareTo(BigDecimal.ZERO) < 0) {
                limit.setUsedLimit(BigDecimal.ZERO);
            }
            limit.setAvailableLimit(limit.getTotalLimit().subtract(limit.getUsedLimit()));
            creditLimitMapper.updateById(limit);
            evictCreditLimitCache(userId);
            log.info("Recovered credit limit for user {}: +{}", userId, amount);
        }
    }

    @Override
    public LimitEstimateVO estimateLimit(Long userId, LimitEstimateDTO dto) {
        LimitEstimateVO vo = new LimitEstimateVO();

        // 基于规则引擎计算预估额度
        BigDecimal baseLimit = calculateEstimatedLimit(dto);

        // 计算信用评分
        int creditScore = calculateCreditScore(dto);

        // 确定风险等级
        String riskLevel = determineRiskLevel(creditScore);

        // 建议期限和利率
        int suggestedTerm = suggestTerm(dto, creditScore);
        BigDecimal referenceRate = calculateReferenceRate(creditScore);

        // 评估因素
        List<LimitEstimateVO.EstimateFactor> factors = buildEstimateFactors(dto, creditScore);

        vo.setEstimatedLimit(baseLimit);
        vo.setCreditScore(creditScore);
        vo.setRiskLevel(riskLevel);
        vo.setSuggestedTerm(suggestedTerm);
        vo.setReferenceRate(referenceRate);
        vo.setFactors(factors);

        return vo;
    }

    @Override
    public LimitEstimateVO predictLimit(Long userId, LimitEstimateDTO dto) {
        // 尝试使用XGBoost模型预测
        try {
            BigDecimal annualIncome = dto.getMonthlyIncome().multiply(new BigDecimal("12"));
            String creditGrade = determineCreditGrade(calculateCreditScore(dto));
            BigDecimal dti = calculateDebtToIncomeRatio(dto);
            BigDecimal interestRate = estimateInterestRate(creditGrade);

            BigDecimal predictedLimit = limitPredictionClient.predictLimitSimple(
                    userId,
                    annualIncome,
                    creditGrade,
                    dti,
                    interestRate
            );

            LimitEstimateVO vo = estimateLimit(userId, dto);
            vo.setEstimatedLimit(predictedLimit);
            log.info("XGBoost predicted limit for user {}: {}", userId, predictedLimit);
            return vo;
        } catch (Exception e) {
            log.warn("Model prediction failed for user {}, using rule-based estimation: {}",
                    userId, e.getMessage());
            return estimateLimit(userId, dto);
        }
    }

    private BigDecimal calculateEstimatedLimit(LimitEstimateDTO dto) {
        // 月收入的3-6倍作为基础额度
        BigDecimal baseMultiplier = new BigDecimal("4");
        BigDecimal baseLimit = dto.getMonthlyIncome().multiply(baseMultiplier);

        // 工作年限加成
        if (dto.getWorkYears() != null && dto.getWorkYears() >= 5) {
            baseLimit = baseLimit.multiply(new BigDecimal("1.2"));
        }

        // 学历加成
        if (dto.getEducation() != null) {
            baseLimit = baseLimit.multiply(getEducationMultiplier(dto.getEducation()));
        }

        // 资产加成
        if (Boolean.TRUE.equals(dto.getHasHouse())) {
            baseLimit = baseLimit.multiply(new BigDecimal("1.3"));
        }
        if (Boolean.TRUE.equals(dto.getHasCar())) {
            baseLimit = baseLimit.multiply(new BigDecimal("1.1"));
        }

        // 负债扣减
        if (dto.getExistingDebt() != null && dto.getExistingDebt().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal debtRatio = dto.getExistingDebt()
                    .divide(dto.getMonthlyIncome().multiply(new BigDecimal("12")), 4, RoundingMode.HALF_UP);
            if (debtRatio.compareTo(new BigDecimal("0.5")) > 0) {
                baseLimit = baseLimit.multiply(new BigDecimal("0.5"));
            }
        }

        // 限制在合理范围内
        if (baseLimit.compareTo(new BigDecimal("500000")) > 0) {
            baseLimit = new BigDecimal("500000");
        }
        if (baseLimit.compareTo(new BigDecimal("1000")) < 0) {
            baseLimit = new BigDecimal("1000");
        }

        return baseLimit.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getEducationMultiplier(String education) {
        return switch (education.toUpperCase()) {
            case "DOCTOR" -> new BigDecimal("1.5");
            case "MASTER" -> new BigDecimal("1.3");
            case "BACHELOR" -> new BigDecimal("1.2");
            case "COLLEGE" -> new BigDecimal("1.1");
            default -> BigDecimal.ONE;
        };
    }

    private int calculateCreditScore(LimitEstimateDTO dto) {
        int score = 600; // 基础分

        // 收入加分
        if (dto.getMonthlyIncome().compareTo(new BigDecimal("30000")) >= 0) {
            score += 80;
        } else if (dto.getMonthlyIncome().compareTo(new BigDecimal("15000")) >= 0) {
            score += 50;
        } else if (dto.getMonthlyIncome().compareTo(new BigDecimal("8000")) >= 0) {
            score += 20;
        }

        // 工作年限加分
        if (dto.getWorkYears() != null) {
            if (dto.getWorkYears() >= 10) {
                score += 60;
            } else if (dto.getWorkYears() >= 5) {
                score += 40;
            } else if (dto.getWorkYears() >= 3) {
                score += 20;
            }
        }

        // 学历加分
        if (dto.getEducation() != null) {
            score += switch (dto.getEducation().toUpperCase()) {
                case "DOCTOR" -> 70;
                case "MASTER" -> 50;
                case "BACHELOR" -> 30;
                case "COLLEGE" -> 15;
                default -> 0;
            };
        }

        // 资产加分
        if (Boolean.TRUE.equals(dto.getHasHouse())) {
            score += 40;
        }
        if (Boolean.TRUE.equals(dto.getHasCar())) {
            score += 20;
        }

        // 负债扣分
        if (dto.getExistingDebt() != null && dto.getExistingDebt().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal debtRatio = calculateDebtToIncomeRatio(dto);
            if (debtRatio.compareTo(new BigDecimal("0.7")) > 0) {
                score -= 80;
            } else if (debtRatio.compareTo(new BigDecimal("0.5")) > 0) {
                score -= 50;
            } else if (debtRatio.compareTo(new BigDecimal("0.3")) > 0) {
                score -= 20;
            }
        }

        // 限制在300-850范围内
        return Math.max(300, Math.min(850, score));
    }

    private BigDecimal calculateDebtToIncomeRatio(LimitEstimateDTO dto) {
        if (dto.getExistingDebt() == null || dto.getExistingDebt().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return dto.getExistingDebt()
                .divide(dto.getMonthlyIncome().multiply(new BigDecimal("12")), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private String determineRiskLevel(int creditScore) {
        if (creditScore >= 750) {
            return "LOW";
        } else if (creditScore >= 650) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }

    private String determineCreditGrade(int creditScore) {
        if (creditScore >= 780) return "A";
        if (creditScore >= 730) return "B";
        if (creditScore >= 670) return "C";
        if (creditScore >= 610) return "D";
        if (creditScore >= 550) return "E";
        if (creditScore >= 490) return "F";
        return "G";
    }

    private int suggestTerm(LimitEstimateDTO dto, int creditScore) {
        // 根据收入和信用评分建议期限
        if (creditScore >= 700 && dto.getMonthlyIncome().compareTo(new BigDecimal("20000")) >= 0) {
            return 24;
        } else if (creditScore >= 650) {
            return 12;
        } else {
            return 6;
        }
    }

    private BigDecimal calculateReferenceRate(int creditScore) {
        if (creditScore >= 750) {
            return new BigDecimal("8.0");
        } else if (creditScore >= 700) {
            return new BigDecimal("10.0");
        } else if (creditScore >= 650) {
            return new BigDecimal("12.0");
        } else if (creditScore >= 600) {
            return new BigDecimal("15.0");
        } else {
            return new BigDecimal("18.0");
        }
    }

    private List<LimitEstimateVO.EstimateFactor> buildEstimateFactors(LimitEstimateDTO dto, int creditScore) {
        List<LimitEstimateVO.EstimateFactor> factors = new ArrayList<>();

        // 收入因素
        LimitEstimateVO.EstimateFactor incomeFactor = new LimitEstimateVO.EstimateFactor();
        incomeFactor.setName("收入水平");
        incomeFactor.setDescription("月收入 " + dto.getMonthlyIncome() + " 元");
        incomeFactor.setImpact(dto.getMonthlyIncome().compareTo(new BigDecimal("15000")) >= 0 ? "POSITIVE" : "NEUTRAL");
        incomeFactor.setWeight(new BigDecimal("0.3"));
        factors.add(incomeFactor);

        // 工作稳定性
        if (dto.getWorkYears() != null) {
            LimitEstimateVO.EstimateFactor workFactor = new LimitEstimateVO.EstimateFactor();
            workFactor.setName("工作稳定性");
            workFactor.setDescription("工作年限 " + dto.getWorkYears() + " 年");
            workFactor.setImpact(dto.getWorkYears() >= 3 ? "POSITIVE" : "NEUTRAL");
            workFactor.setWeight(new BigDecimal("0.2"));
            factors.add(workFactor);
        }

        // 学历
        if (dto.getEducation() != null) {
            LimitEstimateVO.EstimateFactor eduFactor = new LimitEstimateVO.EstimateFactor();
            eduFactor.setName("学历背景");
            eduFactor.setDescription(dto.getEducation());
            eduFactor.setImpact(List.of("BACHELOR", "MASTER", "DOCTOR").contains(dto.getEducation().toUpperCase()) ? "POSITIVE" : "NEUTRAL");
            eduFactor.setWeight(new BigDecimal("0.15"));
            factors.add(eduFactor);
        }

        // 资产
        if (Boolean.TRUE.equals(dto.getHasHouse()) || Boolean.TRUE.equals(dto.getHasCar())) {
            LimitEstimateVO.EstimateFactor assetFactor = new LimitEstimateVO.EstimateFactor();
            assetFactor.setName("资产状况");
            StringBuilder desc = new StringBuilder();
            if (Boolean.TRUE.equals(dto.getHasHouse())) desc.append("有房 ");
            if (Boolean.TRUE.equals(dto.getHasCar())) desc.append("有车");
            assetFactor.setDescription(desc.toString().trim());
            assetFactor.setImpact("POSITIVE");
            assetFactor.setWeight(new BigDecimal("0.2"));
            factors.add(assetFactor);
        }

        // 负债
        if (dto.getExistingDebt() != null && dto.getExistingDebt().compareTo(BigDecimal.ZERO) > 0) {
            LimitEstimateVO.EstimateFactor debtFactor = new LimitEstimateVO.EstimateFactor();
            debtFactor.setName("负债情况");
            debtFactor.setDescription("现有负债 " + dto.getExistingDebt() + " 元");
            BigDecimal debtRatio = calculateDebtToIncomeRatio(dto);
            debtFactor.setImpact(debtRatio.compareTo(new BigDecimal("50")) > 0 ? "NEGATIVE" : "NEUTRAL");
            debtFactor.setWeight(new BigDecimal("0.15"));
            factors.add(debtFactor);
        }

        return factors;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "creditLimit", key = "#userId")
    public CreditLimitVO applyLimitIncrease(Long userId, LimitApplyDTO dto) {
        CreditLimit limit = getOrCreateCreditLimit(userId);

        // 检查申请额度是否合理（不超过当前额度的50%）
        BigDecimal maxIncrease = limit.getTotalLimit().multiply(new BigDecimal("0.5"));
        if (dto.getRequestedLimit().compareTo(maxIncrease) > 0) {
            throw new BusinessException(ErrorCode.LIMIT_INCREASE_TOO_MUCH,
                    "最大可申请额度提升：" + maxIncrease);
        }

        // 简单审批逻辑：根据用户历史还款记录决定
        long completedContracts = loanContractMapper.selectCount(
                new LambdaQueryWrapper<LoanContract>()
                        .eq(LoanContract::getUserId, userId)
                        .eq(LoanContract::getStatus, ContractStatus.SETTLED.getCode()) // 已结清
        );

        if (completedContracts >= 3) {
            // 有良好还款记录，批准提额
            BigDecimal newTotalLimit = limit.getTotalLimit().add(dto.getRequestedLimit());
            limit.setTotalLimit(newTotalLimit);
            limit.setAvailableLimit(newTotalLimit.subtract(limit.getUsedLimit()));
            creditLimitMapper.updateById(limit);

            log.info("Limit increase approved for user {}: +{}", userId, dto.getRequestedLimit());
        } else {
            throw new BusinessException(ErrorCode.LIMIT_INCREASE_REJECTED,
                    "需要有至少3笔已结清的借款记录才能申请提额");
        }

        UserProfile profile = getUserProfile(userId);
        return buildCreditLimitVO(limit, profile);
    }
}
