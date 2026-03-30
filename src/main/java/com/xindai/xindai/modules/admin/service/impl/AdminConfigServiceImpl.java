package com.xindai.xindai.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.admin.dto.ConfigUpdateDTO;
import com.xindai.xindai.modules.admin.entity.SystemConfig;
import com.xindai.xindai.modules.admin.mapper.SystemConfigMapper;
import com.xindai.xindai.modules.admin.service.AdminConfigService;
import com.xindai.xindai.modules.admin.vo.ConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin config service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminConfigServiceImpl implements AdminConfigService {

    private final SystemConfigMapper systemConfigMapper;

    @Override
    public Map<String, List<ConfigVO>> getAllConfigs() {
        List<SystemConfig> configs = systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>()
                        .orderByAsc(SystemConfig::getCategory)
                        .orderByAsc(SystemConfig::getId)
        );

        return configs.stream()
                .map(this::toVO)
                .collect(Collectors.groupingBy(ConfigVO::getCategory));
    }

    @Override
    public List<ConfigVO> getConfigsByCategory(String category) {
        List<SystemConfig> configs = systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getCategory, category)
                        .orderByAsc(SystemConfig::getId)
        );
        return configs.stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigs(ConfigUpdateDTO updateDTO) {
        for (ConfigUpdateDTO.ConfigItem item : updateDTO.getItems()) {
            SystemConfig config = systemConfigMapper.selectOne(
                    new LambdaQueryWrapper<SystemConfig>()
                            .eq(SystemConfig::getConfigKey, item.getKey())
            );

            if (config == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "配置项不存在: " + item.getKey());
            }

            // Validate value format based on config key
            validateConfigValue(config.getConfigKey(), item.getValue());

            // Log the change
            String oldValue = config.getConfigValue();
            String newValue = item.getValue();
            log.info("Updating config: key={}, oldValue={}, newValue={}, reason={}",
                    item.getKey(), oldValue, newValue, item.getReason());

            config.setConfigValue(newValue);
            config.setUpdatedAt(LocalDateTime.now());
            systemConfigMapper.updateById(config);
        }
    }

    private ConfigVO toVO(SystemConfig entity) {
        ConfigVO vo = new ConfigVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private void validateConfigValue(String key, String value) {
        // Validate numeric configs
        if (key.contains("limit") || key.contains("amount") || key.contains("rate")) {
            try {
                new java.math.BigDecimal(value);
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "配置值格式错误: " + key + " 必须是数字");
            }
        }

        // Validate percentage configs
        if (key.contains("percentage") || key.contains("ratio")) {
            try {
                double val = Double.parseDouble(value);
                if (val < 0 || val > 100) {
                    throw new BusinessException(ErrorCode.INVALID_PARAM, "配置值必须在0-100之间: " + key);
                }
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "配置值格式错误: " + key);
            }
        }
    }
}
