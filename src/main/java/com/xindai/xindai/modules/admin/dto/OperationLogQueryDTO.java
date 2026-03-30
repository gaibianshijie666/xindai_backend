package com.xindai.xindai.modules.admin.dto;

import com.xindai.xindai.common.dto.BasePageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Operation log query DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLogQueryDTO extends BasePageDTO {
    private String module;
    private String operation;
    private String username;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
