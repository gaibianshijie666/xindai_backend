package com.xindai.xindai.modules.enterprise.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.enterprise.dto.*;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseCustomer;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 企业客户服务接口
 */
public interface EnterpriseCustomerService {

    /**
     * 获取客户列表
     */
    Page<EnterpriseCustomerVO> list(Long enterpriseId, EnterpriseCustomerQueryDTO queryDTO);

    /**
     * 创建客户
     */
    EnterpriseCustomerVO create(Long enterpriseId, EnterpriseCustomerDTO dto);

    /**
     * 更新客户
     */
    EnterpriseCustomerVO update(Long enterpriseId, Long customerId, EnterpriseCustomerDTO dto);

    /**
     * 删除客户
     */
    void delete(Long enterpriseId, Long customerId);

    /**
     * 根据企业ID和客户ID获取客户
     */
    EnterpriseCustomer getByEnterpriseAndId(Long enterpriseId, Long customerId);

    /**
     * 批量导入客户
     */
    void batchImport(Long enterpriseId, List<EnterpriseCustomerDTO> customers);

    /**
     * 批量删除客户
     */
    BatchOperationResultVO batchDelete(Long enterpriseId, List<Long> ids);

    /**
     * 批量更新客户状态
     */
    BatchOperationResultVO batchUpdateStatus(Long enterpriseId, BatchUpdateStatusDTO dto);

    /**
     * 导出客户Excel
     */
    void export(Long enterpriseId, List<Long> ids, HttpServletResponse response);
}
