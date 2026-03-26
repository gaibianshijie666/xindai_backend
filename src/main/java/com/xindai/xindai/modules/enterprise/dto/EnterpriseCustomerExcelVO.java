package com.xindai.xindai.modules.enterprise.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 企业客户Excel导出VO
 */
@Data
@ColumnWidth(20)
public class EnterpriseCustomerExcelVO {
    @ExcelProperty("客户编号")
    private String customerNo;

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("身份证号")
    @ColumnWidth(25)
    private String idCard;

    @ExcelProperty("手机号")
    @ColumnWidth(15)
    private String phone;

    @ExcelProperty("信用评分")
    private Integer creditScore;

    @ExcelProperty("风险等级")
    private String riskLevelText;

    @ExcelProperty("借款次数")
    private Integer totalLoanCount;

    @ExcelProperty("累计借款金额")
    @ColumnWidth(15)
    private BigDecimal totalLoanAmount;

    @ExcelProperty("状态")
    private String statusText;

    @ExcelProperty("创建时间")
    @ColumnWidth(25)
    private String createdAtText;

    /**
     * 从VO转换为Excel VO
     */
    public static EnterpriseCustomerExcelVO fromVO(EnterpriseCustomerVO vo) {
        EnterpriseCustomerExcelVO excelVO = new EnterpriseCustomerExcelVO();
        excelVO.setCustomerNo(vo.getCustomerNo());
        excelVO.setRealName(vo.getRealName());
        excelVO.setIdCard(vo.getIdCard());
        excelVO.setPhone(vo.getPhone());
        excelVO.setCreditScore(vo.getCreditScore());
        excelVO.setRiskLevelText(getRiskLevelText(vo.getRiskLevel()));
        excelVO.setTotalLoanCount(vo.getTotalLoanCount());
        excelVO.setTotalLoanAmount(vo.getTotalLoanAmount());
        excelVO.setStatusText(getStatusText(vo.getStatus()));
        if (vo.getCreatedAt() != null) {
            excelVO.setCreatedAtText(vo.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return excelVO;
    }

    private static String getRiskLevelText(Integer riskLevel) {
        if (riskLevel == null) return "未知";
        return switch (riskLevel) {
            case 1 -> "低风险";
            case 2 -> "中风险";
            case 3 -> "高风险";
            default -> "未知";
        };
    }

    private static String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "正常";
            case 1 -> "禁用";
            default -> "未知";
        };
    }
}
