package com.xindai.xindai.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 手动还款DTO（线下还款记录）
 */
@Data
@Schema(description = "手动还款参数")
public class ManualRepayDTO {

    @Schema(description = "实还金额", required = true)
    @NotNull(message = "实还金额不能为空")
    private String actualAmount;

    @Schema(description = "还款方式: BANK_TRANSFER/ALIPAY/WECHAT/CASH")
    private String paymentMethod;

    @Schema(description = "备注说明")
    private String remark;
}
