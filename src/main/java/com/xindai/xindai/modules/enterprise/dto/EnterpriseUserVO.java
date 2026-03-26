package com.xindai.xindai.modules.enterprise.dto;

import lombok.Data;

@Data
public class EnterpriseUserVO {
    private Long id;
    private Long enterpriseId;
    private String enterpriseName;
    private String username;
    private String realName;
    private String phone;
    private Integer role;
    private String token;
}
