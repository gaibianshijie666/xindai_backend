package com.xindai.xindai.modules.loan.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.entity.RepaymentPlan;
import com.xindai.xindai.modules.loan.service.ContractPdfService;
import com.xindai.xindai.modules.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 电子合同PDF生成服务实现
 */
@Slf4j
@Service
public class ContractPdfServiceImpl implements ContractPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    @Override
    public byte[] generateContract(LoanContract contract, User user, List<RepaymentPlan> repaymentPlans) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);

            document.open();

            // 使用内置字体（避免中文乱码问题，使用Helvetica作为fallback）
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.BOLD);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL);

            // === 合同标题 ===
            Paragraph title = new Paragraph("LOAN CONTRACT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(5);
            document.add(title);

            Paragraph contractNo = new Paragraph("Contract No: " + contract.getContractNo(), normalFont);
            contractNo.setAlignment(Element.ALIGN_CENTER);
            contractNo.setSpacingAfter(20);
            document.add(contractNo);

            // === 当事人信息 ===
            document.add(createSectionHeader("PARTY INFORMATION", headerFont));
            PdfPTable partyTable = new PdfPTable(2);
            partyTable.setWidthPercentage(100);
            partyTable.setSpacingAfter(10);

            addTableCell(partyTable, "Borrower (Lender Party):", normalFont, Element.ALIGN_LEFT);
            addTableCell(partyTable, maskName(user.getRealName()), normalFont, Element.ALIGN_LEFT);
            addTableCell(partyTable, "ID Card:", normalFont, Element.ALIGN_LEFT);
            addTableCell(partyTable, maskIdCard(user.getIdCard()), normalFont, Element.ALIGN_LEFT);
            addTableCell(partyTable, "Phone:", normalFont, Element.ALIGN_LEFT);
            addTableCell(partyTable, maskPhone(user.getPhone()), normalFont, Element.ALIGN_LEFT);

            document.add(partyTable);
            addBlankLine(document);

            // === 借款详情 ===
            document.add(createSectionHeader("LOAN DETAILS", headerFont));
            PdfPTable loanTable = new PdfPTable(2);
            loanTable.setWidthPercentage(100);
            loanTable.setSpacingAfter(10);

            addTableCell(loanTable, "Principal (CNY):", normalFont, Element.ALIGN_LEFT);
            addTableCell(loanTable, contract.getPrincipal().toPlainString(), normalFont, Element.ALIGN_RIGHT);
            addTableCell(loanTable, "Interest Rate (%):", normalFont, Element.ALIGN_LEFT);
            addTableCell(loanTable, contract.getInterestRate().toPlainString() + "%", normalFont, Element.ALIGN_RIGHT);
            addTableCell(loanTable, "Term (months):", normalFont, Element.ALIGN_LEFT);
            addTableCell(loanTable, String.valueOf(contract.getTerm()), normalFont, Element.ALIGN_RIGHT);
            addTableCell(loanTable, "Total Repayment (CNY):", normalFont, Element.ALIGN_LEFT);
            addTableCell(loanTable, contract.getTotalRepayment().toPlainString(), normalFont, Element.ALIGN_RIGHT);
            addTableCell(loanTable, "Disbursement Date:", normalFont, Element.ALIGN_LEFT);
            addTableCell(loanTable, contract.getDisbursedAt() != null
                    ? contract.getDisbursedAt().toLocalDate().format(DATE_FORMATTER) : "N/A",
                    normalFont, Element.ALIGN_RIGHT);
            addTableCell(loanTable, "Due Date:", normalFont, Element.ALIGN_LEFT);
            addTableCell(loanTable, contract.getDueDate() != null
                    ? contract.getDueDate().format(DATE_FORMATTER) : "N/A",
                    normalFont, Element.ALIGN_RIGHT);

            document.add(loanTable);
            addBlankLine(document);

            // === 还款计划表 ===
            document.add(createSectionHeader("REPAYMENT SCHEDULE", headerFont));

            if (repaymentPlans != null && !repaymentPlans.isEmpty()) {
                PdfPTable planTable = new PdfPTable(5);
                planTable.setWidthPercentage(100);
                planTable.setSpacingAfter(10);

                // 表头
                String[] headers = {"Period", "Due Date", "Principal", "Interest", "Total"};
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setPadding(5);
                    planTable.addCell(cell);
                }

                // 数据行
                for (RepaymentPlan plan : repaymentPlans) {
                    addCellCenter(planTable, String.valueOf(plan.getPeriod()), normalFont);
                    addCellCenter(planTable, plan.getDueDate().format(DATE_FORMATTER), normalFont);
                    addCellRight(planTable, plan.getPrincipal().toPlainString(), normalFont);
                    addCellRight(planTable, plan.getInterest().toPlainString(), normalFont);
                    addCellRight(planTable, plan.getTotalAmount().toPlainString(), normalFont);
                }

                // 合计行
                BigDecimal totalPrincipal = repaymentPlans.stream()
                        .map(RepaymentPlan::getPrincipal).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalInterest = repaymentPlans.stream()
                        .map(RepaymentPlan::getInterest).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalAmount = repaymentPlans.stream()
                        .map(RepaymentPlan::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

                addCellCenter(planTable, "TOTAL", headerFont);
                addCellCenter(planTable, "", normalFont);
                addCellRight(planTable, totalPrincipal.toPlainString(), headerFont);
                addCellRight(planTable, totalInterest.toPlainString(), headerFont);
                addCellRight(planTable, totalAmount.toPlainString(), headerFont);

                document.add(planTable);
            }
            addBlankLine(document);

            // === 合同条款 ===
            document.add(createSectionHeader("TERMS AND CONDITIONS", headerFont));

            String[] terms = {
                "1. The borrower agrees to repay the loan amount with interest according to the repayment schedule above.",
                "2. Late payment shall incur a penalty of 0.05% per day on the overdue amount.",
                "3. The lender reserves the right to take legal action in case of default.",
                "4. This contract is electronically generated and has the same legal effect as a paper contract.",
                "5. Any disputes shall be resolved through negotiation or submitted to the competent court.",
                "6. The borrower confirms that all information provided is true and accurate."
            };
            for (String term : terms) {
                Paragraph p = new Paragraph(term, smallFont);
                p.setSpacingAfter(3);
                document.add(p);
            }
            addBlankLine(document);

            // === 签名区域 ===
            document.add(createSectionHeader("SIGNATURES", headerFont));
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setSpacingBefore(20);

            addTableCell(signTable, "Borrower Signature:", normalFont, Element.ALIGN_LEFT);
            addTableCell(signTable, "Lender Signature:", normalFont, Element.ALIGN_LEFT);
            addTableCell(signTable, "", normalFont, Element.ALIGN_LEFT);
            addTableCell(signTable, "", normalFont, Element.ALIGN_LEFT);
            addTableCell(signTable, "Date: _______________", normalFont, Element.ALIGN_LEFT);
            addTableCell(signTable, "Date: _______________", normalFont, Element.ALIGN_LEFT);

            document.add(signTable);

            // === 印章区域（占位） ===
            addBlankLine(document);
            Paragraph sealPlaceholder = new Paragraph("[Company Seal Placeholder]", smallFont);
            sealPlaceholder.setAlignment(Element.ALIGN_CENTER);
            document.add(sealPlaceholder);

            // 页脚
            addBlankLine(document);
            Paragraph footer = new Paragraph(
                    "This document was generated electronically by Xindai Loan System.",
                    smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            log.info("Contract PDF generated successfully: contractNo={}", contract.getContractNo());
            return baos.toByteArray();

        } catch (DocumentException | IOException e) {
            log.error("Failed to generate contract PDF: contractNo={}", contract.getContractNo(), e);
            throw new RuntimeException("Failed to generate contract PDF", e);
        }
    }

    private Paragraph createSectionHeader(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(10);
        p.setSpacingAfter(5);
        return p;
    }

    private void addBlankLine(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
    }

    private void addTableCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void addCellCenter(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addCellRight(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private String maskName(String name) {
        if (name == null || name.length() <= 1) return name;
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
