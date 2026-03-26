package com.xindai.xindai.modules.admin.service;

import com.xindai.xindai.modules.admin.vo.DashboardOverviewVO;
import com.xindai.xindai.modules.admin.vo.RiskStatsVO;

/**
 * 管理端仪表盘服务接口
 */
public interface AdminDashboardService {

    /**
     * 获取仪表盘概览数据
     */
    DashboardOverviewVO getOverview();

    /**
     * 获取风控统计数据
     */
    RiskStatsVO getRiskStats(String range);
}
