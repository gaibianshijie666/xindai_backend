package com.xindai.xindai.modules.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.enterprise.dto.CreditApplyDTO;
import com.xindai.xindai.modules.enterprise.dto.CreditInfoVO;
import com.xindai.xindai.modules.enterprise.entity.Enterprise;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseOperationLog;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseMapper;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseOperationLogMapper;
import com.xindai.xindai.modules.enterprise.service.EnterpriseCreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EnterpriseCreditServiceImpl implements EnterpriseCreditService {

    private final EnterpriseMapper enterpriseMapper;
    private final EnterpriseOperationLogMapper operationLogMapper;

    @Override
    public CreditInfoVO getCreditInfo(Long enterpriseId) {
        Enterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }

        CreditInfoVO vo = new CreditInfoVO();
        vo.setCreditLimit(enterprise.getCreditLimit());
        vo.setUsedLimit(enterprise.getUsedLimit());
        BigDecimal available = enterprise.getCreditLimit().subtract(enterprise.getUsedLimit());
        vo.setAvailableLimit(available.compareTo(BigDecimal.ZERO) > 0 ? available : BigDecimal.ZERO);
        vo.setExpireAt(enterprise.getExpireAt());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyCreditIncrease(Long enterpriseId, CreditApplyDTO dto) {
        Enterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }

        if (dto.getRequestedLimit().compareTo(enterprise.getCreditLimit()) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "申请额度必须大于当前额度");
        }

        // M1: 信用额度增加保护 - 限制最大2倍增长
        BigDecimal maxAllowedLimit = enterprise.getCreditLimit().multiply(BigDecimal.valueOf(2));
        if (dto.getRequestedLimit().compareTo(maxAllowedLimit) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "申请额度不能超过当前额度的2倍");
        }

        if (dto.getRequestedLimit().compareTo(enterprise.getUsedLimit()) < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "申请额度不能低于已使用额度");
        }

        // 记录操作日志
        EnterpriseOperationLog operationLog = new EnterpriseOperationLog();
        operationLog.setEnterpriseId(enterpriseId);
        operationLog.setOperationType("CREDIT_INCREASE_APPLY");
        operationLog.setTargetType("ENTERPRISE");
        operationLog.setTargetId(enterpriseId);
        operationLog.setDetail("申请额度提升: " + enterprise.getCreditLimit() + " -> " + dto.getRequestedLimit());
        operationLog.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(operationLog);

        // TODO: 后续接入审批工作流，当前为直接调整额度

        // 直接调整额度（后续可扩展审批流程）
        enterprise.setCreditLimit(dto.getRequestedLimit());
        enterpriseMapper.updateById(enterprise);
    }
}
