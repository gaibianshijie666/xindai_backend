package com.xindai.xindai.modules.admin.dto;

import com.xindai.xindai.common.dto.BasePageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Admin notification query DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminNotificationQueryDTO extends BasePageDTO {
    private String type;
    private Integer isRead;
}
