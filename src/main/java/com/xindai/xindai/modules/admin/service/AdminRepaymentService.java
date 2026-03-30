package com.xindai.xindai.modules.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.admin.dto.ManualRepayDTO;
import com.xindai.xindai.modules.admin.dto.RepaymentAdjustDTO;
import com.xindai.xindai.modules.admin.dto.RepaymentQueryDTO;
import com.xindai.xindai.modules.admin.vo.AdminRepaymentVO;
import com.xindai.xindai.modules.admin.vo.OverdueRepaymentVO;
import com.xindai.xindai.modules.admin.vo.RepaymentStatsVO;

import java.util.List;

/**
 * 管理端还款管理服务接口
 */
public interface AdminRepaymentService {

    /**
     * 获取还款计划列表（分页）
     */
    Page<AdminRepaymentVO> getRepaymentList(RepaymentQueryDTO queryDTO);

    /**
     * 获取逾期还款列表（含借款人信息）
     */
    List<OverdueRepaymentVO> getOverdueRepayments();

    /**
     * 调整罚息（仅逾期状态可调整）
     */
    void adjustPenalty(Long id, RepaymentAdjustDTO adjustDTO);

    /**
     * 手动还款（记录线下还款）
     */
    void manualRepay(Long id, ManualRepayDTO repayDTO);

    /**
     * 获取还款统计数据
     */
    RepaymentStatsVO getRepaymentStats();
}
