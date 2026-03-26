package com.xindai.xindai.modules.report.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 借款台账Excel导出VO
 */
@Data
@ColumnWidth(18)
public class LoanLedgerExcelVO {

    @ExcelProperty("合同编号")
    @ColumnWidth(25)
    private String contractNo;

    @ExcelProperty("借款人")
    private String userName;

    @ExcelProperty("身份证号")
    @ColumnWidth(25)
    private String idCard;

    @ExcelProperty("借款金额")
    private BigDecimal principal;

    @ExcelProperty("利率(%)")
    private BigDecimal interestRate;

    @ExcelProperty("期限(月)")
    private Integer term;

    @ExcelProperty("还款总额")
    private BigDecimal totalRepayment;

    @ExcelProperty("放款日期")
    @ColumnWidth(15)
    private String disbursedAt;

    @ExcelProperty("到期日期")
    @ColumnWidth(15)
    private String dueDate;

    @ExcelProperty("合同状态")
    private String statusDesc;
}
