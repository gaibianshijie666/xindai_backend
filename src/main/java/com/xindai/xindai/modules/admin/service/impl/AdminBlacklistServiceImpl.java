package com.xindai.xindai.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.admin.dto.BlacklistAddDTO;
import com.xindai.xindai.modules.admin.dto.BlacklistQueryDTO;
import com.xindai.xindai.modules.admin.service.AdminBlacklistService;
import com.xindai.xindai.modules.admin.vo.BlacklistVO;
import com.xindai.xindai.modules.risk.entity.Blacklist;
import com.xindai.xindai.modules.risk.mapper.BlacklistMapper;
import com.xindai.xindai.modules.risk.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端黑名单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBlacklistServiceImpl implements AdminBlacklistService {

    private final BlacklistMapper blacklistMapper;
    private final BlacklistService blacklistService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PHONE_BLACKLIST_KEY = "blacklist:phone";
    private static final String IDCARD_BLACKLIST_KEY = "blacklist:idcard";
    private static final String DEVICE_BLACKLIST_KEY = "blacklist:device";

    @Override
    public Page<BlacklistVO> getBlacklistList(BlacklistQueryDTO queryDTO) {
        Page<Blacklist> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        LambdaQueryWrapper<Blacklist> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getType() != null) {
            wrapper.eq(Blacklist::getType, queryDTO.getType());
        }
        wrapper.orderByDesc(Blacklist::getCreatedAt);

        Page<Blacklist> blacklistPage = blacklistMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<BlacklistVO> voPage = new Page<>(blacklistPage.getCurrent(), blacklistPage.getSize(), blacklistPage.getTotal());
        List<BlacklistVO> voList = blacklistPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public void addBlacklist(BlacklistAddDTO addDTO) {
        // 检查是否已存在
        Long count = blacklistMapper.selectCount(
                new LambdaQueryWrapper<Blacklist>()
                        .eq(Blacklist::getType, addDTO.getType())
                        .eq(Blacklist::getValue, addDTO.getValue())
                        .and(wrapper -> wrapper.isNull(Blacklist::getExpireAt)
                                .or().gt(Blacklist::getExpireAt, LocalDateTime.now()))
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.BLACKLIST_ALREADY_EXISTS);
        }

        Blacklist blacklist = new Blacklist();
        blacklist.setType(addDTO.getType());
        blacklist.setValue(addDTO.getValue());
        blacklist.setReason(addDTO.getReason());
        blacklist.setCreatedAt(LocalDateTime.now());
        blacklistMapper.insert(blacklist);

        // 更新缓存
        String cacheKey = getCacheKey(addDTO.getType());
        if (cacheKey != null) {
            redisTemplate.opsForSet().add(cacheKey, addDTO.getValue());
        }

        log.info("添加黑名单: type={}, value={}, reason={}", addDTO.getType(), addDTO.getValue(), addDTO.getReason());
    }

    @Override
    public void removeBlacklist(Long id) {
        Blacklist blacklist = blacklistMapper.selectById(id);
        if (blacklist == null) {
            throw new BusinessException(ErrorCode.BLACKLIST_NOT_FOUND);
        }

        blacklistMapper.deleteById(id);

        // 更新缓存
        String cacheKey = getCacheKey(blacklist.getType());
        if (cacheKey != null) {
            redisTemplate.opsForSet().remove(cacheKey, blacklist.getValue());
        }

        log.info("移除黑名单: id={}, type={}, value={}", id, blacklist.getType(), blacklist.getValue());
    }

    private BlacklistVO convertToVO(Blacklist blacklist) {
        BlacklistVO vo = new BlacklistVO();
        vo.setId(blacklist.getId());
        vo.setType(blacklist.getType());
        vo.setValue(blacklist.getValue());
        vo.setReason(blacklist.getReason());
        vo.setExpireAt(blacklist.getExpireAt());
        vo.setCreatedAt(blacklist.getCreatedAt());
        return vo;
    }

    private String getCacheKey(Integer type) {
        return switch (type) {
            case 1 -> PHONE_BLACKLIST_KEY;
            case 2 -> IDCARD_BLACKLIST_KEY;
            case 3 -> DEVICE_BLACKLIST_KEY;
            default -> null;
        };
    }
}
