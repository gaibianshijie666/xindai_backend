package com.xindai.xindai.modules.workflow.service;

import com.xindai.xindai.modules.workflow.dto.TaskVO;

import java.util.List;
import java.util.Map;

/**
 * 工作流服务接口
 */
public interface WorkflowService {

    /**
     * 启动流程
     *
     * @param processKey  流程定义key
     * @param businessKey 业务主键
     * @param variables   流程变量
     * @return 流程实例ID
     */
    String startProcess(String processKey, String businessKey, Map<String, Object> variables);

    /**
     * 获取指定用户的待办任务
     *
     * @param assignee 处理人
     * @return 待办任务列表
     */
    List<TaskVO> getPendingTasks(String assignee);

    /**
     * 完成任务
     *
     * @param taskId    任务ID
     * @param variables 流程变量
     */
    void completeTask(String taskId, Map<String, Object> variables);

    /**
     * 获取任务详情
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    TaskVO getTaskDetail(String taskId);

    /**
     * 获取流程状态
     *
     * @param processInstanceId 流程实例ID
     * @return 流程状态（running / completed / terminated）
     */
    String getProcessStatus(String processInstanceId);
}
