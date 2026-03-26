package com.xindai.xindai.modules.collection.dto;

import com.xindai.xindai.common.dto.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 催收任务查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "催收任务查询条件")
public class CollectionTaskQueryDTO extends BasePageDTO {

    @Schema(description = "任务状态: 0=待分配,1=已分配,2=处理中,3=已完成,4=已关闭")
    private Integer status;

    @Schema(description = "优先级: 1=低,2=中,3=高")
    private Integer priority;

    @Schema(description = "催收员ID")
    private Long collectorId;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "借款人手机号")
    private String userPhone;
}
