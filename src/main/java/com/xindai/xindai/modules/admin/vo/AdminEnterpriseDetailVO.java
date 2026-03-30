package com.xindai.xindai.modules.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端企业详情VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端企业详情")
public class AdminEnterpriseDetailVO extends AdminEnterpriseVO {

    @Schema(description = "统一社会信用代码")
    private String unifiedSocialCreditCode;

    @Schema(description = "API密钥（脱敏）")
    private String maskedApiKey;

    @Schema(description = "企业用户数量")
    private Integer userCount;

    @Schema(description = "客户数量")
    private Integer customerCount;

    @Schema(description = "贷款总数")
    private Integer loanCount;

    @Schema(description = "贷款总金额")
    private BigDecimal totalLoanAmount;

    @Schema(description = "逾期贷款数")
    private Integer overdueLoanCount;

    @Schema(description = "逾期金额")
    private BigDecimal overdueAmount;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
