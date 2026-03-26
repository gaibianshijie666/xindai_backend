package com.xindai.xindai.modules.report.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 催收绩效报表Excel导出VO
 */
@Data
@ColumnWidth(18)
public class CollectionPerformanceExcelVO {

    @ExcelProperty("催收员ID")
    private Long collectorId;

    @ExcelProperty("催收员姓名")
    private String collectorName;

    @ExcelProperty("分配任务数")
    private Integer totalTasks;

    @ExcelProperty("已完成任务数")
    private Integer completedTasks;

    @ExcelProperty("进行中任务数")
    private Integer inProgressTasks;

    @ExcelProperty("关闭任务数")
    private Integer closedTasks;

    @ExcelProperty("催收成功率(%)")
    private BigDecimal successRate;

    @ExcelProperty("催收总金额")
    private BigDecimal totalOverdueAmount;

    @ExcelProperty("已收回金额")
    private BigDecimal collectedAmount;
}
