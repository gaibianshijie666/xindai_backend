package com.xindai.xindai.modules.report.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Credit usage Excel export VO
 */
@Data
@ColumnWidth(20)
public class CreditUsageExcelVO {

    @ExcelProperty("总额度")
    private BigDecimal totalLimit;

    @ExcelProperty("已用额度")
    private BigDecimal usedLimit;

    @ExcelProperty("可用额度")
    private BigDecimal availableLimit;

    @ExcelProperty("使用率(%)")
    private BigDecimal utilizationRate;

    @ExcelProperty("活跃用户数")
    private Long activeUsers;
}
