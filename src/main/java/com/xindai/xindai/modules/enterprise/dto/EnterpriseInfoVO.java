package com.xindai.xindai.modules.enterprise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "企业信息")
@Data
public class EnterpriseInfoVO {

    private String name;
    private String enterpriseNo;
    private String apiKey;
}
