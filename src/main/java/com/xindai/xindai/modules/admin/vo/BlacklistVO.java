package com.xindai.xindai.modules.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 黑名单VO
 */
@Data
@Schema(description = "黑名单信息")
public class BlacklistVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "类型: 1=手机号, 2=身份证, 3=设备ID")
    private Integer type;

    @Schema(description = "值")
    private String value;

    @Schema(description = "原因")
    private String reason;

    @Schema(description = "过期时间")
    private LocalDateTime expireAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
