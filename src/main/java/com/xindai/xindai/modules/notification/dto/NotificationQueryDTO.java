package com.xindai.xindai.modules.notification.dto;

import com.xindai.xindai.common.dto.BasePageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationQueryDTO extends BasePageDTO {
    private Boolean unreadOnly;
    private String type;
}
