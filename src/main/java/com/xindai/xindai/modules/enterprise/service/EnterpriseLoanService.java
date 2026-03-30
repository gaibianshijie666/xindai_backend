package com.xindai.xindai.modules.enterprise.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.enterprise.dto.*;

import java.util.List;

/**
 * 企业借款服务接口
 */
public interface EnterpriseLoanService {

    /**
     * 获取借款列表
     */
    Page<EnterpriseLoanVO> list(Long enterpriseId, EnterpriseLoanQueryDTO queryDTO);

    /**
     * 代客申请借款
     */
    EnterpriseLoanVO apply(Long enterpriseId, Long userId, EnterpriseLoanApplyDTO dto, String ipAddress);

    /**
     * 获取借款详情
     */
    EnterpriseLoanVO getById(Long enterpriseId, Long loanId);

    /**
     * 批量借款申请
     */
    BatchOperationResultVO batchApply(Long enterpriseId, Long userId, List<EnterpriseLoanApplyDTO> dtos, String ipAddress);

    /**
     * 批量审核借款
     */
    BatchOperationResultVO batchReview(Long enterpriseId, Long userId, BatchReviewDTO dto, String ipAddress);
}
