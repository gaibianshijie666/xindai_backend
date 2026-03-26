package com.xindai.xindai.modules.collection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 催收记录VO
 */
@Data
@Schema(description = "催收记录信息")
public class CollectionRecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "催收员ID")
    private Long collectorId;

    @Schema(description = "催收员姓名")
    private String collectorName;

    @Schema(description = "催收方式: phone/sms/visit/legal")
    private String method;

    @Schema(description = "催收方式描述")
    private String methodDesc;

    @Schema(description = "催收内容")
    private String content;

    @Schema(description = "催收结果: promise_pay/refused/unreachable/other")
    private String result;

    @Schema(description = "催收结果描述")
    private String resultDesc;

    @Schema(description = "下次跟进日期")
    private LocalDate nextFollowUpDate;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
