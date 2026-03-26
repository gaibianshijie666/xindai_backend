package com.xindai.xindai.modules.admin.dto;

import com.xindai.xindai.common.dto.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 黑名单查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "黑名单查询参数")
public class BlacklistQueryDTO extends BasePageDTO {

    @Schema(description = "类型: 1=手机号, 2=身份证, 3=设备ID")
    private Integer type;
}
