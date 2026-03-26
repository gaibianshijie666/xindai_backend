package com.xindai.xindai.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xindai.xindai.common.annotation.Desensitize;
import lombok.Data;

@Data
public class UserVO {
    private Long id;
    @Desensitize(Desensitize.DesensitizeType.PHONE)
    private String phone;
    @Desensitize(Desensitize.DesensitizeType.NAME)
    private String realName;
    @Desensitize(Desensitize.DesensitizeType.ID_CARD)
    private String idCard;
    private Integer status;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String token;
}
