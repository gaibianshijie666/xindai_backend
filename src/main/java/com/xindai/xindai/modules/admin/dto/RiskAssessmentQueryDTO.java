package com.xindai.xindai.modules.admin.dto;

import com.xindai.xindai.common.dto.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 风险评估查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "风险评估查询参数")
public class RiskAssessmentQueryDTO extends BasePageDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "风险等级: 1=低, 2=中, 3=高")
    private Integer riskLevel;

    @Schema(description = "决策: APPROVE, REJECT, MANUAL_REVIEW")
    private String decision;

    @Schema(description = "是否已被覆盖")
    private Boolean overridden;

    @Schema(description = "开始日期")
    private LocalDateTime startDate;

    @Schema(description = "结束日期")
    private LocalDateTime endDate;
}
