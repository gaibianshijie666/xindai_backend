package com.xindai.xindai.modules.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.admin.dto.BlacklistAddDTO;
import com.xindai.xindai.modules.admin.dto.BlacklistQueryDTO;
import com.xindai.xindai.modules.admin.vo.BlacklistVO;

/**
 * 管理端黑名单服务接口
 */
public interface AdminBlacklistService {

    /**
     * 获取黑名单列表
     */
    Page<BlacklistVO> getBlacklistList(BlacklistQueryDTO queryDTO);

    /**
     * 添加黑名单
     */
    void addBlacklist(BlacklistAddDTO addDTO);

    /**
     * 移除黑名单
     */
    void removeBlacklist(Long id);
}
