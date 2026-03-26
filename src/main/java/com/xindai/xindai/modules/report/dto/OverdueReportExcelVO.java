package com.xindai.xindai.modules.report.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 逾期报表Excel导出VO
 */
@Data
@ColumnWidth(18)
public class OverdueReportExcelVO {

    @ExcelProperty("合同编号")
    @ColumnWidth(25)
    private String contractNo;

    @ExcelProperty("借款人")
    private String userName;

    @ExcelProperty("手机号")
    @ColumnWidth(15)
    private String userPhone;

    @ExcelProperty("逾期期数")
    private Integer period;

    @ExcelProperty("逾期金额")
    private BigDecimal overdueAmount;

    @ExcelProperty("逾期天数")
    private Integer overdueDays;

    @ExcelProperty("罚息金额")
    private BigDecimal penaltyAmount;

    @ExcelProperty("应还日期")
    @ColumnWidth(15)
    private String dueDate;

    @ExcelProperty("逾期等级")
    private String overdueLevel;
}
