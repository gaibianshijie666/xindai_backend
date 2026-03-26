package com.xindai.xindai.modules.enterprise.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

/**
 * 批量审核DTO
 */
@Data
public class BatchReviewDTO {
    /**
     * 借款申请ID列表
     */
    @NotEmpty(message = "ID列表不能为空")
    @Size(max = 100, message = "单次最多操作100条")
    private List<Long> ids;

    /**
     * 审核状态 (1-通过 2-拒绝)
     */
    @NotNull(message = "审核状态不能为空")
    private Integer status;

    /**
     * 审核备注
     */
    private String reviewNote;
}
