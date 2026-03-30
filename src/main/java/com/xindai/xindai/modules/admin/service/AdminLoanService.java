package com.xindai.xindai.modules.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.admin.dto.ContractAdjustDTO;
import com.xindai.xindai.modules.admin.dto.ContractQueryDTO;
import com.xindai.xindai.modules.admin.dto.DisbursementQueryDTO;
import com.xindai.xindai.modules.admin.dto.DisbursementRejectDTO;
import com.xindai.xindai.modules.admin.vo.AdminContractDetailVO;
import com.xindai.xindai.modules.admin.vo.AdminContractVO;
import com.xindai.xindai.modules.admin.vo.AdminDisbursementVO;

/**
 * 管理端贷款管理服务接口
 */
public interface AdminLoanService {

    /**
     * 获取借款合同列表
     */
    Page<AdminContractVO> getContractList(ContractQueryDTO queryDTO);

    /**
     * 获取借款合同详情
     */
    AdminContractDetailVO getContractDetail(Long id);

    /**
     * 取消合同
     * 仅待放款状态且无还款记录的合同可取消
     */
    void cancelContract(Long id);

    /**
     * 调整合同利率
     * 仅超级管理员可操作
     */
    void adjustContractRate(Long id, ContractAdjustDTO adjustDTO);

    /**
     * 获取放款记录列表
     */
    Page<AdminDisbursementVO> getDisbursementList(DisbursementQueryDTO queryDTO);

    /**
     * 执行放款
     */
    void executeDisbursement(Long id);

    /**
     * 拒绝放款
     */
    void rejectDisbursement(Long id, DisbursementRejectDTO rejectDTO);
}
