package com.xindai.xindai.modules.enterprise.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseRiskQueryDTO;
import com.xindai.xindai.modules.enterprise.dto.RiskAssessResultVO;

import java.util.List;

/**
 * 企业风控评估服务接口
 */
public interface EnterpriseRiskService {

    /**
     * 评估单个客户风险
     *
     * @param enterpriseId 企业ID
     * @param customerId   客户ID
     * @return 风险评估结果
     */
    RiskAssessResultVO assessCustomer(Long enterpriseId, Long customerId);

    /**
     * 批量评估客户风险
     *
     * @param enterpriseId 企业ID
     * @param customerIds  客户ID列表
     * @return 风险评估结果列表
     */
    List<RiskAssessResultVO> batchAssess(Long enterpriseId, List<Long> customerIds);

    /**
     * 获取评估历史记录
     *
     * @param enterpriseId 企业ID
     * @param customerId   客户ID
     * @param queryDTO     分页查询参数
     * @return 分页结果
     */
    Page<RiskAssessResultVO> getAssessHistory(Long enterpriseId, Long customerId, EnterpriseRiskQueryDTO queryDTO);
}
