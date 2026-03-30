package com.xindai.xindai.modules.admin.dto;

import com.xindai.xindai.common.dto.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 还款计划查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "还款计划查询参数")
public class RepaymentQueryDTO extends BasePageDTO {

    @Schema(description = "还款状态: 0=待还款, 1=已还款, 2=已逾期")
    private Integer status;

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "到期日期-开始")
    private LocalDate dueDateStart;

    @Schema(description = "到期日期-结束")
    private LocalDate dueDateEnd;
}
