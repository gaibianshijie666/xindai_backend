package com.xindai.xindai.modules.report.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * User growth statistics view object
 */
@Data
public class UserGrowthVO {
    private LocalDate date;
    private Long newUsers;
    private Long totalUsers;
}
