package com.xindai.xindai.modules.enterprise.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "企业信息")
@Data
public class EnterpriseInfoVO {

    private String name;
    private String enterpriseNo;

    @JsonIgnore
    private String apiKey;

    /**
     * 获取脱敏后的API Key
     * 显示格式: 前4位 + **** + 后4位
     */
    @Schema(description = "API密钥（脱敏）")
    public String getMaskedApiKey() {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
