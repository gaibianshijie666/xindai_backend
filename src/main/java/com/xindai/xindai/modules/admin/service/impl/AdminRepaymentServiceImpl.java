package com.xindai.xindai.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.admin.dto.ManualRepayDTO;
import com.xindai.xindai.modules.admin.dto.RepaymentAdjustDTO;
import com.xindai.xindai.modules.admin.dto.RepaymentQueryDTO;
import com.xindai.xindai.modules.admin.service.AdminRepaymentService;
import com.xindai.xindai.modules.admin.vo.AdminRepaymentVO;
import com.xindai.xindai.modules.admin.vo.OverdueRepaymentVO;
import com.xindai.xindai.modules.admin.vo.RepaymentStatsVO;
import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.entity.RepaymentPlan;
import com.xindai.xindai.modules.loan.enums.ContractStatus;
import com.xindai.xindai.modules.loan.enums.RepaymentStatus;
import com.xindai.xindai.modules.loan.mapper.LoanContractMapper;
import com.xindai.xindai.modules.loan.mapper.RepaymentPlanMapper;
import com.xindai.xindai.modules.loan.service.CreditLimitService;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理端还款管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRepaymentServiceImpl implements AdminRepaymentService {

    private final RepaymentPlanMapper repaymentPlanMapper;
    private final LoanContractMapper loanContractMapper;
    private final UserMapper userMapper;
    private final CreditLimitService creditLimitService;

    @Override
    public Page<AdminRepaymentVO> getRepaymentList(RepaymentQueryDTO queryDTO) {
        Page<RepaymentPlan> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        LambdaQueryWrapper<RepaymentPlan> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getStatus() != null) {
            wrapper.eq(RepaymentPlan::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getContractId() != null) {
            wrapper.eq(RepaymentPlan::getContractId, queryDTO.getContractId());
        }
        if (queryDTO.getDueDateStart() != null) {
            wrapper.ge(RepaymentPlan::getDueDate, queryDTO.getDueDateStart());
        }
        if (queryDTO.getDueDateEnd() != null) {
            wrapper.le(RepaymentPlan::getDueDate, queryDTO.getDueDateEnd());
        }
        wrapper.orderByAsc(RepaymentPlan::getDueDate);

        Page<RepaymentPlan> planPage = repaymentPlanMapper.selectPage(page, wrapper);

        // 获取合同信息
        Set<Long> contractIds = planPage.getRecords().stream()
                .map(RepaymentPlan::getContractId)
                .collect(Collectors.toSet());
        Map<Long, LoanContract> contractMap = contractIds.isEmpty() ? Map.of() :
                loanContractMapper.selectBatchIds(contractIds).stream()
                        .collect(Collectors.toMap(LoanContract::getId, c -> c));

        // 获取用户信息
        Set<Long> userIds = planPage.getRecords().stream()
                .map(plan -> contractMap.get(plan.getContractId()) != null
                        ? contractMap.get(plan.getContractId()).getUserId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of() :
                userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        // 转换为VO
        Page<AdminRepaymentVO> voPage = new Page<>(planPage.getCurrent(), planPage.getSize(), planPage.getTotal());
        List<AdminRepaymentVO> voList = planPage.getRecords().stream()
                .map(plan -> convertToVO(plan, contractMap.get(plan.getContractId()), userMap))
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public List<OverdueRepaymentVO> getOverdueRepayments() {
        List<RepaymentPlan> overduePlans = repaymentPlanMapper.selectList(
                new LambdaQueryWrapper<RepaymentPlan>()
                        .eq(RepaymentPlan::getStatus, RepaymentStatus.OVERDUE.getCode())
                        .orderByAsc(RepaymentPlan::getDueDate)
        );

        if (overduePlans.isEmpty()) {
            return List.of();
        }

        // 获取合同信息
        Set<Long> contractIds = overduePlans.stream()
                .map(RepaymentPlan::getContractId)
                .collect(Collectors.toSet());
        Map<Long, LoanContract> contractMap = loanContractMapper.selectBatchIds(contractIds).stream()
                .collect(Collectors.toMap(LoanContract::getId, c -> c));

        // 获取用户信息
        Set<Long> userIds = contractMap.values().stream()
                .map(LoanContract::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 转换为VO
        return overduePlans.stream()
                .map(plan -> {
                    LoanContract contract = contractMap.get(plan.getContractId());
                    User user = contract != null ? userMap.get(contract.getUserId()) : null;
                    return convertToOverdueVO(plan, contract, user);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustPenalty(Long id, RepaymentAdjustDTO adjustDTO) {
        RepaymentPlan plan = repaymentPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(ErrorCode.REPAYMENT_PLAN_NOT_FOUND);
        }

        // 检查状态是否为逾期
        if (plan.getStatus() != RepaymentStatus.OVERDUE.getCode()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有逾期状态的还款计划可以调整罚息");
        }

        BigDecimal newPenalty = adjustDTO.getPenaltyAmount();
        if (newPenalty.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "罚息金额不能为负数");
        }

        // 更新罚息
        LambdaUpdateWrapper<RepaymentPlan> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepaymentPlan::getId, id)
                .set(RepaymentPlan::getPenaltyAmount, newPenalty);

        repaymentPlanMapper.update(null, wrapper);
        log.info("管理员调整罚息: planId={}, oldPenalty={}, newPenalty={}, reason={}, adminId={}",
                id, plan.getPenaltyAmount(), newPenalty, adjustDTO.getReason(), getCurrentAdminId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualRepay(Long id, ManualRepayDTO repayDTO) {
        RepaymentPlan plan = repaymentPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(ErrorCode.REPAYMENT_PLAN_NOT_FOUND);
        }

        // 检查是否已还款
        if (plan.getStatus() == RepaymentStatus.PAID.getCode()) {
            throw new BusinessException(ErrorCode.REPAYMENT_ALREADY_PAID);
        }

        // 设置为已还款
        LambdaUpdateWrapper<RepaymentPlan> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepaymentPlan::getId, id)
                .set(RepaymentPlan::getStatus, RepaymentStatus.PAID.getCode())
                .set(RepaymentPlan::getPaidAt, LocalDateTime.now());

        repaymentPlanMapper.update(null, wrapper);
        log.info("管理员手动还款: planId={}, contractId={}, amount={}, method={}, remark={}, adminId={}",
                id, plan.getContractId(), repayDTO.getActualAmount(), repayDTO.getPaymentMethod(),
                repayDTO.getRemark(), getCurrentAdminId());

        // 检查合同是否全部还清
        checkAndSettleContract(plan.getContractId());
    }

    @Override
    public RepaymentStatsVO getRepaymentStats() {
        // 使用数据库聚合查询统计
        Map<Integer, List<RepaymentPlan>> statusGroups = Optional.ofNullable(
                repaymentPlanMapper.selectList(new LambdaQueryWrapper<>())
        ).orElse(List.of()).stream()
                .collect(Collectors.groupingBy(RepaymentPlan::getStatus));

        List<RepaymentPlan> paidPlans = statusGroups.getOrDefault(RepaymentStatus.PAID.getCode(), List.of());
        List<RepaymentPlan> pendingPlans = statusGroups.getOrDefault(RepaymentStatus.PENDING.getCode(), List.of());
        List<RepaymentPlan> overduePlans = statusGroups.getOrDefault(RepaymentStatus.OVERDUE.getCode(), List.of());

        // 累计已收金额
        BigDecimal totalCollected = paidPlans.stream()
                .map(RepaymentPlan::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 待收金额
        BigDecimal outstanding = pendingPlans.stream()
                .map(RepaymentPlan::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 逾期金额（包含罚息）
        BigDecimal overdueAmount = overduePlans.stream()
                .map(plan -> {
                    BigDecimal penalty = plan.getPenaltyAmount() != null ? plan.getPenaltyAmount() : BigDecimal.ZERO;
                    return plan.getTotalAmount().add(penalty);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 总应还金额
        BigDecimal totalDue = totalCollected.add(outstanding).add(overdueAmount);

        // 还款率
        BigDecimal collectionRate = totalDue.compareTo(BigDecimal.ZERO) > 0
                ? totalCollected.divide(totalDue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        RepaymentStatsVO stats = new RepaymentStatsVO();
        stats.setTotalCollected(totalCollected);
        stats.setOutstanding(outstanding);
        stats.setOverdueAmount(overdueAmount);
        stats.setCollectionRate(collectionRate);
        stats.setPendingCount((long) pendingPlans.size());
        stats.setPaidCount((long) paidPlans.size());
        stats.setOverdueCount((long) overduePlans.size());

        return stats;
    }

    /**
     * 检查合同是否全部还清，如果是则更新合同状态并恢复额度
     */
    private void checkAndSettleContract(Long contractId) {
        // 查询该合同下的所有还款计划
        List<RepaymentPlan> plans = repaymentPlanMapper.selectList(
                new LambdaQueryWrapper<RepaymentPlan>()
                        .eq(RepaymentPlan::getContractId, contractId)
        );

        // 检查是否全部已还
        boolean allPaid = plans.stream()
                .allMatch(plan -> plan.getStatus() == RepaymentStatus.PAID.getCode());

        if (allPaid) {
            // 更新合同状态为已结清
            LoanContract contract = loanContractMapper.selectById(contractId);
            if (contract != null && contract.getStatus() != ContractStatus.SETTLED.getCode()) {
                LambdaUpdateWrapper<LoanContract> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(LoanContract::getId, contractId)
                        .set(LoanContract::getStatus, ContractStatus.SETTLED.getCode());
                loanContractMapper.update(null, wrapper);

                // 恢复用户额度
                creditLimitService.recoverLimit(contract.getUserId(), contract.getPrincipal());
                log.info("合同已结清，恢复用户额度: contractId={}, userId={}, amount={}",
                        contractId, contract.getUserId(), contract.getPrincipal());
            }
        }
    }

    private Long getCurrentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    private AdminRepaymentVO convertToVO(RepaymentPlan plan, LoanContract contract, Map<Long, User> userMap) {
        AdminRepaymentVO vo = new AdminRepaymentVO();
        vo.setId(plan.getId());
        vo.setContractId(plan.getContractId());
        vo.setContractNo(contract != null ? contract.getContractNo() : null);
        vo.setUserId(contract != null ? contract.getUserId() : null);
        vo.setPeriod(plan.getPeriod());
        vo.setDueDate(plan.getDueDate());
        vo.setPrincipal(plan.getPrincipal());
        vo.setInterest(plan.getInterest());
        vo.setTotalAmount(plan.getTotalAmount());
        vo.setStatus(plan.getStatus());
        vo.setStatusDesc(RepaymentStatus.fromCode(plan.getStatus()).getDesc());
        vo.setPenaltyAmount(plan.getPenaltyAmount());
        vo.setOverdueDays(plan.getOverdueDays());
        vo.setPaidAt(plan.getPaidAt());

        if (contract != null) {
            User user = userMap.get(contract.getUserId());
            vo.setUserName(user != null ? user.getRealName() : null);
            vo.setUserPhone(user != null ? user.getPhone() : null);
        }

        return vo;
    }

    private OverdueRepaymentVO convertToOverdueVO(RepaymentPlan plan, LoanContract contract, User user) {
        OverdueRepaymentVO vo = new OverdueRepaymentVO();
        vo.setId(plan.getId());
        vo.setContractId(plan.getContractId());
        vo.setContractNo(contract != null ? contract.getContractNo() : null);
        vo.setUserId(contract != null ? contract.getUserId() : null);
        vo.setUserName(user != null ? user.getRealName() : null);
        vo.setUserPhone(user != null ? user.getPhone() : null);
        vo.setPeriod(plan.getPeriod());
        vo.setDueDate(plan.getDueDate());
        vo.setTotalAmount(plan.getTotalAmount());
        vo.setPenaltyAmount(plan.getPenaltyAmount() != null ? plan.getPenaltyAmount() : BigDecimal.ZERO);
        vo.setOverdueDays(plan.getOverdueDays() != null ? plan.getOverdueDays() : 0);

        // 计算合计应还
        BigDecimal totalWithPenalty = plan.getTotalAmount()
                .add(vo.getPenaltyAmount());
        vo.setTotalWithPenalty(totalWithPenalty);

        return vo;
    }
}
