package com.xindai.xindai.modules.agent.tools;

import com.xindai.xindai.modules.agent.annotation.ToolAllowed;
import com.xindai.xindai.modules.admin.dto.UserQueryDTO;
import com.xindai.xindai.modules.admin.service.AdminDashboardService;
import com.xindai.xindai.modules.admin.service.AdminUserService;
import com.xindai.xindai.modules.admin.vo.AdminUserVO;
import com.xindai.xindai.modules.admin.vo.DashboardOverviewVO;
import com.xindai.xindai.modules.admin.vo.RiskStatsVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminTools {

    private final AdminDashboardService adminDashboardService;
    private final AdminUserService adminUserService;

    @ToolAllowed(portals = {"admin"})
    @Tool("获取管理后台的仪表盘概览数据，包括用户总数、贷款总额、逾期率等")
    public String getDashboardOverview() {
        try {
            DashboardOverviewVO vo = adminDashboardService.getOverview();
            DashboardOverviewVO.RiskDistribution rd = vo.getRiskDistribution();
            return String.format(
                    "总用户数: %d, 总借款数: %d, 总借款金额: %.2f元, 逾期率: %.2f%%, " +
                    "今日申请: %d, 今日通过: %d, 今日拒绝: %d, 通过率: %.2f%%" +
                    "%s",
                    vo.getTotalUsers() != null ? vo.getTotalUsers() : 0,
                    vo.getTotalLoans() != null ? vo.getTotalLoans() : 0,
                    vo.getTotalAmount() != null ? vo.getTotalAmount() : 0,
                    vo.getOverdueRate() != null ? vo.getOverdueRate() : 0,
                    vo.getTodayApplications() != null ? vo.getTodayApplications() : 0,
                    vo.getTodayApproved() != null ? vo.getTodayApproved() : 0,
                    vo.getTodayRejected() != null ? vo.getTodayRejected() : 0,
                    vo.getApprovalRate() != null ? vo.getApprovalRate() : 0,
                    rd != null ? String.format(", 风险分布(低/中/高): %d/%d/%d",
                            rd.getLow() != null ? rd.getLow() : 0,
                            rd.getMedium() != null ? rd.getMedium() : 0,
                            rd.getHigh() != null ? rd.getHigh() : 0) : ""
            );
        } catch (Exception e) {
            log.error("Tool getDashboardOverview failed", e);
            return "查询仪表盘数据失败: " + e.getMessage();
        }
    }

    @ToolAllowed(portals = {"admin"})
    @Tool("获取风控统计数据，参数range可选: 7d(近7天), 30d(近30天), 90d(近90天)")
    public String getRiskStats(String range) {
        try {
            RiskStatsVO vo = adminDashboardService.getRiskStats(range);
            RiskStatsVO.RiskDistribution dist = vo.getDistribution();
            StringBuilder sb = new StringBuilder("统计范围: " + vo.getRange());
            if (dist != null) {
                sb.append(String.format(", 低风险占比: %.1f%%, 中风险占比: %.1f%%, 高风险占比: %.1f%%",
                        dist.getLowPercent() != null ? dist.getLowPercent() : 0,
                        dist.getMediumPercent() != null ? dist.getMediumPercent() : 0,
                        dist.getHighPercent() != null ? dist.getHighPercent() : 0));
            }
            if (vo.getDailyStats() != null && !vo.getDailyStats().isEmpty()) {
                RiskStatsVO.DailyStats latest = vo.getDailyStats().get(vo.getDailyStats().size() - 1);
                sb.append(String.format(", 最新日期(%s): 申请%d, 通过%d, 拒绝%d, 平均风险评分%.1f",
                        latest.getDate(), latest.getApplications(), latest.getApproved(),
                        latest.getRejected(), latest.getAvgRiskScore()));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Tool getRiskStats failed", e);
            return "查询风控统计失败: " + e.getMessage();
        }
    }

    @ToolAllowed(portals = {"admin"})
    @Tool("查询用户列表，可提供关键字搜索姓名或手机号")
    public String getUserList(String keyword) {
        try {
            UserQueryDTO query = new UserQueryDTO();
            query.setKeyword(keyword);
            Page<AdminUserVO> page = adminUserService.getUserList(query);
            if (page.getRecords().isEmpty()) return "未找到匹配的用户";
            return page.getRecords().stream().map(u -> String.format(
                    "用户ID: %d, 姓名: %s, 手机号: %s, 状态: %s",
                    u.getId(),
                    u.getRealName() != null ? u.getRealName() : "未实名",
                    u.getPhone(),
                    u.getStatus() == 1 ? "正常" : "禁用"
            )).collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("Tool getUserList failed", e);
            return "查询用户列表失败: " + e.getMessage();
        }
    }
}
