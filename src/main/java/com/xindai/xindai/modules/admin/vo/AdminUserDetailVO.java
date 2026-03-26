package com.xindai.xindai.modules.admin.vo;

import com.xindai.xindai.common.annotation.Desensitize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端用户详情VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端用户详情")
public class AdminUserDetailVO extends AdminUserVO {

    @Schema(description = "身份证号(脱敏)")
    @Desensitize(Desensitize.DesensitizeType.ID_CARD)
    private String idCard;

    @Schema(description = "信用评分")
    private Integer creditScore;

    @Schema(description = "风险等级: 0=低, 1=中, 2=高")
    private Integer riskLevel;

    @Schema(description = "信用等级")
    private String creditGrade;
}
