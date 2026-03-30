package com.xindai.xindai.modules.report.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * User growth Excel export VO
 */
@Data
@ColumnWidth(18)
public class UserGrowthExcelVO {

    @ExcelProperty("日期")
    @ColumnWidth(15)
    private String date;

    @ExcelProperty("新增用户数")
    private Long newUsers;

    @ExcelProperty("累计用户数")
    private Long totalUsers;
}
