package com.xindai.xindai.modules.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.admin.dto.RiskAssessmentQueryDTO;
import com.xindai.xindai.modules.admin.dto.RiskOverrideDTO;
import com.xindai.xindai.modules.admin.vo.AdminRiskAssessmentVO;
import com.xindai.xindai.modules.admin.vo.AdminRiskAssessmentDetailVO;
import com.xindai.xindai.modules.admin.vo.RiskRuleVO;

/**
 * 管理端风险评估服务接口
 */
public interface AdminRiskService {

    /**
     * 获取风险评估分页列表
     */
    Page<AdminRiskAssessmentVO> getAssessmentList(RiskAssessmentQueryDTO queryDTO);

    /**
     * 获取风险评估详情
     */
    AdminRiskAssessmentDetailVO getAssessmentDetail(Long id);

    /**
     * 覆盖风险决策
     */
    void overrideDecision(Long id, RiskOverrideDTO overrideDTO, Long adminId);

    /**
     * 获取当前风险规则配置
     */
    RiskRuleVO getRiskRules();

    /**
     * 更新风险规则配置 (当前只读,保留接口)
     */
    void updateRiskRules(RiskRuleVO ruleVO);
}
