package com.xindai.xindai.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.admin.dto.ContractAdjustDTO;
import com.xindai.xindai.modules.admin.dto.ContractQueryDTO;
import com.xindai.xindai.modules.admin.dto.DisbursementQueryDTO;
import com.xindai.xindai.modules.admin.dto.DisbursementRejectDTO;
import com.xindai.xindai.modules.admin.service.AdminLoanService;
import com.xindai.xindai.modules.admin.vo.AdminContractDetailVO;
import com.xindai.xindai.modules.admin.vo.AdminContractVO;
import com.xindai.xindai.modules.admin.vo.AdminDisbursementVO;
import com.xindai.xindai.modules.loan.entity.DisbursementRecord;
import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.entity.RepaymentPlan;
import com.xindai.xindai.modules.loan.enums.ContractStatus;
import com.xindai.xindai.modules.loan.enums.DisbursementStatus;
import com.xindai.xindai.modules.loan.enums.RepaymentStatus;
import com.xindai.xindai.modules.loan.mapper.DisbursementRecordMapper;
import com.xindai.xindai.modules.loan.mapper.LoanContractMapper;
import com.xindai.xindai.modules.loan.mapper.RepaymentPlanMapper;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理端贷款管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLoanServiceImpl implements AdminLoanService {

    private final LoanContractMapper loanContractMapper;
    private final DisbursementRecordMapper disbursementRecordMapper;
    private final RepaymentPlanMapper repaymentPlanMapper;
    private final UserMapper userMapper;

    @Override
    public Page<AdminContractVO> getContractList(ContractQueryDTO queryDTO) {
        Page<LoanContract> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        LambdaQueryWrapper<LoanContract> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getContractNo() != null && !queryDTO.getContractNo().isBlank()) {
            wrapper.like(LoanContract::getContractNo, queryDTO.getContractNo());
        }
        if (queryDTO.getUserId() != null) {
            wrapper.eq(LoanContract::getUserId, queryDTO.getUserId());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(LoanContract::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getApplicationId() != null) {
            wrapper.eq(LoanContract::getApplicationId, queryDTO.getApplicationId());
        }
        wrapper.orderByDesc(LoanContract::getCreatedAt);

        Page<LoanContract> contractPage = loanContractMapper.selectPage(page, wrapper);

        // 获取用户信息
        Set<Long> userIds = contractPage.getRecords().stream()
                .map(LoanContract::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of() :
                userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        // 获取还款统计
        Set<Long> contractIds = contractPage.getRecords().stream()
                .map(LoanContract::getId)
                .collect(Collectors.toSet());
        Map<Long, RepaymentStats> statsMap = contractIds.isEmpty() ? Map.of() :
                calculateRepaymentStats(contractIds);

        // 转换为VO
        Page<AdminContractVO> voPage = new Page<>(contractPage.getCurrent(), contractPage.getSize(), contractPage.getTotal());
        List<AdminContractVO> voList = contractPage.getRecords().stream()
                .map(contract -> convertToVO(contract, userMap.get(contract.getUserId()), statsMap.get(contract.getId())))
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public AdminContractDetailVO getContractDetail(Long id) {
        LoanContract contract = loanContractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_FOUND);
        }

        User user = userMapper.selectById(contract.getUserId());

        // 获取还款计划
        List<RepaymentPlan> repaymentPlans = repaymentPlanMapper.selectList(
                new LambdaQueryWrapper<RepaymentPlan>()
                        .eq(RepaymentPlan::getContractId, id)
                        .orderByAsc(RepaymentPlan::getPeriod)
        );

        // 计算还款统计
        Map<Long, RepaymentStats> statsMap = calculateRepaymentStats(Set.of(id));
        RepaymentStats stats = statsMap.getOrDefault(id, new RepaymentStats());

        // 转换为详情VO
        AdminContractDetailVO vo = new AdminContractDetailVO();
        vo.setId(contract.getId());
        vo.setContractNo(contract.getContractNo());
        vo.setUserId(contract.getUserId());
        vo.setUserName(user != null ? user.getRealName() : null);
        vo.setUserPhone(user != null ? user.getPhone() : null);
        vo.setApplicationId(contract.getApplicationId());
        vo.setPrincipal(contract.getPrincipal());
        vo.setInterestRate(contract.getInterestRate());
        vo.setTotalRepayment(contract.getTotalRepayment());
        vo.setTerm(contract.getTerm());
        vo.setStatus(contract.getStatus());
        vo.setDisbursedAt(contract.getDisbursedAt());
        vo.setDueDate(contract.getDueDate());
        vo.setCreatedAt(contract.getCreatedAt());
        vo.setPaidPeriods(stats.paidPeriods);
        vo.setPendingPeriods(stats.pendingPeriods);
        vo.setOverduePeriods(stats.overduePeriods);
        vo.setPaidAmount(stats.paidAmount);
        vo.setRemainingAmount(stats.remainingAmount);

        // 转换还款计划
        List<AdminContractDetailVO.RepaymentSummaryVO> planVOs = repaymentPlans.stream()
                .map(plan -> {
                    AdminContractDetailVO.RepaymentSummaryVO planVO = new AdminContractDetailVO.RepaymentSummaryVO();
                    planVO.setPeriod(plan.getPeriod());
                    planVO.setDueDate(plan.getDueDate());
                    planVO.setPrincipal(plan.getPrincipal());
                    planVO.setInterest(plan.getInterest());
                    planVO.setTotalAmount(plan.getTotalAmount());
                    planVO.setStatus(plan.getStatus());
                    planVO.setOverdueDays(plan.getOverdueDays());
                    planVO.setPenaltyAmount(plan.getPenaltyAmount());
                    planVO.setPaidAt(plan.getPaidAt());
                    return planVO;
                })
                .collect(Collectors.toList());
        vo.setRepaymentPlans(planVOs);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelContract(Long id) {
        LoanContract contract = loanContractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_FOUND);
        }

        // 检查状态是否可以取消
        if (contract.getStatus() != ContractStatus.PENDING.getCode()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有待放款状态的合同可以取消");
        }

        // 检查是否有还款记录
        long repaymentCount = repaymentPlanMapper.selectCount(
                new LambdaQueryWrapper<RepaymentPlan>()
                        .eq(RepaymentPlan::getContractId, id)
                        .eq(RepaymentPlan::getStatus, RepaymentStatus.PAID.getCode())
        );
        if (repaymentCount > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已有还款记录的合同不能取消");
        }

        // 更新合同状态为已结清（取消）
        LambdaUpdateWrapper<LoanContract> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(LoanContract::getId, id)
                .set(LoanContract::getStatus, ContractStatus.SETTLED.getCode());

        loanContractMapper.update(null, wrapper);
        log.info("管理员取消合同: contractId={}, adminId={}", id, getCurrentAdminId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustContractRate(Long id, ContractAdjustDTO adjustDTO) {
        LoanContract contract = loanContractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_FOUND);
        }

        // 检查状态是否可以调整
        if (contract.getStatus() != ContractStatus.REPAYING.getCode()
                && contract.getStatus() != ContractStatus.PENDING.getCode()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有还款中或待放款状态的合同可以调整利率");
        }

        BigDecimal newRate = adjustDTO.getNewInterestRate();
        if (newRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "利率不能为负数");
        }

        // 更新利率
        LambdaUpdateWrapper<LoanContract> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(LoanContract::getId, id)
                .set(LoanContract::getInterestRate, newRate);

        loanContractMapper.update(null, wrapper);
        log.info("管理员调整合同利率: contractId={}, oldRate={}, newRate={}, reason={}, adminId={}",
                id, contract.getInterestRate(), newRate, adjustDTO.getReason(), getCurrentAdminId());
    }

    @Override
    public Page<AdminDisbursementVO> getDisbursementList(DisbursementQueryDTO queryDTO) {
        Page<DisbursementRecord> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        LambdaQueryWrapper<DisbursementRecord> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getContractId() != null) {
            wrapper.eq(DisbursementRecord::getContractId, queryDTO.getContractId());
        }
        if (queryDTO.getUserId() != null) {
            wrapper.eq(DisbursementRecord::getUserId, queryDTO.getUserId());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(DisbursementRecord::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getApplicationId() != null) {
            wrapper.eq(DisbursementRecord::getApplicationId, queryDTO.getApplicationId());
        }
        wrapper.orderByDesc(DisbursementRecord::getCreatedAt);

        Page<DisbursementRecord> disbursementPage = disbursementRecordMapper.selectPage(page, wrapper);

        // 获取用户信息
        Set<Long> userIds = disbursementPage.getRecords().stream()
                .map(DisbursementRecord::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of() :
                userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        // 获取合同信息
        Set<Long> contractIds = disbursementPage.getRecords().stream()
                .map(DisbursementRecord::getContractId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, LoanContract> contractMap = contractIds.isEmpty() ? Map.of() :
                loanContractMapper.selectBatchIds(contractIds).stream()
                        .collect(Collectors.toMap(LoanContract::getId, c -> c));

        // 转换为VO
        Page<AdminDisbursementVO> voPage = new Page<>(disbursementPage.getCurrent(), disbursementPage.getSize(), disbursementPage.getTotal());
        List<AdminDisbursementVO> voList = disbursementPage.getRecords().stream()
                .map(record -> convertToVO(record, userMap.get(record.getUserId()), contractMap.get(record.getContractId())))
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeDisbursement(Long id) {
        DisbursementRecord record = disbursementRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.DISBURSEMENT_NOT_FOUND);
        }

        // 检查状态是否可以执行
        if (record.getStatus() != DisbursementStatus.PENDING.getCode()) {
            throw new BusinessException(ErrorCode.DISBURSEMENT_INVALID_STATUS, "只有待放款状态的记录可以执行");
        }

        // 模拟放款执行
        LambdaUpdateWrapper<DisbursementRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DisbursementRecord::getId, id)
                .set(DisbursementRecord::getStatus, DisbursementStatus.COMPLETED.getCode())
                .set(DisbursementRecord::getTransactionNo, "MOCK_TXN_" + System.currentTimeMillis())
                .set(DisbursementRecord::getCompletedAt, LocalDateTime.now());

        disbursementRecordMapper.update(null, wrapper);

        // 更新合同状态为还款中
        if (record.getContractId() != null) {
            LambdaUpdateWrapper<LoanContract> contractWrapper = new LambdaUpdateWrapper<>();
            contractWrapper.eq(LoanContract::getId, record.getContractId())
                    .set(LoanContract::getStatus, ContractStatus.REPAYING.getCode())
                    .set(LoanContract::getDisbursedAt, LocalDateTime.now());
            loanContractMapper.update(null, contractWrapper);
        }

        log.info("管理员执行放款: disbursementId={}, contractId={}, adminId={}",
                id, record.getContractId(), getCurrentAdminId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectDisbursement(Long id, DisbursementRejectDTO rejectDTO) {
        DisbursementRecord record = disbursementRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.DISBURSEMENT_NOT_FOUND);
        }

        // 检查状态是否可以拒绝
        if (record.getStatus() != DisbursementStatus.PENDING.getCode()) {
            throw new BusinessException(ErrorCode.DISBURSEMENT_INVALID_STATUS, "只有待放款状态的记录可以拒绝");
        }

        // 更新为放款失败
        LambdaUpdateWrapper<DisbursementRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DisbursementRecord::getId, id)
                .set(DisbursementRecord::getStatus, DisbursementStatus.FAILED.getCode())
                .set(DisbursementRecord::getFailedReason, rejectDTO.getReason())
                .set(DisbursementRecord::getCompletedAt, LocalDateTime.now());

        disbursementRecordMapper.update(null, wrapper);

        log.info("管理员拒绝放款: disbursementId={}, contractId={}, reason={}, adminId={}",
                id, record.getContractId(), rejectDTO.getReason(), getCurrentAdminId());
    }

    private Long getCurrentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    private AdminContractVO convertToVO(LoanContract contract, User user, RepaymentStats stats) {
        AdminContractVO vo = new AdminContractVO();
        vo.setId(contract.getId());
        vo.setContractNo(contract.getContractNo());
        vo.setUserId(contract.getUserId());
        vo.setUserName(user != null ? user.getRealName() : null);
        vo.setApplicationId(contract.getApplicationId());
        vo.setPrincipal(contract.getPrincipal());
        vo.setInterestRate(contract.getInterestRate());
        vo.setTotalRepayment(contract.getTotalRepayment());
        vo.setTerm(contract.getTerm());
        vo.setStatus(contract.getStatus());
        vo.setDisbursedAt(contract.getDisbursedAt());
        vo.setDueDate(contract.getDueDate());
        vo.setCreatedAt(contract.getCreatedAt());

        if (stats != null) {
            vo.setPaidPeriods(stats.paidPeriods);
            vo.setTotalPeriods(contract.getTerm());
            vo.setRemainingAmount(stats.remainingAmount);
        } else {
            vo.setTotalPeriods(contract.getTerm());
        }

        return vo;
    }

    private AdminDisbursementVO convertToVO(DisbursementRecord record, User user, LoanContract contract) {
        AdminDisbursementVO vo = new AdminDisbursementVO();
        vo.setId(record.getId());
        vo.setContractId(record.getContractId());
        vo.setContractNo(contract != null ? contract.getContractNo() : null);
        vo.setUserId(record.getUserId());
        vo.setUserName(user != null ? user.getRealName() : null);
        vo.setApplicationId(record.getApplicationId());
        vo.setAmount(record.getAmount());
        vo.setBankAccountId(record.getBankAccountId());
        vo.setStatus(record.getStatus());
        vo.setTransactionNo(record.getTransactionNo());
        vo.setCompletedAt(record.getCompletedAt());
        vo.setFailedReason(record.getFailedReason());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }

    private Map<Long, RepaymentStats> calculateRepaymentStats(Set<Long> contractIds) {
        if (contractIds.isEmpty()) {
            return Map.of();
        }

        // 获取所有还款计划
        List<RepaymentPlan> plans = repaymentPlanMapper.selectList(
                new LambdaQueryWrapper<RepaymentPlan>()
                        .in(RepaymentPlan::getContractId, contractIds)
        );

        Map<Long, RepaymentStats> statsMap = new HashMap<>();
        for (Long contractId : contractIds) {
            statsMap.put(contractId, new RepaymentStats());
        }

        for (RepaymentPlan plan : plans) {
            RepaymentStats stats = statsMap.get(plan.getContractId());
            if (stats == null) {
                stats = new RepaymentStats();
                statsMap.put(plan.getContractId(), stats);
            }

            stats.totalAmount = stats.totalAmount.add(plan.getTotalAmount());
            if (plan.getPenaltyAmount() != null) {
                stats.totalPenalty = stats.totalPenalty.add(plan.getPenaltyAmount());
            }

            if (plan.getStatus() == RepaymentStatus.PAID.getCode()) {
                stats.paidPeriods++;
                stats.paidAmount = stats.paidAmount.add(plan.getTotalAmount());
            } else if (plan.getStatus() == RepaymentStatus.OVERDUE.getCode()) {
                stats.overduePeriods++;
            } else {
                stats.pendingPeriods++;
                stats.remainingAmount = stats.remainingAmount.add(plan.getTotalAmount());
            }
        }

        // 计算剩余应还金额（包含罚息）
        for (RepaymentStats stats : statsMap.values()) {
            stats.remainingAmount = stats.remainingAmount.add(stats.totalPenalty);
        }

        return statsMap;
    }

    private static class RepaymentStats {
        int paidPeriods = 0;
        int pendingPeriods = 0;
        int overduePeriods = 0;
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal remainingAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalPenalty = BigDecimal.ZERO;
    }
}
