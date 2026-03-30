package com.xindai.xindai.modules.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.admin.dto.CreditLimitAdjustDTO;
import com.xindai.xindai.modules.admin.dto.EnterpriseQueryDTO;
import com.xindai.xindai.modules.admin.dto.EnterpriseStatusDTO;
import com.xindai.xindai.modules.admin.vo.AdminEnterpriseDetailVO;
import com.xindai.xindai.modules.admin.vo.AdminEnterpriseVO;

/**
 * 管理端企业管理服务接口
 */
public interface AdminEnterpriseService {

    /**
     * 获取企业列表
     */
    Page<AdminEnterpriseVO> getEnterpriseList(EnterpriseQueryDTO queryDTO);

    /**
     * 获取企业详情
     */
    AdminEnterpriseDetailVO getEnterpriseDetail(Long id);

    /**
     * 更新企业状态
     */
    void updateEnterpriseStatus(Long id, EnterpriseStatusDTO statusDTO);

    /**
     * 调整企业授信额度
     */
    void adjustCreditLimit(Long id, CreditLimitAdjustDTO adjustDTO);
}
