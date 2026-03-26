package com.xindai.xindai.modules.collection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.collection.dto.*;
import com.xindai.xindai.modules.collection.entity.CollectionRecord;
import com.xindai.xindai.modules.collection.entity.CollectionTask;
import com.xindai.xindai.modules.collection.enums.CollectionTaskStatus;
import com.xindai.xindai.modules.collection.mapper.CollectionRecordMapper;
import com.xindai.xindai.modules.collection.mapper.CollectionTaskMapper;
import com.xindai.xindai.modules.collection.service.CollectionTaskService;
import com.xindai.xindai.modules.loan.entity.LoanContract;
import com.xindai.xindai.modules.loan.mapper.LoanContractMapper;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 催收任务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionTaskServiceImpl implements CollectionTaskService {

    private final CollectionTaskMapper collectionTaskMapper;
    private final CollectionRecordMapper collectionRecordMapper;
    private final LoanContractMapper loanContractMapper;
    private final UserMapper userMapper;

    @Override
    public Page<CollectionTaskVO> getTaskList(CollectionTaskQueryDTO queryDTO) {
        Page<CollectionTask> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        LambdaQueryWrapper<CollectionTask> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getStatus() != null) {
            wrapper.eq(CollectionTask::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getPriority() != null) {
            wrapper.eq(CollectionTask::getPriority, queryDTO.getPriority());
        }
        if (queryDTO.getCollectorId() != null) {
            wrapper.eq(CollectionTask::getCollectorId, queryDTO.getCollectorId());
        }
        wrapper.orderByDesc(CollectionTask::getPriority);
        wrapper.orderByAsc(CollectionTask::getCreatedAt);

        Page<CollectionTask> taskPage = collectionTaskMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<CollectionTaskVO> voPage = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        List<CollectionTaskVO> voList = taskPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public CollectionTaskVO getTaskDetail(Long taskId) {
        CollectionTask task = collectionTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "催收任务不存在");
        }
        return convertToVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectionTaskVO assignTask(Long taskId, Long collectorId) {
        CollectionTask task = getTaskOrThrow(taskId);

        if (task.getStatus() != CollectionTaskStatus.PENDING.getCode()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有待分配的任务才能分配");
        }

        task.setCollectorId(collectorId);
        task.setStatus(CollectionTaskStatus.ASSIGNED.getCode());
        collectionTaskMapper.updateById(task);

        log.info("Collection task assigned: taskId={}, collectorId={}", taskId, collectorId);
        return convertToVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectionTaskVO startTask(Long taskId) {
        CollectionTask task = getTaskOrThrow(taskId);

        if (task.getStatus() != CollectionTaskStatus.ASSIGNED.getCode()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有已分配的任务才能开始处理");
        }

        task.setStatus(CollectionTaskStatus.IN_PROGRESS.getCode());
        collectionTaskMapper.updateById(task);

        log.info("Collection task started: taskId={}", taskId);
        return convertToVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectionRecordVO addRecord(Long collectorId, CreateCollectionRecordDTO dto) {
        CollectionTask task = getTaskOrThrow(dto.getTaskId());

        if (task.getStatus() == CollectionTaskStatus.COMPLETED.getCode()
                || task.getStatus() == CollectionTaskStatus.CLOSED.getCode()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务已结束，无法添加催收记录");
        }

        CollectionRecord record = new CollectionRecord();
        record.setTaskId(dto.getTaskId());
        record.setCollectorId(collectorId);
        record.setMethod(dto.getMethod());
        record.setContent(dto.getContent());
        record.setResult(dto.getResult());
        record.setNextFollowUpDate(dto.getNextFollowUpDate());
        collectionRecordMapper.insert(record);

        // 如果任务还在已分配状态，自动变为处理中
        if (task.getStatus() == CollectionTaskStatus.ASSIGNED.getCode()) {
            task.setStatus(CollectionTaskStatus.IN_PROGRESS.getCode());
            collectionTaskMapper.updateById(task);
        }

        log.info("Collection record added: taskId={}, method={}", dto.getTaskId(), dto.getMethod());
        return convertRecordToVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectionTaskVO completeTask(Long taskId) {
        CollectionTask task = getTaskOrThrow(taskId);

        if (task.getStatus() != CollectionTaskStatus.IN_PROGRESS.getCode()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有处理中的任务才能完成");
        }

        task.setStatus(CollectionTaskStatus.COMPLETED.getCode());
        collectionTaskMapper.updateById(task);

        log.info("Collection task completed: taskId={}", taskId);
        return convertToVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectionTaskVO closeTask(Long taskId) {
        CollectionTask task = getTaskOrThrow(taskId);

        if (task.getStatus() == CollectionTaskStatus.CLOSED.getCode()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务已关闭");
        }

        task.setStatus(CollectionTaskStatus.CLOSED.getCode());
        collectionTaskMapper.updateById(task);

        log.info("Collection task closed: taskId={}", taskId);
        return convertToVO(task);
    }

    @Override
    public List<CollectionRecordVO> getRecords(Long taskId) {
        CollectionTask task = collectionTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "催收任务不存在");
        }

        List<CollectionRecord> records = collectionRecordMapper.selectList(
                new LambdaQueryWrapper<CollectionRecord>()
                        .eq(CollectionRecord::getTaskId, taskId)
                        .orderByDesc(CollectionRecord::getCreatedAt)
        );

        return records.stream()
                .map(this::convertRecordToVO)
                .collect(Collectors.toList());
    }

    private CollectionTask getTaskOrThrow(Long taskId) {
        CollectionTask task = collectionTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "催收任务不存在");
        }
        return task;
    }

    private CollectionTaskVO convertToVO(CollectionTask task) {
        CollectionTaskVO vo = new CollectionTaskVO();
        vo.setId(task.getId());
        vo.setContractId(task.getContractId());
        vo.setUserId(task.getUserId());
        vo.setOverdueAmount(task.getOverdueAmount());
        vo.setOverdueDays(task.getOverdueDays());
        vo.setCollectorId(task.getCollectorId());
        vo.setStatus(task.getStatus());
        vo.setPriority(task.getPriority());
        vo.setDeadline(task.getDeadline());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());

        // 填充合同编号
        LoanContract contract = loanContractMapper.selectById(task.getContractId());
        if (contract != null) {
            vo.setContractNo(contract.getContractNo());
        }

        // 填充借款人信息
        User user = userMapper.selectById(task.getUserId());
        if (user != null) {
            vo.setUserName(user.getRealName());
            vo.setUserPhone(user.getPhone());
        }

        // 填充催收员姓名
        if (task.getCollectorId() != null) {
            User collector = userMapper.selectById(task.getCollectorId());
            if (collector != null) {
                vo.setCollectorName(collector.getRealName());
            }
        }

        // 填充状态描述
        CollectionTaskStatus statusEnum = CollectionTaskStatus.fromCode(task.getStatus());
        vo.setStatusDesc(statusEnum.getDesc());

        // 填充催收记录数
        Long recordCount = collectionRecordMapper.selectCount(
                new LambdaQueryWrapper<CollectionRecord>()
                        .eq(CollectionRecord::getTaskId, task.getId())
        );
        vo.setRecordCount(recordCount.intValue());

        return vo;
    }

    private CollectionRecordVO convertRecordToVO(CollectionRecord record) {
        CollectionRecordVO vo = new CollectionRecordVO();
        vo.setId(record.getId());
        vo.setTaskId(record.getTaskId());
        vo.setCollectorId(record.getCollectorId());
        vo.setMethod(record.getMethod());
        vo.setContent(record.getContent());
        vo.setResult(record.getResult());
        vo.setNextFollowUpDate(record.getNextFollowUpDate());
        vo.setCreatedAt(record.getCreatedAt());

        // 填充催收员姓名
        User collector = userMapper.selectById(record.getCollectorId());
        if (collector != null) {
            vo.setCollectorName(collector.getRealName());
        }

        // 填充方式描述
        vo.setMethodDesc(getMethodDesc(record.getMethod()));

        // 填充结果描述
        vo.setResultDesc(getResultDesc(record.getResult()));

        return vo;
    }

    private String getMethodDesc(String method) {
        if (method == null) return null;
        return switch (method) {
            case "phone" -> "电话催收";
            case "sms" -> "短信催收";
            case "visit" -> "上门催收";
            case "legal" -> "法律催收";
            default -> method;
        };
    }

    private String getResultDesc(String result) {
        if (result == null) return null;
        return switch (result) {
            case "promise_pay" -> "承诺还款";
            case "refused" -> "拒绝还款";
            case "unreachable" -> "无法联系";
            case "other" -> "其他";
            default -> result;
        };
    }
}
