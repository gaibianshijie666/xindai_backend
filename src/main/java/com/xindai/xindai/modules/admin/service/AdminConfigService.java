package com.xindai.xindai.modules.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.admin.dto.ConfigUpdateDTO;
import com.xindai.xindai.modules.admin.vo.ConfigVO;

import java.util.List;
import java.util.Map;

/**
 * Admin config service interface
 */
public interface AdminConfigService {

    /**
     * Get all configs grouped by category
     */
    Map<String, List<ConfigVO>> getAllConfigs();

    /**
     * Get configs by category
     */
    List<ConfigVO> getConfigsByCategory(String category);

    /**
     * Update config values (SUPER_ADMIN only)
     */
    void updateConfigs(ConfigUpdateDTO updateDTO);
}
