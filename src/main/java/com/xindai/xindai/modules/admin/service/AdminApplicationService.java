package com.xindai.xindai.modules.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.admin.dto.ApplicationQueryDTO;
import com.xindai.xindai.modules.admin.dto.ApplicationReviewDTO;
import com.xindai.xindai.modules.admin.vo.AdminApplicationVO;

/**
 * 管理端借款申请服务接口
 */
public interface AdminApplicationService {

    /**
     * 获取借款申请列表
     */
    Page<AdminApplicationVO> getApplicationList(ApplicationQueryDTO queryDTO);

    /**
     * 审核借款申请
     */
    void reviewApplication(Long id, ApplicationReviewDTO reviewDTO);
}
