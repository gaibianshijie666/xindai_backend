package com.xindai.xindai.modules.admin.dto;

import com.xindai.xindai.common.dto.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 借款合同查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "借款合同查询参数")
public class ContractQueryDTO extends BasePageDTO {

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "合同状态: 0=待放款, 1=还款中, 2=已结清, 3=逾期")
    private Integer status;

    @Schema(description = "申请ID")
    private Long applicationId;
}
