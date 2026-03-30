package com.xindai.xindai.modules.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端企业列表VO
 */
@Data
@Schema(description = "管理端企业信息")
public class AdminEnterpriseVO {

    @Schema(description = "企业ID")
    private Long id;

    @Schema(description = "企业编号")
    private String enterpriseNo;

    @Schema(description = "企业名称")
    private String name;

    @Schema(description = "法人")
    private String legalPerson;

    @Schema(description = "联系人电话")
    private String contactPhone;

    @Schema(description = "企业类型: 1=大型企业, 2=中型企业, 3=小型企业")
    private Integer enterpriseType;

    @Schema(description = "企业状态: 0=待审核, 1=正常, 2=已暂停, 3=已禁用")
    private Integer status;

    @Schema(description = "授信额度")
    private BigDecimal creditLimit;

    @Schema(description = "已用额度")
    private BigDecimal usedLimit;

    @Schema(description = "可用额度")
    private BigDecimal availableLimit;

    @Schema(description = "到期时间")
    private LocalDateTime expireAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
