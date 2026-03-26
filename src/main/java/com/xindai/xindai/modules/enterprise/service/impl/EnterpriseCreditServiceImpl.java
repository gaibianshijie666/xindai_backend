package com.xindai.xindai.modules.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.enterprise.dto.CreditApplyDTO;
import com.xindai.xindai.modules.enterprise.dto.CreditInfoVO;
import com.xindai.xindai.modules.enterprise.entity.Enterprise;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseMapper;
import com.xindai.xindai.modules.enterprise.service.EnterpriseCreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class EnterpriseCreditServiceImpl implements EnterpriseCreditService {

    private final EnterpriseMapper enterpriseMapper;

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

        if (dto.getRequestedLimit().compareTo(enterprise.getUsedLimit()) < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "申请额度不能低于已使用额度");
        }

        // 直接调整额度（后续可扩展审批流程）
        enterprise.setCreditLimit(dto.getRequestedLimit());
        enterpriseMapper.updateById(enterprise);
    }
}
