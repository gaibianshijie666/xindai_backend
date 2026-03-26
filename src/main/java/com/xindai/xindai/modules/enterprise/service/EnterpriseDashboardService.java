package com.xindai.xindai.modules.enterprise.service;

import com.xindai.xindai.modules.enterprise.dto.DashboardOverviewVO;
import com.xindai.xindai.modules.enterprise.dto.DashboardTrendVO;

import java.time.LocalDate;

/**
 * 企业数据看板服务接口
 */
public interface EnterpriseDashboardService {

    /**
     * 获取数据看板概览
     *
     * @param enterpriseId 企业ID
     * @return 概览数据
     */
    DashboardOverviewVO getOverview(Long enterpriseId);

    /**
     * 获取数据趋势
     *
     * @param enterpriseId 企业ID
     * @param startDate    开始日期
     * @param endDate      结束日期
     * @return 趋势数据
     */
    DashboardTrendVO getTrend(Long enterpriseId, LocalDate startDate, LocalDate endDate);
}
