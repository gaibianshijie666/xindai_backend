package com.xindai.xindai.modules.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户画像更新 DTO
 * 用于用户主动填写个人画像信息
 */
@Data
public class UserProfileUpdateDTO {

    /**
     * 年收入（元）
     */
    @DecimalMin(value = "0", message = "年收入不能为负数")
    @DecimalMax(value = "100000000", message = "年收入不能超过1亿")
    private BigDecimal annualIncome;

    /**
     * 就业年限（年）
     */
    @Min(value = 0, message = "就业年限不能为负数")
    @Max(value = 50, message = "就业年限不能超过50年")
    private Integer employmentYears;

    /**
     * 月收入（元）- 可选，用于自动计算年收入
     */
    @DecimalMin(value = "0", message = "月收入不能为负数")
    private BigDecimal monthlyIncome;

    /**
     * 月负债（元）- 用于计算 DTI（债务收入比）
     */
    @DecimalMin(value = "0", message = "月负债不能为负数")
    private BigDecimal monthlyDebt;

    /**
     * 工作单位
     */
    private String company;

    /**
     * 职位
     */
    private String position;
}
