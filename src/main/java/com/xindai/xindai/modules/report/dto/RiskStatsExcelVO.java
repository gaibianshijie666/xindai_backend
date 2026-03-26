package com.xindai.xindai.modules.report.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 风控统计报表Excel导出VO
 */
@Data
@ColumnWidth(18)
public class RiskStatsExcelVO {

    @ExcelProperty("统计项目")
    @ColumnWidth(25)
    private String itemName;

    @ExcelProperty("数值")
    private String value;

    @ExcelProperty("说明")
    @ColumnWidth(30)
    private String description;
}
