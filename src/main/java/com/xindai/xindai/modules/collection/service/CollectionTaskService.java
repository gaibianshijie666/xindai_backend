package com.xindai.xindai.modules.collection.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.collection.dto.*;

import java.util.List;

/**
 * 催收任务服务接口
 */
public interface CollectionTaskService {

    /**
     * 获取催收任务列表（分页）
     */
    Page<CollectionTaskVO> getTaskList(CollectionTaskQueryDTO queryDTO);

    /**
     * 获取催收任务详情
     */
    CollectionTaskVO getTaskDetail(Long taskId);

    /**
     * 分配催收任务
     */
    CollectionTaskVO assignTask(Long taskId, Long collectorId);

    /**
     * 开始处理催收任务
     */
    CollectionTaskVO startTask(Long taskId);

    /**
     * 添加催收记录
     */
    CollectionRecordVO addRecord(Long collectorId, CreateCollectionRecordDTO dto);

    /**
     * 完成催收任务
     */
    CollectionTaskVO completeTask(Long taskId);

    /**
     * 关闭催收任务
     */
    CollectionTaskVO closeTask(Long taskId);

    /**
     * 获取催收记录列表
     */
    List<CollectionRecordVO> getRecords(Long taskId);
}
