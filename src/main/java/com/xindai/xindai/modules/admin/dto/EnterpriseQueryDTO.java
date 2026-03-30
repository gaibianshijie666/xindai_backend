package com.xindai.xindai.modules.admin.dto;

import com.xindai.xindai.common.dto.BasePageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企业查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "企业查询参数")
public class EnterpriseQueryDTO extends BasePageDTO {

    @Schema(description = "企业状态: 0=待审核, 1=正常, 2=已暂停, 3=已禁用")
    private Integer status;

    @Schema(description = "搜索关键词(企业名称/企业编号/法人/联系人)")
    private String keyword;
}
