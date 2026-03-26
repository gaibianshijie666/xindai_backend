package com.xindai.xindai.modules.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "risk_assessment", autoResultMap = true)
public class RiskAssessment {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("assessment_no")
    private String assessmentNo;

    @TableField("user_id")
    private Long userId;

    @TableField("application_id")
    private Long applicationId;

    @TableField("assessment_type")
    private Integer assessmentType;  // 1-额度评估 2-借款评估 3-贷后监控

    @TableField("risk_score")
    private BigDecimal riskScore;

    @TableField("risk_level")
    private Integer riskLevel;  // 1-低 2-中 3-高

    @TableField("decision")
    private String decision;  // APPROVE/REJECT/MANUAL_REVIEW

    @TableField("model_version")
    private String modelVersion;

    @TableField(value = "feature_snapshot", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> featureSnapshot;

    @TableField(value = "factors", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> factors;

    @TableField("processing_time_ms")
    private Integer processingTimeMs;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
