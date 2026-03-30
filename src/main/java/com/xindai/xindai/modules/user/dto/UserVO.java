package com.xindai.xindai.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xindai.xindai.common.annotation.Desensitize;
import com.xindai.xindai.modules.user.entity.User;
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

    public static UserVO from(User user, String token) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setRealName(user.getRealName());
        vo.setIdCard(user.getIdCard());
        vo.setStatus(user.getStatus());
        vo.setToken(token);
        return vo;
    }
}
