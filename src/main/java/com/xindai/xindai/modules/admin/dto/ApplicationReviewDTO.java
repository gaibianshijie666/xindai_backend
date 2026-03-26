package com.xindai.xindai.modules.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 借款审核DTO
 */
@Data
@Schema(description = "借款审核参数")
public class ApplicationReviewDTO {

    @Schema(description = "是否通过", required = true)
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    @Schema(description = "审核意见/拒绝原因")
    private String reason;

    /**
     * 审批动作：APPROVED-通过, REJECTED-拒绝, RETURNED-退回补充材料, CONDITIONAL-条件性通过
     * 如果不指定，则根据approved自动推断(APPROVED/REJECTED)
     */
    @Schema(description = "审批动作: APPROVED/REJECTED/RETURNED/CONDITIONAL")
    private String action;
}
