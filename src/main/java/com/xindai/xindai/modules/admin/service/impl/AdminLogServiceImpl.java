package com.xindai.xindai.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.entity.OperationLog;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.common.mapper.OperationLogMapper;
import com.xindai.xindai.modules.admin.dto.OperationLogQueryDTO;
import com.xindai.xindai.modules.admin.service.AdminLogService;
import com.xindai.xindai.modules.admin.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin log service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogServiceImpl implements AdminLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public Page<OperationLogVO> getOperationLogs(OperationLogQueryDTO query) {
        Page<OperationLog> page = new Page<>(query.getPage(), query.getSize());

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        if (query.getModule() != null && !query.getModule().isEmpty()) {
            wrapper.eq(OperationLog::getModule, query.getModule());
        }
        if (query.getOperation() != null && !query.getOperation().isEmpty()) {
            wrapper.eq(OperationLog::getOperation, query.getOperation());
        }
        if (query.getUsername() != null && !query.getUsername().isEmpty()) {
            wrapper.like(OperationLog::getUsername, query.getUsername());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(OperationLog::getCreatedAt, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(OperationLog::getCreatedAt, query.getEndDate());
        }

        wrapper.orderByDesc(OperationLog::getCreatedAt);

        Page<OperationLog> entityPage = operationLogMapper.selectPage(page, wrapper);

        Page<OperationLogVO> voPage = new Page<>(
                entityPage.getCurrent(),
                entityPage.getSize(),
                entityPage.getTotal()
        );

        List<OperationLogVO> voList = entityPage.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public OperationLogVO getOperationLogDetail(Long id) {
        OperationLog log = operationLogMapper.selectById(id);
        if (log == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "操作日志不存在");
        }
        return toVO(log);
    }

    private OperationLogVO toVO(OperationLog entity) {
        OperationLogVO vo = new OperationLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
