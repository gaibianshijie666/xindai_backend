package com.xindai.xindai.modules.loan;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.xindai.xindai.modules.loan.service.impl.DisbursementServiceImpl;
import com.xindai.xindai.modules.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DisbursementService 单元测试")
class DisbursementServiceTest {

    @Mock
    private BankAccountMapper bankAccountMapper;

    @Mock
    private DisbursementRecordMapper disbursementRecordMapper;

    @Mock
    private LoanContractMapper loanContractMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DisbursementServiceImpl disbursementService;

    private BankAccountDTO bankAccountDTO;
    private BankAccount existingDefaultBankAccount;
    private BankAccount nonDefaultBankAccount;
    private LoanContract loanContract;
    private DisbursementRecord pendingRecord;

    @BeforeEach
    void setUp() {
        bankAccountDTO = new BankAccountDTO();
        bankAccountDTO.setBankName("中国工商银行");
        bankAccountDTO.setAccountNo("6222021234567890123");
        bankAccountDTO.setAccountName("张三");
        bankAccountDTO.setIsDefault(false);

        existingDefaultBankAccount = new BankAccount();
        existingDefaultBankAccount.setId(1L);
        existingDefaultBankAccount.setUserId(1L);
        existingDefaultBankAccount.setBankName("中国建设银行");
        existingDefaultBankAccount.setAccountNo("6227001234567890123");
        existingDefaultBankAccount.setAccountName("张三");
        existingDefaultBankAccount.setIsDefault(1);
        existingDefaultBankAccount.setStatus(1);
        existingDefaultBankAccount.setCreatedAt(LocalDateTime.now());
        existingDefaultBankAccount.setUpdatedAt(LocalDateTime.now());

        nonDefaultBankAccount = new BankAccount();
        nonDefaultBankAccount.setId(2L);
        nonDefaultBankAccount.setUserId(1L);
        nonDefaultBankAccount.setBankName("中国农业银行");
        nonDefaultBankAccount.setAccountNo("6228481234567890123");
        nonDefaultBankAccount.setAccountName("张三");
        nonDefaultBankAccount.setIsDefault(0);
        nonDefaultBankAccount.setStatus(1);
        nonDefaultBankAccount.setCreatedAt(LocalDateTime.now());
        nonDefaultBankAccount.setUpdatedAt(LocalDateTime.now());

        loanContract = new LoanContract();
        loanContract.setId(100L);
        loanContract.setContractNo("HT20260326001");
        loanContract.setUserId(1L);
        loanContract.setPrincipal(new BigDecimal("10000"));
        loanContract.setStatus(0);

        pendingRecord = new DisbursementRecord();
        pendingRecord.setId(1L);
        pendingRecord.setContractId(100L);
        pendingRecord.setApplicationId(10L);
        pendingRecord.setUserId(1L);
        pendingRecord.setAmount(new BigDecimal("10000"));
        pendingRecord.setBankAccountId(1L);
        pendingRecord.setStatus(DisbursementStatus.PENDING.getCode());
        pendingRecord.setCreatedAt(LocalDateTime.now());
    }

    // ==================== 银行卡绑定测试 ====================

    @Nested
    @DisplayName("银行卡绑定测试")
    class BindBankAccountTests {

        @Test
        @DisplayName("绑定非默认银行卡 - 成功")
        void bindBankAccount_NonDefault_Success() {
            // Given
            when(bankAccountMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
            when(bankAccountMapper.insert(any(BankAccount.class))).thenReturn(1);

            // When
            BankAccount result = disbursementService.bindBankAccount(1L, bankAccountDTO);

            // Then
            assertNotNull(result);
            assertEquals("中国工商银行", result.getBankName());
            assertEquals("6222021234567890123", result.getAccountNo());
            assertEquals("张三", result.getAccountName());
            assertEquals(0, result.getIsDefault());
            assertEquals(1, result.getStatus());
            verify(bankAccountMapper).insert(any(BankAccount.class));
            // 非默认卡绑定，不应调用 update 清除其他默认
            verify(bankAccountMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("绑定银行卡超过5张上限 - 抛出异常")
        void bindBankAccount_ExceedLimit_ThrowsException() {
            // Given
            when(bankAccountMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> disbursementService.bindBankAccount(1L, bankAccountDTO));

            assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("最多绑定5张银行卡"));
            verify(bankAccountMapper, never()).insert(any());
        }

        @Test
        @DisplayName("刚好4张卡时仍可绑定第5张 - 成功")
        void bindBankAccount_AtLimitBoundary_Success() {
            // Given
            when(bankAccountMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(4L);
            when(bankAccountMapper.insert(any(BankAccount.class))).thenReturn(1);

            // When
            BankAccount result = disbursementService.bindBankAccount(1L, bankAccountDTO);

            // Then
            assertNotNull(result);
            verify(bankAccountMapper).insert(any(BankAccount.class));
        }
    }

    // ==================== 银行卡查询测试 ====================

    @Nested
    @DisplayName("银行卡查询测试")
    class GetBankAccountsTests {

        @Test
        @DisplayName("获取银行卡列表 - 默认卡排在前面")
        void getBankAccounts_DefaultCardFirst() {
            // Given
            BankAccount card1 = new BankAccount();
            card1.setId(1L);
            card1.setIsDefault(1);
            card1.setBankName("中国工商银行");
            BankAccount card2 = new BankAccount();
            card2.setId(2L);
            card2.setIsDefault(0);
            card2.setBankName("中国建设银行");

            when(bankAccountMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(card1, card2));

            // When
            List<BankAccount> result = disbursementService.getBankAccounts(1L);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(1, result.get(0).getIsDefault()); // 默认卡排在前面
            assertEquals(0, result.get(1).getIsDefault());
        }

        @Test
        @DisplayName("获取默认银行卡 - 有默认卡时直接返回")
        void getDefaultBankAccount_HasDefault_ReturnsDefault() {
            // Given
            when(bankAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(existingDefaultBankAccount);

            // When
            BankAccount result = disbursementService.getDefaultBankAccount(1L);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getIsDefault());
            assertEquals("中国建设银行", result.getBankName());
            verify(bankAccountMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("获取默认银行卡 - 无默认卡时回退到第一张活跃卡")
        void getDefaultBankAccount_NoDefault_FallsBackToFirstActive() {
            // Given - 第一次查询返回null（无默认卡），第二次返回第一张活跃卡
            when(bankAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(null)
                    .thenReturn(nonDefaultBankAccount);

            // When
            BankAccount result = disbursementService.getDefaultBankAccount(1L);

            // Then
            assertNotNull(result);
            assertEquals(2L, result.getId());
            assertEquals("中国农业银行", result.getBankName());
            verify(bankAccountMapper, times(2)).selectOne(any(LambdaQueryWrapper.class));
        }
    }

    // ==================== 银行卡解绑测试 ====================

    @Nested
    @DisplayName("银行卡解绑测试")
    class UnbindBankAccountTests {

        @Test
        @DisplayName("解绑非默认银行卡 - 成功设置状态为禁用")
        void unbindBankAccount_NonDefault_Success() {
            // Given
            when(bankAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(nonDefaultBankAccount);
            when(bankAccountMapper.updateById(any(BankAccount.class))).thenReturn(1);

            // When
            disbursementService.unbindBankAccount(1L, 2L);

            // Then - 仅调用一次 updateById（解绑当前卡，无需设置下一张默认）
            verify(bankAccountMapper, times(1)).updateById(argThat(account -> {
                assertEquals(0, account.getStatus());
                assertNotNull(account.getUpdatedAt());
                return true;
            }));
        }

        @Test
        @DisplayName("解绑不存在的银行卡 - 抛出异常")
        void unbindBankAccount_NotFound_ThrowsException() {
            // Given
            when(bankAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> disbursementService.unbindBankAccount(1L, 999L));

            assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("银行卡不存在"));
        }

        @Test
        @DisplayName("解绑默认银行卡 - 自动设置下一张活跃卡为默认")
        void unbindBankAccount_DefaultCard_SetsNextAsDefault() {
            // Given
            BankAccount nextCard = new BankAccount();
            nextCard.setId(3L);
            nextCard.setUserId(1L);
            nextCard.setIsDefault(0);
            nextCard.setStatus(1);

            when(bankAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(existingDefaultBankAccount) // 查找要解绑的卡
                    .thenReturn(nextCard);                  // 查找下一张活跃卡
            when(bankAccountMapper.updateById(any(BankAccount.class))).thenReturn(1);

            // When
            disbursementService.unbindBankAccount(1L, 1L);

            // Then - updateById 被调用两次：一次解绑当前卡，一次设置下一张默认
            verify(bankAccountMapper, times(2)).updateById(any(BankAccount.class));
            verify(bankAccountMapper).updateById(argThat(account ->
                    account.getId() == 3L && account.getIsDefault() == 1
            ));
        }
    }

    // ==================== 放款记录创建测试 ====================

    @Nested
    @DisplayName("放款记录创建测试")
    class CreateDisbursementTests {

        @Test
        @DisplayName("创建放款记录 - 成功返回待放款状态")
        void createDisbursement_Success() {
            // Given
            when(loanContractMapper.selectById(100L)).thenReturn(loanContract);
            when(bankAccountMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(existingDefaultBankAccount);
            when(disbursementRecordMapper.insert(any(DisbursementRecord.class))).thenReturn(1);

            // When
            DisbursementRecord result = disbursementService.createDisbursement(
                    100L, 10L, 1L, new BigDecimal("10000"), 1L);

            // Then
            assertNotNull(result);
            assertEquals(DisbursementStatus.PENDING.getCode(), result.getStatus());
            assertEquals(new BigDecimal("10000"), result.getAmount());
            assertEquals(100L, result.getContractId());
            assertEquals(1L, result.getBankAccountId());
            verify(disbursementRecordMapper).insert(any(DisbursementRecord.class));
        }

        @Test
        @DisplayName("创建放款记录 - 合同不存在时抛出异常")
        void createDisbursement_ContractNotFound_ThrowsException() {
            // Given
            when(loanContractMapper.selectById(999L)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> disbursementService.createDisbursement(
                            999L, 10L, 1L, new BigDecimal("10000"), 1L));

            assertEquals(ErrorCode.CONTRACT_NOT_FOUND.getCode(), exception.getCode());
            verify(disbursementRecordMapper, never()).insert(any());
        }

        @Test
        @DisplayName("创建放款记录 - 银行卡不存在或已解绑时抛出异常")
        void createDisbursement_BankAccountNotFound_ThrowsException() {
            // Given
            when(loanContractMapper.selectById(100L)).thenReturn(loanContract);
            when(bankAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> disbursementService.createDisbursement(
                            100L, 10L, 1L, new BigDecimal("10000"), 999L));

            assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("银行卡不存在或已解绑"));
            verify(disbursementRecordMapper, never()).insert(any());
        }
    }

    // ==================== 放款执行测试 ====================

    @Nested
    @DisplayName("放款执行测试")
    class ExecuteDisbursementTests {

        @Test
        @DisplayName("执行放款 - 成功完成并更新合同状态为还款中")
        void executeDisbursement_Success() {
            // Given
            when(disbursementRecordMapper.selectById(1L)).thenReturn(pendingRecord);
            when(disbursementRecordMapper.updateById(any(DisbursementRecord.class))).thenReturn(1);
            when(loanContractMapper.selectById(100L)).thenReturn(loanContract);
            doNothing().when(notificationService).send(anyLong(), anyString(), anyString(),
                    anyString(), anyString(), anyString());

            // When
            disbursementService.executeDisbursement(1L);

            // Then - 验证最终更新为已完成状态
            verify(disbursementRecordMapper, atLeastOnce()).updateById(argThat(record ->
                    record.getStatus() == DisbursementStatus.COMPLETED.getCode()
                            && record.getTransactionNo() != null
                            && record.getCompletedAt() != null
            ));
            // 验证合同状态更新为还款中
            verify(loanContractMapper).updateById(argThat(contract -> {
                assertEquals(1, contract.getStatus());
                assertNotNull(contract.getDisbursedAt());
                return true;
            }));
            // 验证发送放款成功通知
            verify(notificationService).send(eq(1L), eq("USER"), eq("放款成功"),
                    anyString(), eq("LOAN"), eq("100"));
        }

        @Test
        @DisplayName("执行放款 - 记录不存在时抛出异常")
        void executeDisbursement_RecordNotFound_ThrowsException() {
            // Given
            when(disbursementRecordMapper.selectById(999L)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> disbursementService.executeDisbursement(999L));

            assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("放款记录不存在"));
        }

        @Test
        @DisplayName("执行放款 - 已完成状态不允许再次执行")
        void executeDisbursement_AlreadyCompleted_ThrowsException() {
            // Given
            pendingRecord.setStatus(DisbursementStatus.COMPLETED.getCode());
            when(disbursementRecordMapper.selectById(1L)).thenReturn(pendingRecord);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> disbursementService.executeDisbursement(1L));

            assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("当前放款记录状态不允许执行放款"));
        }
    }

    // ==================== 放款记录查询测试 ====================

    @Nested
    @DisplayName("放款记录查询测试")
    class GetDisbursementRecordsTests {

        @Test
        @DisplayName("获取放款记录 - 带状态过滤返回正确状态描述")
        void getDisbursementRecords_WithStatusFilter() {
            // Given
            DisbursementRecord record = new DisbursementRecord();
            record.setId(1L);
            record.setContractId(100L);
            record.setUserId(1L);
            record.setAmount(new BigDecimal("10000"));
            record.setBankAccountId(1L);
            record.setStatus(DisbursementStatus.COMPLETED.getCode());
            record.setCreatedAt(LocalDateTime.now());

            when(disbursementRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(record));
            when(bankAccountMapper.selectById(1L)).thenReturn(existingDefaultBankAccount);

            // When
            List<DisbursementVO> result = disbursementService.getDisbursementRecords(
                    DisbursementStatus.COMPLETED.getCode());

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("已放款", result.get(0).getStatusDesc());
            assertEquals(new BigDecimal("10000"), result.get(0).getAmount());
        }

        @Test
        @DisplayName("获取用户放款记录 - 按用户ID过滤返回待放款记录")
        void getUserDisbursements_Success() {
            // Given
            DisbursementRecord record = new DisbursementRecord();
            record.setId(1L);
            record.setContractId(100L);
            record.setUserId(1L);
            record.setAmount(new BigDecimal("5000"));
            record.setBankAccountId(1L);
            record.setStatus(DisbursementStatus.PENDING.getCode());
            record.setCreatedAt(LocalDateTime.now());

            when(disbursementRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(record));
            when(bankAccountMapper.selectById(1L)).thenReturn(existingDefaultBankAccount);

            // When
            List<DisbursementVO> result = disbursementService.getUserDisbursements(1L);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).getUserId());
            assertEquals("待放款", result.get(0).getStatusDesc());
        }
    }
}
