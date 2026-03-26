package com.xindai.xindai.modules.user.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户画像 VO
 * 返回给前端的用户画像信息
 */
@Data
public class UserProfileVO {

    private Long id;

    private Long userId;

    /**
     * 综合信用分 (300-850)
     */
    private Integer creditScore;

    /**
     * 风险等级: 1-低 2-中 3-高
     */
    private Integer riskLevel;

    /**
     * 信用等级 (A-G)
     */
    private String creditGrade;

    /**
     * 年收入（元）
     */
    private BigDecimal annualIncome;

    /**
     * 债务收入比 (0-1)
     */
    private BigDecimal dti;

    /**
     * 就业年限（年）
     */
    private Integer employmentYears;

    /**
     * 风险评分 (0-100)
     */
    private Double riskScore;

    /**
     * 工作单位
     */
    private String company;

    /**
     * 职位
     */
    private String position;

    /**
     * 画像更新时间
     */
    private LocalDateTime profileUpdatedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
