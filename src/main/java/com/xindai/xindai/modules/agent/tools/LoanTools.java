package com.xindai.xindai.modules.agent.tools;

import com.xindai.xindai.modules.agent.annotation.ToolAllowed;
import com.xindai.xindai.modules.loan.dto.CreditLimitVO;
import com.xindai.xindai.modules.loan.dto.LoanApplicationVO;
import com.xindai.xindai.modules.loan.dto.LoanContractVO;
import com.xindai.xindai.modules.loan.dto.RepaymentPlanVO;
import com.xindai.xindai.modules.loan.enums.ApplicationStatus;
import com.xindai.xindai.modules.loan.enums.ContractStatus;
import com.xindai.xindai.modules.loan.enums.RepaymentStatus;
import com.xindai.xindai.modules.loan.service.LoanService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanTools {

    private final LoanService loanService;

    @ToolAllowed(portals = {"user", "admin", "enterprise"})
    @Tool("查询当前用户的信用额度信息，包括总额度、已用额度、可用额度")
    public String getCreditLimit() {
        try {
            CreditLimitVO vo = loanService.getCreditLimit(ToolContext.getUserId());
            return String.format("总额度: %.2f元, 已用额度: %.2f元, 可用额度: %.2f元, 说明: %s",
                    vo.getTotalLimit(), vo.getUsedLimit(), vo.getAvailableLimit(),
                    vo.getExplanation() != null ? vo.getExplanation() : "无");
        } catch (Exception e) {
            return "查询额度失败: " + e.getMessage();
        }
    }

    @ToolAllowed(portals = {"user", "admin", "enterprise"})
    @Tool("查询当前用户的所有借款申请记录")
    public String getLoanApplications() {
        try {
            List<LoanApplicationVO> apps = loanService.getApplications(ToolContext.getUserId());
            if (apps.isEmpty()) return "暂无借款申请记录";
            return apps.stream().map(a -> String.format(
                    "申请编号: %s, 金额: %.2f元, 期限: %d个月, 状态: %s, 申请时间: %s",
                    a.getApplicationNo(), a.getAmount(), a.getTerm(),
                    mapAppStatus(a.getStatus()), a.getCreatedAt()
            )).collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "查询借款申请失败: " + e.getMessage();
        }
    }

    @ToolAllowed(portals = {"user", "admin", "enterprise"})
    @Tool("查询当前用户的所有借款合同")
    public String getLoanContracts() {
        try {
            List<LoanContractVO> contracts = loanService.getContracts(ToolContext.getUserId());
            if (contracts.isEmpty()) return "暂无借款合同";
            return contracts.stream().map(c -> String.format(
                    "合同编号: %s, 本金: %.2f元, 利率: %.2f%%, 期限: %d个月, 状态: %s",
                    c.getContractNo(), c.getPrincipal(), c.getInterestRate().doubleValue() * 100,
                    c.getTerm(), mapContractStatus(c.getStatus())
            )).collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "查询合同失败: " + e.getMessage();
        }
    }

    @ToolAllowed(portals = {"user", "admin", "enterprise"})
    @Tool("查询当前用户的待还款列表")
    public String getPendingRepayment() {
        try {
            List<RepaymentPlanVO> plans = loanService.getPendingRepayment(ToolContext.getUserId());
            if (plans.isEmpty()) return "暂无待还款项";
            return plans.stream().map(p -> String.format(
                    "合同编号: %s, 第%d期, 应还: %.2f元, 到期日: %s",
                    p.getContractNo(), p.getPeriod(), p.getTotalAmount(), p.getDueDate()
            )).collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "查询待还款失败: " + e.getMessage();
        }
    }

    @ToolAllowed(portals = {"user", "admin", "enterprise"})
    @Tool("查询指定合同的全部还款计划，需要提供合同ID")
    public String getRepaymentPlans(Long contractId) {
        try {
            List<RepaymentPlanVO> plans = loanService.getRepaymentPlansByContract(ToolContext.getUserId(), contractId);
            if (plans.isEmpty()) return "该合同暂无还款计划";
            return plans.stream().map(p -> String.format(
                    "第%d期, 本金: %.2f元, 利息: %.2f元, 应还: %.2f元, 状态: %s, 到期日: %s",
                    p.getPeriod(), p.getPrincipal(), p.getInterest(),
                    p.getTotalAmount(), mapRepayStatus(p.getStatus()), p.getDueDate()
            )).collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "查询还款计划失败: " + e.getMessage();
        }
    }

    private String mapAppStatus(Integer status) {
        if (status == null) return "未知";
        return ApplicationStatus.fromCode(status).getDesc();
    }

    private String mapContractStatus(Integer status) {
        if (status == null) return "未知";
        try {
            return ContractStatus.fromCode(status).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }

    private String mapRepayStatus(Integer status) {
        if (status == null) return "未知";
        try {
            return RepaymentStatus.fromCode(status).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
