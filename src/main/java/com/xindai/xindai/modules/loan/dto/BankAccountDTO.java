package com.xindai.xindai.modules.loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "银行卡绑定请求")
public class BankAccountDTO {

    @NotBlank(message = "银行名称不能为空")
    @Schema(description = "银行名称", example = "中国工商银行")
    private String bankName;

    @NotBlank(message = "银行卡号不能为空")
    @Schema(description = "银行卡号", example = "6222021234567890123")
    private String accountNo;

    @NotBlank(message = "账户名不能为空")
    @Schema(description = "账户名（需与实名一致）", example = "张三")
    private String accountName;

    @Schema(description = "是否设为默认银行卡", example = "true")
    private Boolean isDefault;
}
