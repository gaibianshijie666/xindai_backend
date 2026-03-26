package com.xindai.xindai.modules.agent.tools;

import com.xindai.xindai.modules.agent.annotation.ToolAllowed;
import com.xindai.xindai.modules.enterprise.dto.DashboardOverviewVO;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseCustomerQueryDTO;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseCustomerVO;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseLoanQueryDTO;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseLoanVO;
import com.xindai.xindai.modules.enterprise.service.EnterpriseCustomerService;
import com.xindai.xindai.modules.enterprise.service.EnterpriseDashboardService;
import com.xindai.xindai.modules.enterprise.service.EnterpriseLoanService;
import com.xindai.xindai.modules.loan.enums.ApplicationStatus;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnterpriseTools {

    private final EnterpriseDashboardService enterpriseDashboardService;
    private final EnterpriseCustomerService enterpriseCustomerService;
    private final EnterpriseLoanService enterpriseLoanService;

    @ToolAllowed(portals = {"enterprise", "admin"})
    @Tool("获取企业仪表盘概览数据，包括客户总数、贷款总额等")
    public String getDashboardOverview() {
        try {
            Long enterpriseId = ToolContext.getEnterpriseId();
            if (enterpriseId == null) return "缺少企业ID，无法查询";
            DashboardOverviewVO vo = enterpriseDashboardService.getOverview(enterpriseId);
            DashboardOverviewVO.RiskDistribution rd = vo.getRiskDistribution();
            return String.format(
                    "客户总数: %d, 总贷款数: %d, 总贷款金额: %.2f元, " +
                    "今日新增客户: %d, 今日放款: %d笔 %.2f元" +
                    "%s",
                    vo.getTotalCustomers() != null ? vo.getTotalCustomers() : 0,
                    vo.getTotalLoanCount() != null ? vo.getTotalLoanCount() : 0,
                    vo.getTotalLoanAmount() != null ? vo.getTotalLoanAmount() : 0,
                    vo.getTodayNewCustomers() != null ? vo.getTodayNewCustomers() : 0,
                    vo.getTodayLoanCount() != null ? vo.getTodayLoanCount() : 0,
                    vo.getTodayLoanAmount() != null ? vo.getTodayLoanAmount() : 0,
                    rd != null ? String.format(", 风险分布(低/中/高): %d/%d/%d",
                            rd.getLow() != null ? rd.getLow() : 0,
                            rd.getMedium() != null ? rd.getMedium() : 0,
                            rd.getHigh() != null ? rd.getHigh() : 0) : ""
            );
        } catch (Exception e) {
            log.error("Tool getEnterpriseDashboard failed", e);
            return "查询企业仪表盘失败: " + e.getMessage();
        }
    }

    @ToolAllowed(portals = {"enterprise", "admin"})
    @Tool("查询企业客户列表，可提供关键字搜索客户姓名或手机号")
    public String getCustomerList(String keyword) {
        try {
            Long enterpriseId = ToolContext.getEnterpriseId();
            if (enterpriseId == null) return "缺少企业ID，无法查询";
            Page<EnterpriseCustomerVO> page = enterpriseCustomerService.list(enterpriseId, queryDTO(keyword));
            if (page.getRecords().isEmpty()) return "未找到匹配的客户";
            return page.getRecords().stream().map(c -> String.format(
                    "客户ID: %d, 姓名: %s, 手机号: %s, 状态: %s",
                    c.getId(), c.getRealName(), c.getPhone(),
                    c.getStatus() == 1 ? "正常" : "禁用"
            )).collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("Tool getCustomerList failed", e);
            return "查询客户列表失败: " + e.getMessage();
        }
    }

    @ToolAllowed(portals = {"enterprise", "admin"})
    @Tool("查询企业的贷款列表，可提供状态筛选: 0=待审批, 1=审批中, 2=已通过, 3=已拒绝, 4=已放款, 5=已结清")
    public String getLoanList(Integer status) {
        try {
            Long enterpriseId = ToolContext.getEnterpriseId();
            if (enterpriseId == null) return "缺少企业ID，无法查询";
            Page<EnterpriseLoanVO> page = enterpriseLoanService.list(enterpriseId, loanQueryDTO(status));
            if (page.getRecords().isEmpty()) return "暂无贷款记录";
            return page.getRecords().stream().map(l -> String.format(
                    "申请编号: %s, 客户: %s, 金额: %.2f元, 期限: %d个月, 状态: %s",
                    l.getApplicationNo(), l.getCustomerName(), l.getAmount(), l.getTerm(),
                    mapStatus(l.getStatus())
            )).collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("Tool getEnterpriseLoanList failed", e);
            return "查询贷款列表失败: " + e.getMessage();
        }
    }

    private String mapStatus(Integer status) {
        if (status == null) return "未知";
        return ApplicationStatus.fromCode(status).getDesc();
    }

    private EnterpriseCustomerQueryDTO queryDTO(String keyword) {
        EnterpriseCustomerQueryDTO dto = new EnterpriseCustomerQueryDTO();
        dto.setKeyword(keyword);
        dto.setPage(1);
        dto.setSize(20);
        return dto;
    }

    private EnterpriseLoanQueryDTO loanQueryDTO(Integer status) {
        EnterpriseLoanQueryDTO dto = new EnterpriseLoanQueryDTO();
        dto.setStatus(status);
        dto.setPage(1);
        dto.setSize(20);
        return dto;
    }
}
