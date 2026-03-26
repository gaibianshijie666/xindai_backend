package com.xindai.xindai.modules.workflow;

import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.workflow.dto.TaskVO;
import com.xindai.xindai.modules.workflow.service.impl.WorkflowServiceImpl;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WorkflowService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WorkflowService 单元测试")
class WorkflowServiceTest {

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private TaskService taskService;

    @Mock
    private HistoryService historyService;

    @Mock
    private ProcessInstance processInstance;

    @Mock
    private Task task;

    @Mock
    private TaskQuery taskQuery;

    @Mock
    private ProcessInstanceQuery processInstanceQuery;

    @Mock
    private HistoricProcessInstance historicProcessInstance;

    @Mock
    private HistoricProcessInstanceQuery historicProcessInstanceQuery;

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    private static final String PROCESS_KEY = "loan_approval";
    private static final String BUSINESS_KEY = "APP-2026-001";
    private static final String PROCESS_INSTANCE_ID = "proc-inst-12345";
    private static final String TASK_ID = "task-67890";
    private static final String ASSIGNEE = "admin01";

    @BeforeEach
    void setUp() {
        when(processInstance.getId()).thenReturn(PROCESS_INSTANCE_ID);
        when(processInstance.getBusinessKey()).thenReturn(BUSINESS_KEY);

        when(task.getId()).thenReturn(TASK_ID);
        when(task.getName()).thenReturn("审批贷款申请");
        when(task.getAssignee()).thenReturn(ASSIGNEE);
        when(task.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
        when(task.getCreateTime()).thenReturn(new Date());
    }

    @Nested
    @DisplayName("启动流程测试")
    class StartProcessTests {

        @Test
        @DisplayName("启动流程 - 成功返回流程实例ID")
        void startProcess_success_returnsInstanceId() {
            // Given
            Map<String, Object> variables = Map.of("applicantId", 1L, "amount", 50000);
            when(runtimeService.startProcessInstanceByKey(PROCESS_KEY, BUSINESS_KEY, variables))
                    .thenReturn(processInstance);

            // When
            String result = workflowService.startProcess(PROCESS_KEY, BUSINESS_KEY, variables);

            // Then
            assertNotNull(result);
            assertEquals(PROCESS_INSTANCE_ID, result);
            verify(runtimeService).startProcessInstanceByKey(PROCESS_KEY, BUSINESS_KEY, variables);
        }
    }

    @Nested
    @DisplayName("待办任务查询测试")
    class GetPendingTasksTests {

        @Test
        @DisplayName("获取待办任务 - 返回任务列表")
        void getPendingTasks_returnsTaskList() {
            // Given
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskAssignee(ASSIGNEE)).thenReturn(taskQuery);
            when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
            when(taskQuery.desc()).thenReturn(taskQuery);
            when(taskQuery.list()).thenReturn(List.of(task));

            when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
            when(processInstanceQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(processInstanceQuery);
            when(processInstanceQuery.singleResult()).thenReturn(processInstance);
            when(taskService.getVariables(TASK_ID)).thenReturn(Map.of("amount", 50000));

            // When
            List<TaskVO> result = workflowService.getPendingTasks(ASSIGNEE);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(TASK_ID, result.get(0).getTaskId());
            assertEquals("审批贷款申请", result.get(0).getTaskName());
            assertEquals(ASSIGNEE, result.get(0).getAssignee());
            assertEquals(BUSINESS_KEY, result.get(0).getBusinessKey());
        }

        @Test
        @DisplayName("获取待办任务 - 无任务返回空列表")
        void getPendingTasks_noTasks_returnsEmptyList() {
            // Given
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskAssignee(ASSIGNEE)).thenReturn(taskQuery);
            when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
            when(taskQuery.desc()).thenReturn(taskQuery);
            when(taskQuery.list()).thenReturn(Collections.emptyList());

            // When
            List<TaskVO> result = workflowService.getPendingTasks(ASSIGNEE);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("完成任务测试")
    class CompleteTaskTests {

        @Test
        @DisplayName("完成任务 - 成功")
        void completeTask_success() {
            // Given
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskId(TASK_ID)).thenReturn(taskQuery);
            when(taskQuery.singleResult()).thenReturn(task);

            Map<String, Object> variables = Map.of("approved", true);

            // When
            workflowService.completeTask(TASK_ID, variables);

            // Then
            verify(taskService).setVariables(TASK_ID, variables);
            verify(taskService).complete(TASK_ID);
        }

        @Test
        @DisplayName("完成任务 - 任务不存在抛出异常")
        void completeTask_taskNotFound_throwsException() {
            // Given
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskId(TASK_ID)).thenReturn(taskQuery);
            when(taskQuery.singleResult()).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> workflowService.completeTask(TASK_ID, null));

            assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("任务详情查询测试")
    class GetTaskDetailTests {

        @Test
        @DisplayName("获取任务详情 - 任务不存在抛出异常")
        void getTaskDetail_taskNotFound_throwsException() {
            // Given
            when(taskService.createTaskQuery()).thenReturn(taskQuery);
            when(taskQuery.taskId(TASK_ID)).thenReturn(taskQuery);
            when(taskQuery.singleResult()).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> workflowService.getTaskDetail(TASK_ID));

            assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("流程状态查询测试")
    class GetProcessStatusTests {

        @Test
        @DisplayName("获取流程状态 - 运行中")
        void getProcessStatus_running() {
            // Given
            when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
            when(processInstanceQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(processInstanceQuery);
            when(processInstanceQuery.singleResult()).thenReturn(processInstance);

            // When
            String result = workflowService.getProcessStatus(PROCESS_INSTANCE_ID);

            // Then
            assertEquals("running", result);
        }

        @Test
        @DisplayName("获取流程状态 - 已完成")
        void getProcessStatus_completed() {
            // Given
            when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
            when(processInstanceQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(processInstanceQuery);
            when(processInstanceQuery.singleResult()).thenReturn(null);

            when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQuery);
            when(historicProcessInstanceQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(historicProcessInstanceQuery);
            when(historicProcessInstanceQuery.singleResult()).thenReturn(historicProcessInstance);
            when(historicProcessInstance.getEndTime()).thenReturn(new Date());

            // When
            String result = workflowService.getProcessStatus(PROCESS_INSTANCE_ID);

            // Then
            assertEquals("completed", result);
        }

        @Test
        @DisplayName("获取流程状态 - 流程实例不存在抛出异常")
        void getProcessStatus_notFound_throwsException() {
            // Given
            when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
            when(processInstanceQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(processInstanceQuery);
            when(processInstanceQuery.singleResult()).thenReturn(null);

            when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicProcessInstanceQuery);
            when(historicProcessInstanceQuery.processInstanceId(PROCESS_INSTANCE_ID)).thenReturn(historicProcessInstanceQuery);
            when(historicProcessInstanceQuery.singleResult()).thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> workflowService.getProcessStatus(PROCESS_INSTANCE_ID));

            assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
        }
    }
}
