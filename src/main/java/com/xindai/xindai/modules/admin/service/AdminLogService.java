package com.xindai.xindai.modules.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.admin.dto.OperationLogQueryDTO;
import com.xindai.xindai.modules.admin.vo.OperationLogVO;

/**
 * Admin log service interface
 */
public interface AdminLogService {

    /**
     * Get operation logs with pagination and filters
     */
    Page<OperationLogVO> getOperationLogs(OperationLogQueryDTO query);

    /**
     * Get operation log detail by id
     */
    OperationLogVO getOperationLogDetail(Long id);
}
