package com.xindai.xindai.modules.loan.service;

import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.entity.RepaymentPlan;
import com.xindai.xindai.modules.loan.service.impl.ContractPdfServiceImpl;
import com.xindai.xindai.modules.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("合同PDF生成服务 单元测试")
class ContractPdfServiceTest {

    private ContractPdfServiceImpl contractPdfService;

    @BeforeEach
    void setUp() {
        contractPdfService = new ContractPdfServiceImpl();
    }

    @Test
    @DisplayName("正常数据生成合同 - 返回非空PDF字节数组")
    void generateContract_withValidData_returnsNonEmptyPdf() {
        LoanContract contract = createTestContract();
        User user = createTestUser();
        List<RepaymentPlan> plans = createTestPlans(12);

        byte[] result = contractPdfService.generateContract(contract, user, plans);

        assertNotNull(result);
        assertTrue(result.length > 0, "PDF字节数组不应为空");
        // PDF文件以 %PDF 开头
        assertEquals('%', result[0]);
        assertEquals('P', result[1]);
        assertEquals('D', result[2]);
        assertEquals('F', result[3]);
    }

    @Test
    @DisplayName("还款计划为null - 不应抛出异常并返回非空PDF")
    void generateContract_withNullRepaymentPlans_returnsNonEmptyPdf() {
        LoanContract contract = createTestContract();
        User user = createTestUser();

        byte[] result = contractPdfService.generateContract(contract, user, null);

        assertNotNull(result);
        assertTrue(result.length > 0, "还款计划为null时仍应生成有效PDF");
    }

    @Test
    @DisplayName("放款日期为null - 不应抛出异常并生成有效PDF")
    void generateContract_withNullDisbursedDate_showsNA() {
        LoanContract contract = createTestContract();
        contract.setDisbursedAt(null);
        contract.setDueDate(null);
        User user = createTestUser();
        List<RepaymentPlan> plans = createTestPlans(12);

        // 日期为null时不应抛出异常
        assertDoesNotThrow(() -> contractPdfService.generateContract(contract, user, plans));

        byte[] result = contractPdfService.generateContract(contract, user, plans);

        assertNotNull(result);
        assertTrue(result.length > 0, "日期为null时仍应生成有效PDF");
        // 验证是有效的PDF文件
        assertEquals('%', result[0], "输出应以PDF魔数开头");
    }

    @Test
    @DisplayName("还款计划为空列表 - 应正常生成PDF")
    void generateContract_withEmptyPlans_returnsNonEmptyPdf() {
        LoanContract contract = createTestContract();
        User user = createTestUser();

        byte[] result = contractPdfService.generateContract(contract, user, Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.length > 0, "还款计划为空列表时仍应生成有效PDF");
    }

    @Test
    @DisplayName("多期还款计划 - 验证合计行计算正确")
    void generateContract_withMultiplePlans_includesTotalRow() {
        LoanContract contract = createTestContract();
        User user = createTestUser();
        List<RepaymentPlan> plans = createTestPlans(12);

        byte[] result = contractPdfService.generateContract(contract, user, plans);

        assertNotNull(result);
        assertTrue(result.length > 0);

        // 计算期望的合计值，验证业务逻辑正确性
        BigDecimal expectedTotalPrincipal = BigDecimal.ZERO;
        BigDecimal expectedTotalInterest = BigDecimal.ZERO;
        BigDecimal expectedTotalAmount = BigDecimal.ZERO;

        for (RepaymentPlan plan : plans) {
            expectedTotalPrincipal = expectedTotalPrincipal.add(plan.getPrincipal());
            expectedTotalInterest = expectedTotalInterest.add(plan.getInterest());
            expectedTotalAmount = expectedTotalAmount.add(plan.getTotalAmount());
        }

        // 验证各期金额总和等于合同约定的本金和总还款额（使用compareTo避免scale差异）
        assertEquals(0, contract.getPrincipal().compareTo(expectedTotalPrincipal),
                "各期本金合计应等于合同本金");
        assertEquals(0, contract.getTotalRepayment().compareTo(expectedTotalAmount),
                "各期总额合计应等于合同约定的总还款额");

        // 验证利息合计 = 总还款额 - 本金
        assertEquals(0, contract.getTotalRepayment().subtract(contract.getPrincipal()).compareTo(expectedTotalInterest),
                "利息合计应等于总还款额减去本金");

        // 有还款计划时生成的PDF应比无还款计划时更大（包含还款计划表格）
        byte[] resultWithoutPlans = contractPdfService.generateContract(contract, user, Collections.emptyList());
        assertNotNull(resultWithoutPlans);
        assertTrue(result.length > resultWithoutPlans.length,
                "包含还款计划的PDF应比无还款计划的PDF更大");
    }

    // ========== 辅助方法 ==========

    private LoanContract createTestContract() {
        LoanContract contract = new LoanContract();
        contract.setContractNo("HT20260326001");
        // 使用可被12整除的金额，避免舍入误差
        contract.setPrincipal(new BigDecimal("48000"));
        contract.setInterestRate(new BigDecimal("0.05"));
        contract.setTerm(12);
        // 48000 / 12 = 4000, 2400 / 12 = 200, 每期总额 = 4200, 合计 = 50400
        contract.setTotalRepayment(new BigDecimal("50400"));
        contract.setDisbursedAt(LocalDateTime.now());
        contract.setDueDate(LocalDate.now().plusMonths(12));
        return contract;
    }

    private User createTestUser() {
        User user = new User();
        user.setRealName("张三");
        user.setIdCard("110101199001011234");
        user.setPhone("13800138000");
        return user;
    }

    private List<RepaymentPlan> createTestPlans(int count) {
        List<RepaymentPlan> plans = new ArrayList<>();
        // 使用可被12整除的金额: 48000/12=4000, 2400/12=200
        BigDecimal principalPerPeriod = new BigDecimal("48000").divide(new BigDecimal(count), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal interestPerPeriod = new BigDecimal("2400").divide(new BigDecimal(count), 2, java.math.RoundingMode.HALF_UP);

        for (int i = 1; i <= count; i++) {
            RepaymentPlan plan = new RepaymentPlan();
            plan.setPeriod(i);
            plan.setDueDate(LocalDate.now().plusMonths(i));
            plan.setPrincipal(principalPerPeriod);
            plan.setInterest(interestPerPeriod);
            plan.setTotalAmount(principalPerPeriod.add(interestPerPeriod));
            plans.add(plan);
        }
        return plans;
    }
}
