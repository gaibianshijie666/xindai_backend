package com.xindai.xindai.modules.loan.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.loan.dto.BankAccountDTO;
import com.xindai.xindai.modules.loan.dto.DisbursementVO;
import com.xindai.xindai.modules.loan.entity.BankAccount;
import com.xindai.xindai.modules.loan.entity.DisbursementRecord;
import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.enums.DisbursementStatus;
import com.xindai.xindai.modules.loan.mapper.BankAccountMapper;
import com.xindai.xindai.modules.loan.mapper.DisbursementRecordMapper;
import com.xindai.xindai.modules.loan.mapper.LoanContractMapper;
import com.xindai.xindai.modules.loan.service.DisbursementService;
import com.xindai.xindai.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 放款服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisbursementServiceImpl implements DisbursementService {

    private final BankAccountMapper bankAccountMapper;
    private final DisbursementRecordMapper disbursementRecordMapper;
    private final LoanContractMapper loanContractMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankAccount bindBankAccount(Long userId, BankAccountDTO dto) {
        // 限制每人最多绑定5张银行卡
        long count = bankAccountMapper.selectCount(
                new LambdaQueryWrapper<BankAccount>()
                        .eq(BankAccount::getUserId, userId)
                        .eq(BankAccount::getStatus, 1)
        );
        if (count >= 5) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "最多绑定5张银行卡");
        }

        BankAccount account = new BankAccount();
        account.setUserId(userId);
        account.setBankName(dto.getBankName());
        account.setAccountNo(dto.getAccountNo());
        account.setAccountName(dto.getAccountName());
        account.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()) ? 1 : 0);
        account.setStatus(1);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        // 如果设为默认，先清除其他默认
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            clearDefaultBankAccount(userId);
        }

        bankAccountMapper.insert(account);
        log.info("User {} bound bank account: {}", userId, account.getId());
        return account;
    }

    @Override
    public List<BankAccount> getBankAccounts(Long userId) {
        return bankAccountMapper.selectList(
                new LambdaQueryWrapper<BankAccount>()
                        .eq(BankAccount::getUserId, userId)
                        .eq(BankAccount::getStatus, 1)
                        .orderByDesc(BankAccount::getIsDefault)
                        .orderByDesc(BankAccount::getCreatedAt)
        );
    }

    @Override
    public BankAccount getDefaultBankAccount(Long userId) {
        BankAccount account = bankAccountMapper.selectOne(
                new LambdaQueryWrapper<BankAccount>()
                        .eq(BankAccount::getUserId, userId)
                        .eq(BankAccount::getIsDefault, 1)
                        .eq(BankAccount::getStatus, 1)
        );

        if (account == null) {
            // 没有默认银行卡，返回第一张正常状态的卡
            account = bankAccountMapper.selectOne(
                    new LambdaQueryWrapper<BankAccount>()
                            .eq(BankAccount::getUserId, userId)
                            .eq(BankAccount::getStatus, 1)
                            .orderByDesc(BankAccount::getCreatedAt)
                            .last("LIMIT 1")
            );
        }

        return account;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindBankAccount(Long userId, Long bankAccountId) {
        BankAccount account = bankAccountMapper.selectOne(
                new LambdaQueryWrapper<BankAccount>()
                        .eq(BankAccount::getId, bankAccountId)
                        .eq(BankAccount::getUserId, userId)
        );

        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "银行卡不存在");
        }

        account.setStatus(0);
        account.setUpdatedAt(LocalDateTime.now());
        bankAccountMapper.updateById(account);

        // 如果解绑的是默认卡，设置下一张为默认
        if (account.getIsDefault() == 1) {
            BankAccount nextDefault = bankAccountMapper.selectOne(
                    new LambdaQueryWrapper<BankAccount>()
                            .eq(BankAccount::getUserId, userId)
                            .eq(BankAccount::getStatus, 1)
                            .orderByDesc(BankAccount::getCreatedAt)
                            .last("LIMIT 1")
            );
            if (nextDefault != null) {
                nextDefault.setIsDefault(1);
                nextDefault.setUpdatedAt(LocalDateTime.now());
                bankAccountMapper.updateById(nextDefault);
            }
        }

        log.info("User {} unbound bank account: {}", userId, bankAccountId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DisbursementRecord createDisbursement(Long contractId, Long applicationId,
                                                 Long userId, BigDecimal amount, Long bankAccountId) {
        // 验证合同
        LoanContract contract = loanContractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_FOUND, "id=" + contractId);
        }

        // 验证银行卡
        BankAccount bankAccount = bankAccountMapper.selectOne(
                new LambdaQueryWrapper<BankAccount>()
                        .eq(BankAccount::getId, bankAccountId)
                        .eq(BankAccount::getUserId, userId)
                        .eq(BankAccount::getStatus, 1)
        );
        if (bankAccount == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "银行卡不存在或已解绑");
        }

        DisbursementRecord record = new DisbursementRecord();
        record.setContractId(contractId);
        record.setApplicationId(applicationId);
        record.setUserId(userId);
        record.setAmount(amount);
        record.setBankAccountId(bankAccountId);
        record.setStatus(DisbursementStatus.PENDING.getCode());
        record.setCreatedAt(LocalDateTime.now());

        disbursementRecordMapper.insert(record);
        log.info("Created disbursement record: id={}, contractId={}, amount={}",
                record.getId(), contractId, amount);

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeDisbursement(Long disbursementId) {
        DisbursementRecord record = disbursementRecordMapper.selectById(disbursementId);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "放款记录不存在");
        }

        if (record.getStatus() != DisbursementStatus.PENDING.getCode()
                && record.getStatus() != DisbursementStatus.FAILED.getCode()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前放款记录状态不允许执行放款");
        }

        // 更新为放款中
        record.setStatus(DisbursementStatus.PROCESSING.getCode());
        disbursementRecordMapper.updateById(record);

        // 模拟放款操作（实际对接银行/支付渠道）
        try {
            // 生成交易流水号
            String transactionNo = "TXN" + IdUtil.getSnowflakeNextIdStr();

            // 模拟放款处理
            simulateDisbursement(record, transactionNo);

            // 放款成功
            record.setStatus(DisbursementStatus.COMPLETED.getCode());
            record.setTransactionNo(transactionNo);
            record.setCompletedAt(LocalDateTime.now());
            disbursementRecordMapper.updateById(record);

            // 更新合同状态为放款中（还款中）
            LoanContract contract = loanContractMapper.selectById(record.getContractId());
            if (contract != null) {
                contract.setStatus(1); // 还款中
                contract.setDisbursedAt(LocalDateTime.now());
                loanContractMapper.updateById(contract);
            }

            log.info("Disbursement completed: id={}, transactionNo={}, amount={}",
                    record.getId(), transactionNo, record.getAmount());

            notificationService.send(record.getUserId(), "USER", "放款成功",
                    "您的贷款已成功放款，金额：" + record.getAmount() + "元，流水号：" + transactionNo + "。",
                    "LOAN", record.getContractId().toString());

        } catch (Exception e) {
            log.error("Disbursement failed: id={}", disbursementId, e);
            record.setStatus(DisbursementStatus.FAILED.getCode());
            record.setFailedReason(e.getMessage());
            disbursementRecordMapper.updateById(record);

            notificationService.send(record.getUserId(), "USER", "放款失败",
                    "您的贷款放款处理失败，原因：" + e.getMessage() + "，我们将尽快处理。",
                    "LOAN", record.getContractId().toString());
        }
    }

    @Override
    public List<DisbursementVO> getDisbursementRecords(Integer status) {
        LambdaQueryWrapper<DisbursementRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(DisbursementRecord::getStatus, status);
        }
        wrapper.orderByDesc(DisbursementRecord::getCreatedAt);

        List<DisbursementRecord> records = disbursementRecordMapper.selectList(wrapper);
        return records.stream().map(this::toDisbursementVO).collect(Collectors.toList());
    }

    @Override
    public List<DisbursementVO> getUserDisbursements(Long userId) {
        List<DisbursementRecord> records = disbursementRecordMapper.selectList(
                new LambdaQueryWrapper<DisbursementRecord>()
                        .eq(DisbursementRecord::getUserId, userId)
                        .orderByDesc(DisbursementRecord::getCreatedAt)
        );
        return records.stream().map(this::toDisbursementVO).collect(Collectors.toList());
    }

    /**
     * 模拟放款处理（实际对接银行/第三方支付渠道）
     */
    private void simulateDisbursement(DisbursementRecord record, String transactionNo) {
        // Mock: 模拟银行处理延迟
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Simulated bank transfer: transactionNo={}, amount={}, bankAccountId={}",
                transactionNo, record.getAmount(), record.getBankAccountId());
    }

    private void clearDefaultBankAccount(Long userId) {
        bankAccountMapper.update(null,
                new LambdaUpdateWrapper<BankAccount>()
                        .eq(BankAccount::getUserId, userId)
                        .eq(BankAccount::getIsDefault, 1)
                        .set(BankAccount::getIsDefault, 0)
                        .set(BankAccount::getUpdatedAt, LocalDateTime.now())
        );
    }

    private DisbursementVO toDisbursementVO(DisbursementRecord record) {
        DisbursementVO vo = new DisbursementVO();
        vo.setId(record.getId());
        vo.setContractId(record.getContractId());
        vo.setApplicationId(record.getApplicationId());
        vo.setUserId(record.getUserId());
        vo.setAmount(record.getAmount());
        vo.setBankAccountId(record.getBankAccountId());
        vo.setStatus(record.getStatus());
        vo.setStatusDesc(DisbursementStatus.fromCode(record.getStatus()).getDesc());
        vo.setTransactionNo(record.getTransactionNo());
        vo.setCompletedAt(record.getCompletedAt());
        vo.setFailedReason(record.getFailedReason());
        vo.setCreatedAt(record.getCreatedAt());

        // 查询银行卡信息
        BankAccount bankAccount = bankAccountMapper.selectById(record.getBankAccountId());
        if (bankAccount != null) {
            vo.setBankName(bankAccount.getBankName());
            vo.setAccountNo(desensitizeAccountNo(bankAccount.getAccountNo()));
        }

        return vo;
    }

    /**
     * 银行卡号脱敏
     */
    private String desensitizeAccountNo(String accountNo) {
        if (accountNo == null || accountNo.length() < 8) {
            return accountNo;
        }
        return accountNo.substring(0, 4) + "****" + accountNo.substring(accountNo.length() - 4);
    }
}
