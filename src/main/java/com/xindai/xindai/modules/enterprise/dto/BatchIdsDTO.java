package com.xindai.xindai.modules.enterprise.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

/**
 * 批量ID请求DTO
 */
@Data
public class BatchIdsDTO {
    /**
     * ID列表
     */
    @NotEmpty(message = "ID列表不能为空")
    @Size(max = 100, message = "单次最多操作100条")
    private List<Long> ids;
}
