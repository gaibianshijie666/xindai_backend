package com.xindai.xindai.modules.enterprise.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

/**
 * 批量更新状态DTO
 */
@Data
public class BatchUpdateStatusDTO {
    /**
     * 客户ID列表
     */
    @NotEmpty(message = "ID列表不能为空")
    @Size(max = 100, message = "单次最多操作100条")
    private List<Long> ids;

    /**
     * 状态 (0-正常 1-禁用)
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
