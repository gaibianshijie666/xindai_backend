package com.xindai.xindai.modules.admin.dto;

import com.xindai.xindai.common.dto.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 借款申请查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "借款申请查询参数")
public class ApplicationQueryDTO extends BasePageDTO {

    @Schema(description = "申请状态: 0=待审核, 1=审核中, 2=已通过, 3=已拒绝")
    private Integer status;
}
