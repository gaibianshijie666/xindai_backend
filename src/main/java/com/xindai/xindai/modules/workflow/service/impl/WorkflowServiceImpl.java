package com.xindai.xindai.modules.workflow.service.impl;

import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.workflow.dto.TaskVO;
import com.xindai.xindai.modules.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 工作流服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    @Override
    public String startProcess(String processKey, String businessKey, Map<String, Object> variables) {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(processKey, businessKey, variables);
        log.info("Started process: key={}, instanceId={}, businessKey={}",
                processKey, instance.getId(), businessKey);
        return instance.getId();
    }

    @Override
    public List<TaskVO> getPendingTasks(String assignee) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime().desc()
                .list();

        return tasks.stream().map(this::toTaskVO).toList();
    }

    @Override
    public void completeTask(String taskId, Map<String, Object> variables) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在：" + taskId);
        }

        if (variables != null) {
            taskService.setVariables(taskId, variables);
        }

        taskService.complete(taskId);
        log.info("Completed task: taskId={}, taskName={}, processInstanceId={}",
                taskId, task.getName(), task.getProcessInstanceId());
    }

    @Override
    public TaskVO getTaskDetail(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在：" + taskId);
        }

        return toTaskVO(task);
    }

    @Override
    public String getProcessStatus(String processInstanceId) {
        // 检查运行中的流程
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (instance != null) {
            return "running";
        }

        // 检查历史流程
        HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (historicInstance != null) {
            if (historicInstance.getEndTime() != null) {
                return "completed";
            }
            return "terminated";
        }

        throw new BusinessException(ErrorCode.NOT_FOUND, "流程实例不存在：" + processInstanceId);
    }

    private TaskVO toTaskVO(Task task) {
        TaskVO vo = new TaskVO();
        vo.setTaskId(task.getId());
        vo.setTaskName(task.getName());
        vo.setAssignee(task.getAssignee());
        vo.setCreateTime(task.getCreateTime() != null ? task.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
        vo.setProcessInstanceId(task.getProcessInstanceId());

        // 获取业务主键
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();
        if (instance != null) {
            vo.setBusinessKey(instance.getBusinessKey());
        }

        // 获取流程变量
        Map<String, Object> variables = taskService.getVariables(task.getId());
        vo.setVariables(variables);

        return vo;
    }
}
