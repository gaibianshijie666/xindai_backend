package com.xindai.xindai.modules.collection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建催收记录DTO
 */
@Data
@Schema(description = "创建催收记录请求")
public class CreateCollectionRecordDTO {

    @NotNull(message = "任务ID不能为空")
    @Schema(description = "催收任务ID", required = true)
    private Long taskId;

    @NotBlank(message = "催收方式不能为空")
    @Schema(description = "催收方式: phone/sms/visit/legal", required = true)
    private String method;

    @Schema(description = "催收内容")
    private String content;

    @Schema(description = "催收结果: promise_pay/refused/unreachable/other")
    private String result;

    @Schema(description = "下次跟进日期")
    private LocalDate nextFollowUpDate;
}
