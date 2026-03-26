package com.xindai.xindai.modules.risk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.risk.dto.RiskAssessmentVO;
import com.xindai.xindai.modules.risk.entity.RiskAssessment;

import java.time.LocalDateTime;
import java.util.List;

public interface RiskAssessmentService {
    RiskAssessment assess(Long userId, Long applicationId, Integer assessmentType);
    Page<RiskAssessment> getAssessmentHistory(Long userId, int page, int size);
    RiskAssessment getByAssessmentNo(String assessmentNo);

    /**
     * 将风险评估实体转换为VO
     */
    RiskAssessmentVO toVO(RiskAssessment assessment);

    /**
     * 批量将风险评估实体转换为VO
     */
    List<RiskAssessmentVO> toVOList(List<RiskAssessment> assessments);

    /**
     * 获取指定时间范围内的风险评估列表
     */
    List<RiskAssessment> getAssessmentsSince(LocalDateTime since);
}
