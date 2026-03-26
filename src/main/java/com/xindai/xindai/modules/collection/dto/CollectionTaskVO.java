package com.xindai.xindai.modules.collection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 催收任务VO
 */
@Data
@Schema(description = "催收任务信息")
public class CollectionTaskVO {

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "借款人用户ID")
    private Long userId;

    @Schema(description = "借款人姓名")
    private String userName;

    @Schema(description = "借款人手机号")
    private String userPhone;

    @Schema(description = "逾期金额")
    private BigDecimal overdueAmount;

    @Schema(description = "逾期天数")
    private Integer overdueDays;

    @Schema(description = "催收员ID")
    private Long collectorId;

    @Schema(description = "催收员姓名")
    private String collectorName;

    @Schema(description = "任务状态: 0=待分配,1=已分配,2=处理中,3=已完成,4=已关闭")
    private Integer status;

    @Schema(description = "任务状态描述")
    private String statusDesc;

    @Schema(description = "优先级: 1=低,2=中,3=高")
    private Integer priority;

    @Schema(description = "催收截止日期")
    private LocalDateTime deadline;

    @Schema(description = "催收记录数")
    private Integer recordCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
