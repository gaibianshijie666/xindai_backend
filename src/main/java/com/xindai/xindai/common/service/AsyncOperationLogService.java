package com.xindai.xindai.common.service;

import com.xindai.xindai.common.entity.OperationLog;
import com.xindai.xindai.common.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncOperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Async
    public void save(OperationLog operationLog) {
        try {
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.error("异步保存操作日志失败", e);
        }
    }
}
