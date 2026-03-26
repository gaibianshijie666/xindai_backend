package com.xindai.xindai.modules.agent.tools;

import com.xindai.xindai.modules.agent.annotation.ToolAllowed;
import com.xindai.xindai.modules.risk.entity.RiskAssessment;
import com.xindai.xindai.modules.risk.service.RiskAssessmentService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskTools {

    private final RiskAssessmentService riskAssessmentService;

    @ToolAllowed(portals = {"user", "admin", "enterprise"})
    @Tool("查询用户的风险评估历史记录，包括风险评分、风险等级和审批决策")
    public String getRiskHistory() {
        try {
            List<RiskAssessment> history = riskAssessmentService.getAssessmentHistory(ToolContext.getUserId());
            if (history.isEmpty()) return "暂无风险评估记录";
            return history.stream().map(r -> String.format(
                    "评估编号: %s, 风险评分: %s, 风险等级: %s, 决策: %s, 时间: %s",
                    r.getAssessmentNo(),
                    r.getRiskScore() != null ? r.getRiskScore().toString() : "N/A",
                    mapRiskLevel(r.getRiskLevel()),
                    r.getDecision() != null ? r.getDecision() : "N/A",
                    r.getCreatedAt()
            )).collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("Tool getRiskHistory failed", e);
            return "查询风险评估历史失败: " + e.getMessage();
        }
    }

    private String mapRiskLevel(Integer level) {
        if (level == null) return "未知";
        return switch (level) {
            case 1 -> "低风险";
            case 2 -> "中风险";
            case 3 -> "高风险";
            default -> "未知";
        };
    }
}
