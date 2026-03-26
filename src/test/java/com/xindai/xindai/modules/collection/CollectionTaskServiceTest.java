package com.xindai.xindai.modules.collection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.collection.dto.CollectionRecordVO;
import com.xindai.xindai.modules.collection.dto.CollectionTaskQueryDTO;
import com.xindai.xindai.modules.collection.dto.CollectionTaskVO;
import com.xindai.xindai.modules.collection.dto.CreateCollectionRecordDTO;
import com.xindai.xindai.modules.collection.entity.CollectionRecord;
import com.xindai.xindai.modules.collection.entity.CollectionTask;
import com.xindai.xindai.modules.collection.enums.CollectionTaskStatus;
import com.xindai.xindai.modules.collection.mapper.CollectionRecordMapper;
import com.xindai.xindai.modules.collection.mapper.CollectionTaskMapper;
import com.xindai.xindai.modules.collection.service.impl.CollectionTaskServiceImpl;
import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.mapper.LoanContractMapper;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollectionTaskServiceImpl 单元测试")
class CollectionTaskServiceTest {

    @Mock
    private CollectionTaskMapper collectionTaskMapper;

    @Mock
    private CollectionRecordMapper collectionRecordMapper;

    @Mock
    private LoanContractMapper loanContractMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CollectionTaskServiceImpl collectionTaskService;

    private CollectionTask testTask;
    private User testUser;
    private User testCollector;
    private LoanContract testContract;

    @BeforeEach
    void setUp() {
        testTask = new CollectionTask();
        testTask.setId(1L);
        testTask.setContractId(10L);
        testTask.setUserId(100L);
        testTask.setOverdueAmount(new BigDecimal("5000.00"));
        testTask.setOverdueDays(30);
        testTask.setStatus(CollectionTaskStatus.PENDING.getCode());
        testTask.setPriority(2);
        testTask.setDeadline(LocalDateTime.of(2026, 4, 15, 0, 0));
        testTask.setCreatedAt(LocalDateTime.of(2026, 3, 20, 10, 0));
        testTask.setUpdatedAt(LocalDateTime.of(2026, 3, 20, 10, 0));

        testUser = new User();
        testUser.setId(100L);
        testUser.setRealName("张三");
        testUser.setPhone("13800138000");

        testCollector = new User();
        testCollector.setId(200L);
        testCollector.setRealName("李四");

        testContract = new LoanContract();
        testContract.setId(10L);
        testContract.setContractNo("LC20260320001");
    }

    @Nested
    @DisplayName("获取任务列表测试")
    class GetTaskListTests {

        @Test
        @DisplayName("带状态过滤条件查询 - 返回分页VO结果")
        void getTaskList_WithStatusFilter_ReturnsPagedVO() {
            // Given
            CollectionTaskQueryDTO queryDTO = new CollectionTaskQueryDTO();
            queryDTO.setPage(1);
            queryDTO.setSize(10);
            queryDTO.setStatus(CollectionTaskStatus.PENDING.getCode());

            Page<CollectionTask> taskPage = new Page<>(1, 10, 1);
            taskPage.setRecords(List.of(testTask));

            when(collectionTaskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(taskPage);
            when(loanContractMapper.selectById(10L)).thenReturn(testContract);
            when(userMapper.selectById(100L)).thenReturn(testUser);
            when(collectionRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // When
            Page<CollectionTaskVO> result = collectionTaskService.getTaskList(queryDTO);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getRecords().size());
            assertEquals(1, result.getTotal());
            CollectionTaskVO vo = result.getRecords().get(0);
            assertEquals("张三", vo.getUserName());
            assertEquals("LC20260320001", vo.getContractNo());
            assertEquals(new BigDecimal("5000.00"), vo.getOverdueAmount());
            assertEquals(30, vo.getOverdueDays());
        }

        @Test
        @DisplayName("无过滤条件查询 - 返回所有任务")
        void getTaskList_NoFilters_ReturnsAllTasks() {
            // Given
            CollectionTaskQueryDTO queryDTO = new CollectionTaskQueryDTO();
            queryDTO.setPage(1);
            queryDTO.setSize(10);

            Page<CollectionTask> taskPage = new Page<>(1, 10, 2);
            taskPage.setRecords(List.of(testTask, testTask));

            when(collectionTaskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(taskPage);
            when(loanContractMapper.selectById(anyLong())).thenReturn(testContract);
            when(userMapper.selectById(100L)).thenReturn(testUser);
            when(collectionRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // When
            Page<CollectionTaskVO> result = collectionTaskService.getTaskList(queryDTO);

            // Then
            assertNotNull(result);
            assertEquals(2, result.getRecords().size());
            assertEquals(2, result.getTotal());
        }

        @Test
        @DisplayName("空列表查询 - 返回空结果")
        void getTaskList_EmptyResult_ReturnsEmptyPage() {
            // Given
            CollectionTaskQueryDTO queryDTO = new CollectionTaskQueryDTO();
            queryDTO.setPage(1);
            queryDTO.setSize(10);

            Page<CollectionTask> taskPage = new Page<>(1, 10, 0);
            taskPage.setRecords(Collections.emptyList());

            when(collectionTaskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(taskPage);

            // When
            Page<CollectionTaskVO> result = collectionTaskService.getTaskList(queryDTO);

            // Then
            assertNotNull(result);
            assertTrue(result.getRecords().isEmpty());
            assertEquals(0, result.getTotal());
        }
    }

    @Nested
    @DisplayName("获取任务详情测试")
    class GetTaskDetailTests {

        @Test
        @DisplayName("任务存在 - 返回完整详情")
        void getTaskDetail_Found_ReturnsDetail() {
            // Given
            when(collectionTaskMapper.selectById(1L)).thenReturn(testTask);
            when(loanContractMapper.selectById(10L)).thenReturn(testContract);
            when(userMapper.selectById(100L)).thenReturn(testUser);
            when(collectionRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

            // When
            CollectionTaskVO result = collectionTaskService.getTaskDetail(1L);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("张三", result.getUserName());
            assertEquals("13800138000", result.getUserPhone());
            assertEquals("LC20260320001", result.getContractNo());
            assertEquals(new BigDecimal("5000.00"), result.getOverdueAmount());
            assertEquals("待分配", result.getStatusDesc());
            assertEquals(3, result.getRecordCount());
        }

        @Test
        @DisplayName("任务不存在 - 抛出NOT_FOUND异常")
        void getTaskDetail_NotFound_ThrowsException() {
            // Given
            when(collectionTaskMapper.selectById(999L)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> collectionTaskService.getTaskDetail(999L));

            assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("分配任务测试")
    class AssignTaskTests {

        @Test
        @DisplayName("分配待分配任务 - 成功")
        void assignTask_Pending_Success() {
            // Given
            when(collectionTaskMapper.selectById(1L)).thenReturn(testTask);
            when(collectionTaskMapper.updateById(any(CollectionTask.class))).thenReturn(1);
            when(loanContractMapper.selectById(10L)).thenReturn(testContract);
            when(userMapper.selectById(100L)).thenReturn(testUser);
            when(userMapper.selectById(200L)).thenReturn(testCollector);
            when(collectionRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // When
            CollectionTaskVO result = collectionTaskService.assignTask(1L, 200L);

            // Then
            assertNotNull(result);
            assertEquals(200L, result.getCollectorId());
            assertEquals(CollectionTaskStatus.ASSIGNED.getCode(), result.getStatus());
            verify(collectionTaskMapper).updateById(any(CollectionTask.class));
        }

        @Test
        @DisplayName("分配非待分配任务 - 抛出BAD_REQUEST异常")
        void assignTask_NonPending_ThrowsException() {
            // Given
            testTask.setStatus(CollectionTaskStatus.IN_PROGRESS.getCode());
            when(collectionTaskMapper.selectById(1L)).thenReturn(testTask);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> collectionTaskService.assignTask(1L, 200L));

            assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode());
            verify(collectionTaskMapper, never()).updateById(any());
        }
    }

    @Nested
    @DisplayName("开始任务测试")
    class StartTaskTests {

        @Test
        @DisplayName("开始已分配任务 - 成功")
        void startTask_Assigned_Success() {
            // Given
            testTask.setStatus(CollectionTaskStatus.ASSIGNED.getCode());
            testTask.setCollectorId(200L);
            when(collectionTaskMapper.selectById(1L)).thenReturn(testTask);
            when(collectionTaskMapper.updateById(any(CollectionTask.class))).thenReturn(1);
            when(loanContractMapper.selectById(10L)).thenReturn(testContract);
            when(userMapper.selectById(100L)).thenReturn(testUser);
            when(userMapper.selectById(200L)).thenReturn(testCollector);
            when(collectionRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // When
            CollectionTaskVO result = collectionTaskService.startTask(1L);

            // Then
            assertNotNull(result);
            assertEquals(CollectionTaskStatus.IN_PROGRESS.getCode(), result.getStatus());
            verify(collectionTaskMapper).updateById(any(CollectionTask.class));
        }

        @Test
        @DisplayName("开始待分配任务 - 抛出BAD_REQUEST异常")
        void startTask_Pending_ThrowsException() {
            // Given
            when(collectionTaskMapper.selectById(1L)).thenReturn(testTask);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> collectionTaskService.startTask(1L));

            assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode());
            verify(collectionTaskMapper, never()).updateById(any());
        }
    }

    @Nested
    @DisplayName("添加催收记录测试")
    class AddRecordTests {

        @Test
        @DisplayName("向处理中的任务添加记录 - 成功")
        void addRecord_InProgress_Success() {
            // Given
            testTask.setStatus(CollectionTaskStatus.IN_PROGRESS.getCode());
            CreateCollectionRecordDTO dto = new CreateCollectionRecordDTO();
            dto.setTaskId(1L);
            dto.setMethod("phone");
            dto.setContent("电话沟通还款事宜");
            dto.setResult("promise_pay");
            dto.setNextFollowUpDate(LocalDate.of(2026, 3, 30));

            when(collectionTaskMapper.selectById(1L)).thenReturn(testTask);
            when(collectionRecordMapper.insert(any(CollectionRecord.class))).thenReturn(1);
            when(userMapper.selectById(200L)).thenReturn(testCollector);

            // When
            CollectionRecordVO result = collectionTaskService.addRecord(200L, dto);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getTaskId());
            assertEquals("phone", result.getMethod());
            assertEquals("电话催收", result.getMethodDesc());
            assertEquals("promise_pay", result.getResult());
            assertEquals("承诺还款", result.getResultDesc());
            assertEquals("李四", result.getCollectorName());
            verify(collectionRecordMapper).insert(any(CollectionRecord.class));
        }

        @Test
        @DisplayName("向已分配任务添加记录 - 自动转为处理中")
        void addRecord_Assigned_AutoTransitionsToInProgress() {
            // Given
            testTask.setStatus(CollectionTaskStatus.ASSIGNED.getCode());
            CreateCollectionRecordDTO dto = new CreateCollectionRecordDTO();
            dto.setTaskId(1L);
            dto.setMethod("sms");
            dto.setContent("发送短信提醒");

            when(collectionTaskMapper.selectById(1L)).thenReturn(testTask);
            when(collectionRecordMapper.insert(any(CollectionRecord.class))).thenReturn(1);
            when(collectionTaskMapper.updateById(any(CollectionTask.class))).thenReturn(1);
            when(userMapper.selectById(200L)).thenReturn(testCollector);

            // When
            collectionTaskService.addRecord(200L, dto);

            // Then
            verify(collectionTaskMapper).updateById(argThat(task ->
                    task.getStatus().equals(CollectionTaskStatus.IN_PROGRESS.getCode())
            ));
        }

        @Test
        @DisplayName("向已完成任务添加记录 - 抛出BAD_REQUEST异常")
        void addRecord_Completed_ThrowsException() {
            // Given
            testTask.setStatus(CollectionTaskStatus.COMPLETED.getCode());
            CreateCollectionRecordDTO dto = new CreateCollectionRecordDTO();
            dto.setTaskId(1L);
            dto.setMethod("phone");

            when(collectionTaskMapper.selectById(1L)).thenReturn(testTask);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> collectionTaskService.addRecord(200L, dto));

            assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode());
            verify(collectionRecordMapper, never()).insert(any());
        }

        @Test
        @DisplayName("任务不存在时添加记录 - 抛出NOT_FOUND异常")
        void addRecord_TaskNotFound_ThrowsException() {
            // Given
            CreateCollectionRecordDTO dto = new CreateCollectionRecordDTO();
            dto.setTaskId(999L);
            dto.setMethod("phone");

            when(collectionTaskMapper.selectById(999L)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> collectionTaskService.addRecord(200L, dto));

            assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("完成任务测试")
    class CompleteTaskTests {

        @Test
        @DisplayName("完成处理中的任务 - 成功")
        void completeTask_InProgress_Success() {
            // Given
            testTask.setStatus(CollectionTaskStatus.IN_PROGRESS.getCode());
            testTask.setCollectorId(200L);
            when(collectionTaskMapper.selectById(1L)).thenReturn(testTask);
            when(collectionTaskMapper.updateById(any(CollectionTask.class))).thenReturn(1);
            when(loanContractMapper.selectById(10L)).thenReturn(testContract);
            when(userMapper.selectById(100L)).thenReturn(testUser);
            when(userMapper.selectById(200L)).thenReturn(testCollector);
            when(collectionRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // When
            CollectionTaskVO result = collectionTaskService.completeTask(1L);

            // Then
            assertNotNull(result);
            assertEquals(CollectionTaskStatus.COMPLETED.getCode(), result.getStatus());
            assertEquals("已完成", result.getStatusDesc());
            verify(collectionTaskMapper).updateById(any(CollectionTask.class));
        }

        @Test
        @DisplayName("完成待分配任务 - 抛出BAD_REQUEST异常")
        void completeTask_Pending_ThrowsException() {
            // Given
            when(collectionTaskMapper.selectById(1L)).thenReturn(testTask);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> collectionTaskService.completeTask(1L));

            assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode());
            verify(collectionTaskMapper, never()).updateById(any());
        }
    }

    @Nested
    @DisplayName("关闭任务测试")
    class CloseTaskTests {

        @Test
        @DisplayName("关闭处理中的任务 - 成功")
        void closeTask_InProgress_Success() {
            // Given
            testTask.setStatus(CollectionTaskStatus.IN_PROGRESS.getCode());
            testTask.setCollectorId(200L);
            when(collectionTaskMapper.selectById(1L)).thenReturn(testTask);
            when(collectionTaskMapper.updateById(any(CollectionTask.class))).thenReturn(1);
            when(loanContractMapper.selectById(10L)).thenReturn(testContract);
            when(userMapper.selectById(100L)).thenReturn(testUser);
            when(userMapper.selectById(200L)).thenReturn(testCollector);
            when(collectionRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // When
            CollectionTaskVO result = collectionTaskService.closeTask(1L);

            // Then
            assertNotNull(result);
            assertEquals(CollectionTaskStatus.CLOSED.getCode(), result.getStatus());
            assertEquals("已关闭", result.getStatusDesc());
            verify(collectionTaskMapper).updateById(any(CollectionTask.class));
        }

        @Test
        @DisplayName("关闭已关闭的任务 - 抛出BAD_REQUEST异常")
        void closeTask_AlreadyClosed_ThrowsException() {
            // Given
            testTask.setStatus(CollectionTaskStatus.CLOSED.getCode());
            when(collectionTaskMapper.selectById(1L)).thenReturn(testTask);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> collectionTaskService.closeTask(1L));

            assertEquals(ErrorCode.BAD_REQUEST.getCode(), exception.getCode());
            verify(collectionTaskMapper, never()).updateById(any());
        }
    }

    @Nested
    @DisplayName("获取催收记录列表测试")
    class GetRecordsTests {

        @Test
        @DisplayName("获取任务的催收记录 - 成功")
        void getRecords_Success() {
            // Given
            CollectionRecord record = new CollectionRecord();
            record.setId(1L);
            record.setTaskId(1L);
            record.setCollectorId(200L);
            record.setMethod("phone");
            record.setContent("电话催收");
            record.setResult("promise_pay");
            record.setCreatedAt(LocalDateTime.of(2026, 3, 25, 14, 0));

            when(collectionTaskMapper.selectById(1L)).thenReturn(testTask);
            when(collectionRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(record));
            when(userMapper.selectById(200L)).thenReturn(testCollector);

            // When
            List<CollectionRecordVO> result = collectionTaskService.getRecords(1L);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            CollectionRecordVO vo = result.get(0);
            assertEquals("phone", vo.getMethod());
            assertEquals("电话催收", vo.getMethodDesc());
            assertEquals("promise_pay", vo.getResult());
            assertEquals("承诺还款", vo.getResultDesc());
            assertEquals("李四", vo.getCollectorName());
        }

        @Test
        @DisplayName("任务不存在时获取记录 - 抛出NOT_FOUND异常")
        void getRecords_TaskNotFound_ThrowsException() {
            // Given
            when(collectionTaskMapper.selectById(999L)).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> collectionTaskService.getRecords(999L));

            assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
        }
    }
}
