package com.xindai.xindai.modules.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@ToString(exclude = {"annualIncome", "dti", "behaviorFeatures", "socialFeatures", "creditFeatures"})
@TableName(value = "user_profile", autoResultMap = true)
public class UserProfile {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("credit_score")
    private Integer creditScore;

    @TableField("risk_level")
    private Integer riskLevel;

    /**
     * 信用等级 (A-G)
     */
    @TableField("credit_grade")
    private String creditGrade;

    /**
     * 年收入
     */
    @TableField("annual_income")
    private BigDecimal annualIncome;

    /**
     * 债务收入比 (Debt-to-Income Ratio)
     */
    @TableField("dti")
    private BigDecimal dti;

    /**
     * 就业年限
     */
    @TableField("employment_years")
    private Integer employmentYears;

    /**
     * 风险评分 (0-100)
     */
    @TableField("risk_score")
    private Double riskScore;

    /**
     * 工作单位
     */
    @TableField("company")
    private String company;

    /**
     * 职位
     */
    @TableField("position")
    private String position;

    @TableField(value = "behavior_features", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> behaviorFeatures;

    @TableField(value = "social_features", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> socialFeatures;

    @TableField(value = "credit_features", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> creditFeatures;

    /**
     * 实名认证状态: 0=未认证,1=认证中,2=已认证,3=认证失败
     */
    @TableField("identity_status")
    private Integer identityStatus;

    /**
     * 实名认证时间
     */
    @TableField("identity_verified_at")
    private LocalDateTime identityVerifiedAt;

    @TableField("profile_updated_at")
    private LocalDateTime profileUpdatedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
