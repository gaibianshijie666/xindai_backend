package com.xindai.xindai.modules.admin.dto;

import com.xindai.xindai.common.dto.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 放款记录查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "放款记录查询参数")
public class DisbursementQueryDTO extends BasePageDTO {

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "放款状态: 0=待放款, 1=放款中, 2=已放款, 3=放款失败")
    private Integer status;

    @Schema(description = "申请ID")
    private Long applicationId;
}
