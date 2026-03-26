package com.xindai.xindai.modules.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.enterprise.dto.*;
import com.xindai.xindai.modules.enterprise.entity.Enterprise;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseCustomer;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseOperationLog;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseCustomerMapper;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseMapper;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseOperationLogMapper;
import com.xindai.xindai.modules.enterprise.service.EnterpriseLoanService;
import com.xindai.xindai.modules.loan.entity.LoanApplication;
import com.xindai.xindai.modules.loan.enums.ApplicationStatus;
import com.xindai.xindai.modules.loan.mapper.LoanApplicationMapper;
import com.xindai.xindai.modules.risk.service.RiskAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 企业借款服务实现
 */
@Service
@RequiredArgsConstructor
public class EnterpriseLoanServiceImpl implements EnterpriseLoanService {

    private final LoanApplicationMapper loanApplicationMapper;
    private final EnterpriseCustomerMapper customerMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final EnterpriseOperationLogMapper logMapper;
    private final RiskAssessmentService riskAssessmentService;

    @Override
    public Page<EnterpriseLoanVO> list(Long enterpriseId, EnterpriseLoanQueryDTO queryDTO) {
        LambdaQueryWrapper<LoanApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoanApplication::getEnterpriseId, enterpriseId);
        if (queryDTO.getStatus() != null) {
            wrapper.eq(LoanApplication::getStatus, queryDTO.getStatus());
        }
        wrapper.orderByDesc(LoanApplication::getCreatedAt);

        Page<LoanApplication> pageParam = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        Page<LoanApplication> result = loanApplicationMapper.selectPage(pageParam, wrapper);

        // 转换为VO
        Page<EnterpriseLoanVO> voPage = new Page<>();
        voPage.setTotal(result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EnterpriseLoanVO apply(Long enterpriseId, Long userId, EnterpriseLoanApplyDTO dto) {
        // 1. 获取客户信息
        EnterpriseCustomer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<EnterpriseCustomer>()
                        .eq(EnterpriseCustomer::getEnterpriseId, enterpriseId)
                        .eq(EnterpriseCustomer::getId, dto.getEnterpriseCustomerId())
        );
        if (customer == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_CUSTOMER_NOT_FOUND);
        }

        // 2. 检查企业额度
        Enterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise.getUsedLimit().add(dto.getAmount()).compareTo(enterprise.getCreditLimit()) > 0) {
            throw new BusinessException(ErrorCode.ENTERPRISE_CREDIT_LIMIT_EXCEEDED);
        }

        // 3. 创建借款申请
        LoanApplication application = new LoanApplication();
        application.setEnterpriseId(enterpriseId);
        application.setEnterpriseCustomerId(dto.getEnterpriseCustomerId());
        application.setAmount(dto.getAmount());
        application.setTerm(dto.getTerm());
        application.setPurpose(dto.getPurpose());
        application.setStatus(ApplicationStatus.PENDING.getCode()); // 待审批
        application.setApplicationNo(generateApplicationNo());
        application.setCreatedAt(LocalDateTime.now());
        loanApplicationMapper.insert(application);

        // 4. 更新企业已用额度
        enterprise.setUsedLimit(enterprise.getUsedLimit().add(dto.getAmount()));
        enterpriseMapper.updateById(enterprise);

        // 5. 记录操作日志
        saveOperationLog(enterpriseId, userId, "LOAN_APPLY", application.getId());

        return toVOWithCustomer(application, customer);
    }

    @Override
    public EnterpriseLoanVO getById(Long enterpriseId, Long loanId) {
        LoanApplication application = loanApplicationMapper.selectOne(
                new LambdaQueryWrapper<LoanApplication>()
                        .eq(LoanApplication::getEnterpriseId, enterpriseId)
                        .eq(LoanApplication::getId, loanId)
        );
        if (application == null) {
            throw new BusinessException(ErrorCode.LOAN_NOT_FOUND);
        }

        EnterpriseCustomer customer = customerMapper.selectById(application.getEnterpriseCustomerId());
        return toVOWithCustomer(application, customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResultVO batchApply(Long enterpriseId, Long userId, List<EnterpriseLoanApplyDTO> dtos) {
        List<BatchOperationResultVO.FailDetail> failDetails = new ArrayList<>();
        int successCount = 0;

        // 预先获取企业信息
        Enterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }

        // 计算总申请金额
        BigDecimal totalAmount = dtos.stream()
                .map(EnterpriseLoanApplyDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 检查企业额度
        if (enterprise.getUsedLimit().add(totalAmount).compareTo(enterprise.getCreditLimit()) > 0) {
            throw new BusinessException(ErrorCode.ENTERPRISE_CREDIT_LIMIT_EXCEEDED, "批量申请总额超过企业剩余额度");
        }

        for (EnterpriseLoanApplyDTO dto : dtos) {
            try {
                // 获取客户信息
                EnterpriseCustomer customer = customerMapper.selectOne(
                        new LambdaQueryWrapper<EnterpriseCustomer>()
                                .eq(EnterpriseCustomer::getEnterpriseId, enterpriseId)
                                .eq(EnterpriseCustomer::getId, dto.getEnterpriseCustomerId())
                );
                if (customer == null) {
                    failDetails.add(new BatchOperationResultVO.FailDetail(dto.getEnterpriseCustomerId(), "客户不存在"));
                    continue;
                }

                // 创建借款申请
                LoanApplication application = new LoanApplication();
                application.setEnterpriseId(enterpriseId);
                application.setEnterpriseCustomerId(dto.getEnterpriseCustomerId());
                application.setAmount(dto.getAmount());
                application.setTerm(dto.getTerm());
                application.setPurpose(dto.getPurpose());
                application.setStatus(ApplicationStatus.PENDING.getCode()); // 待审批
                application.setApplicationNo(generateApplicationNo());
                application.setCreatedAt(LocalDateTime.now());
                loanApplicationMapper.insert(application);

                // 更新企业已用额度
                enterprise.setUsedLimit(enterprise.getUsedLimit().add(dto.getAmount()));

                // 记录操作日志
                saveOperationLog(enterpriseId, userId, "BATCH_LOAN_APPLY", application.getId());

                successCount++;
            } catch (Exception e) {
                failDetails.add(new BatchOperationResultVO.FailDetail(dto.getEnterpriseCustomerId(), "申请失败: " + e.getMessage()));
            }
        }

        // 批量更新企业已用额度
        if (successCount > 0) {
            enterpriseMapper.updateById(enterprise);
        }

        return BatchOperationResultVO.of(dtos.size(), successCount, failDetails);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResultVO batchReview(Long enterpriseId, Long userId, BatchReviewDTO dto) {
        List<BatchOperationResultVO.FailDetail> failDetails = new ArrayList<>();
        int successCount = 0;

        for (Long loanId : dto.getIds()) {
            try {
                LoanApplication application = loanApplicationMapper.selectOne(
                        new LambdaQueryWrapper<LoanApplication>()
                                .eq(LoanApplication::getEnterpriseId, enterpriseId)
                                .eq(LoanApplication::getId, loanId)
                );
                if (application == null) {
                    failDetails.add(new BatchOperationResultVO.FailDetail(loanId, "借款申请不存在或无权操作"));
                    continue;
                }

                // 只有待审批状态才能审核
                if (application.getStatus() != ApplicationStatus.PENDING.getCode()) {
                    failDetails.add(new BatchOperationResultVO.FailDetail(loanId, "该借款申请已审核，当前状态: " + getStatusText(application.getStatus())));
                    continue;
                }

                application.setStatus(dto.getStatus());
                application.setReviewedAt(LocalDateTime.now());
                loanApplicationMapper.updateById(application);

                // 记录操作日志
                saveOperationLog(enterpriseId, userId, "BATCH_LOAN_REVIEW", application.getId());

                successCount++;
            } catch (Exception e) {
                failDetails.add(new BatchOperationResultVO.FailDetail(loanId, "审核失败: " + e.getMessage()));
            }
        }

        return BatchOperationResultVO.of(dto.getIds().size(), successCount, failDetails);
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        return ApplicationStatus.fromCode(status).getDesc();
    }

    private String generateApplicationNo() {
        return "EL" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
    }

    private EnterpriseLoanVO toVO(LoanApplication app) {
        EnterpriseCustomer customer = customerMapper.selectById(app.getEnterpriseCustomerId());
        return toVOWithCustomer(app, customer);
    }

    private EnterpriseLoanVO toVOWithCustomer(LoanApplication app, EnterpriseCustomer customer) {
        EnterpriseLoanVO vo = new EnterpriseLoanVO();
        BeanUtils.copyProperties(app, vo);
        if (customer != null) {
            vo.setCustomerName(customer.getRealName());
            vo.setCustomerIdCard(customer.getIdCard());
        }
        return vo;
    }

    private void saveOperationLog(Long enterpriseId, Long userId, String operationType, Long targetId) {
        EnterpriseOperationLog log = new EnterpriseOperationLog();
        log.setEnterpriseId(enterpriseId);
        log.setUserId(userId);
        log.setOperationType(operationType);
        log.setTargetType("LOAN_APPLICATION");
        log.setTargetId(targetId);
        log.setCreatedAt(LocalDateTime.now());
        logMapper.insert(log);
    }
}
