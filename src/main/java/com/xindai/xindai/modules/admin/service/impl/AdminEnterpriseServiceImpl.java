package com.xindai.xindai.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.admin.dto.CreditLimitAdjustDTO;
import com.xindai.xindai.modules.admin.dto.EnterpriseQueryDTO;
import com.xindai.xindai.modules.admin.dto.EnterpriseStatusDTO;
import com.xindai.xindai.modules.admin.service.AdminEnterpriseService;
import com.xindai.xindai.modules.admin.vo.AdminEnterpriseDetailVO;
import com.xindai.xindai.modules.admin.vo.AdminEnterpriseVO;
import com.xindai.xindai.modules.enterprise.entity.Enterprise;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseCustomer;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseUser;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseCustomerMapper;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseMapper;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseUserMapper;
import com.xindai.xindai.modules.loan.entity.LoanApplication;
import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.mapper.LoanApplicationMapper;
import com.xindai.xindai.modules.loan.mapper.LoanContractMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端企业管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEnterpriseServiceImpl implements AdminEnterpriseService {

    private final EnterpriseMapper enterpriseMapper;
    private final EnterpriseUserMapper enterpriseUserMapper;
    private final EnterpriseCustomerMapper enterpriseCustomerMapper;
    private final LoanApplicationMapper loanApplicationMapper;
    private final LoanContractMapper loanContractMapper;

    @Override
    public Page<AdminEnterpriseVO> getEnterpriseList(EnterpriseQueryDTO queryDTO) {
        Page<Enterprise> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        LambdaQueryWrapper<Enterprise> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Enterprise::getStatus, queryDTO.getStatus());
        }
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w
                    .like(Enterprise::getName, queryDTO.getKeyword())
                    .or()
                    .like(Enterprise::getEnterpriseNo, queryDTO.getKeyword())
                    .or()
                    .like(Enterprise::getLegalPerson, queryDTO.getKeyword())
                    .or()
                    .like(Enterprise::getContactPhone, queryDTO.getKeyword())
            );
        }
        wrapper.orderByDesc(Enterprise::getCreatedAt);

        Page<Enterprise> enterprisePage = enterpriseMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<AdminEnterpriseVO> voPage = new Page<>(enterprisePage.getCurrent(), enterprisePage.getSize(), enterprisePage.getTotal());
        List<AdminEnterpriseVO> voList = enterprisePage.getRecords().stream()
                .map(this::convertToEnterpriseVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public AdminEnterpriseDetailVO getEnterpriseDetail(Long id) {
        Enterprise enterprise = enterpriseMapper.selectById(id);
        if (enterprise == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }

        AdminEnterpriseDetailVO vo = new AdminEnterpriseDetailVO();
        vo.setId(enterprise.getId());
        vo.setEnterpriseNo(enterprise.getEnterpriseNo());
        vo.setName(enterprise.getName());
        vo.setLegalPerson(enterprise.getLegalPerson());
        vo.setContactPhone(enterprise.getContactPhone());
        vo.setEnterpriseType(enterprise.getEnterpriseType());
        vo.setStatus(enterprise.getStatus());
        vo.setCreditLimit(enterprise.getCreditLimit());
        vo.setUsedLimit(enterprise.getUsedLimit());
        vo.setAvailableLimit(enterprise.getCreditLimit().subtract(enterprise.getUsedLimit()));
        vo.setExpireAt(enterprise.getExpireAt());
        vo.setCreatedAt(enterprise.getCreatedAt());
        vo.setUpdatedAt(enterprise.getUpdatedAt());
        vo.setUnifiedSocialCreditCode(enterprise.getUnifiedSocialCreditCode());

        // 脱敏API Key
        if (enterprise.getApiKey() != null && enterprise.getApiKey().length() >= 8) {
            vo.setMaskedApiKey(enterprise.getApiKey().substring(0, 4) + "****" + enterprise.getApiKey().substring(enterprise.getApiKey().length() - 4));
        } else {
            vo.setMaskedApiKey("****");
        }

        // 统计企业用户数量
        Long userCount = enterpriseUserMapper.selectCount(
                new LambdaQueryWrapper<EnterpriseUser>().eq(EnterpriseUser::getEnterpriseId, id)
        );
        vo.setUserCount(userCount.intValue());

        // 统计客户数量
        Long customerCount = enterpriseCustomerMapper.selectCount(
                new LambdaQueryWrapper<EnterpriseCustomer>().eq(EnterpriseCustomer::getEnterpriseId, id)
        );
        vo.setCustomerCount(customerCount.intValue());

        // 统计贷款数据
        List<LoanApplication> loanApplications = loanApplicationMapper.selectList(
                new LambdaQueryWrapper<LoanApplication>().eq(LoanApplication::getEnterpriseId, id)
        );
        vo.setLoanCount(loanApplications.size());

        BigDecimal totalLoanAmount = BigDecimal.ZERO;
        int overdueLoanCount = 0;
        BigDecimal overdueAmount = BigDecimal.ZERO;

        for (LoanApplication application : loanApplications) {
            totalLoanAmount = totalLoanAmount.add(application.getAmount());

            // 查询对应的合同
            LoanContract contract = loanContractMapper.selectOne(
                    new LambdaQueryWrapper<LoanContract>().eq(LoanContract::getApplicationId, application.getId())
            );
            if (contract != null) {
                // 假设状态3为逾期状态
                if (contract.getStatus() == 3) {
                    overdueLoanCount++;
                    overdueAmount = overdueAmount.add(contract.getPrincipal());
                }
            }
        }

        vo.setTotalLoanAmount(totalLoanAmount);
        vo.setOverdueLoanCount(overdueLoanCount);
        vo.setOverdueAmount(overdueAmount);

        return vo;
    }

    @Override
    public void updateEnterpriseStatus(Long id, EnterpriseStatusDTO statusDTO) {
        Enterprise enterprise = enterpriseMapper.selectById(id);
        if (enterprise == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }

        LambdaUpdateWrapper<Enterprise> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Enterprise::getId, id)
                .set(Enterprise::getStatus, statusDTO.getStatus())
                .set(Enterprise::getUpdatedAt, java.time.LocalDateTime.now());

        enterpriseMapper.update(null, wrapper);
        log.info("更新企业状态: enterpriseId={}, status={}", id, statusDTO.getStatus());
    }

    @Override
    public void adjustCreditLimit(Long id, CreditLimitAdjustDTO adjustDTO) {
        Enterprise enterprise = enterpriseMapper.selectById(id);
        if (enterprise == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }

        // 检查新额度是否小于已用额度
        if (adjustDTO.getCreditLimit().compareTo(enterprise.getUsedLimit()) < 0) {
            throw new BusinessException(ErrorCode.ENTERPRISE_CREDIT_LIMIT_EXCEEDED, "新授信额度不能小于已用额度");
        }

        LambdaUpdateWrapper<Enterprise> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Enterprise::getId, id)
                .set(Enterprise::getCreditLimit, adjustDTO.getCreditLimit())
                .set(Enterprise::getUpdatedAt, java.time.LocalDateTime.now());

        enterpriseMapper.update(null, wrapper);
        log.info("调整企业授信额度: enterpriseId={}, oldLimit={}, newLimit={}, reason={}",
                id, enterprise.getCreditLimit(), adjustDTO.getCreditLimit(), adjustDTO.getReason());
    }

    private AdminEnterpriseVO convertToEnterpriseVO(Enterprise enterprise) {
        AdminEnterpriseVO vo = new AdminEnterpriseVO();
        vo.setId(enterprise.getId());
        vo.setEnterpriseNo(enterprise.getEnterpriseNo());
        vo.setName(enterprise.getName());
        vo.setLegalPerson(enterprise.getLegalPerson());
        vo.setContactPhone(enterprise.getContactPhone());
        vo.setEnterpriseType(enterprise.getEnterpriseType());
        vo.setStatus(enterprise.getStatus());
        vo.setCreditLimit(enterprise.getCreditLimit());
        vo.setUsedLimit(enterprise.getUsedLimit());
        vo.setAvailableLimit(enterprise.getCreditLimit().subtract(enterprise.getUsedLimit()));
        vo.setExpireAt(enterprise.getExpireAt());
        vo.setCreatedAt(enterprise.getCreatedAt());
        return vo;
    }
}
