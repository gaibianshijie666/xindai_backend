package com.xindai.xindai.modules.risk.service;

import com.xindai.xindai.modules.risk.entity.RiskAssessment;
import java.util.List;

public interface RiskAssessmentService {
    RiskAssessment assess(Long userId, Long applicationId, Integer assessmentType);
    List<RiskAssessment> getAssessmentHistory(Long userId);
    RiskAssessment getByAssessmentNo(String assessmentNo);
}
