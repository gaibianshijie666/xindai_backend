package com.xindai.xindai.modules.enterprise.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * 每日客户统计DTO
 */
@Data
public class DailyCustomerStats {
    /**
     * 日期
     */
    private LocalDate date;

    /**
     * 新增客户数量
     */
    private Integer newCustomerCount;
}
