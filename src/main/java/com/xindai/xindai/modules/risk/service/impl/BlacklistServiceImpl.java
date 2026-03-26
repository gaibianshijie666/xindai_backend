package com.xindai.xindai.modules.risk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.modules.risk.entity.Blacklist;
import com.xindai.xindai.modules.risk.mapper.BlacklistMapper;
import com.xindai.xindai.modules.risk.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlacklistServiceImpl implements BlacklistService {

    private final BlacklistMapper blacklistMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PHONE_BLACKLIST_KEY = "blacklist:phone";
    private static final String IDCARD_BLACKLIST_KEY = "blacklist:idcard";

    @Override
    public boolean isPhoneBlacklisted(String phone) {
        // 先查缓存
        Boolean inCache = redisTemplate.opsForSet().isMember(PHONE_BLACKLIST_KEY, phone);
        if (Boolean.TRUE.equals(inCache)) {
            return true;
        }

        // 查数据库
        Blacklist record = blacklistMapper.selectOne(
                new LambdaQueryWrapper<Blacklist>()
                        .eq(Blacklist::getType, 1)
                        .eq(Blacklist::getValue, phone)
                        .and(wrapper -> wrapper.isNull(Blacklist::getExpireAt)
                                .or().gt(Blacklist::getExpireAt, LocalDateTime.now()))
        );

        if (record != null) {
            // 加入缓存
            redisTemplate.opsForSet().add(PHONE_BLACKLIST_KEY, phone);
            return true;
        }
        return false;
    }

    @Override
    public boolean isIdCardBlacklisted(String idCard) {
        Boolean inCache = redisTemplate.opsForSet().isMember(IDCARD_BLACKLIST_KEY, idCard);
        if (Boolean.TRUE.equals(inCache)) {
            return true;
        }

        Blacklist record = blacklistMapper.selectOne(
                new LambdaQueryWrapper<Blacklist>()
                        .eq(Blacklist::getType, 2)
                        .eq(Blacklist::getValue, idCard)
                        .and(wrapper -> wrapper.isNull(Blacklist::getExpireAt)
                                .or().gt(Blacklist::getExpireAt, LocalDateTime.now()))
        );

        if (record != null) {
            redisTemplate.opsForSet().add(IDCARD_BLACKLIST_KEY, idCard);
            return true;
        }
        return false;
    }

    @Override
    public void addToBlacklist(Integer type, String value, String reason) {
        Blacklist blacklist = new Blacklist();
        blacklist.setType(type);
        blacklist.setValue(value);
        blacklist.setReason(reason);
        blacklist.setCreatedAt(LocalDateTime.now());
        blacklistMapper.insert(blacklist);

        // 更新缓存
        String key = type == 1 ? PHONE_BLACKLIST_KEY : IDCARD_BLACKLIST_KEY;
        redisTemplate.opsForSet().add(key, value);
    }

    @Override
    public void removeFromBlacklist(Integer type, String value) {
        blacklistMapper.delete(
                new LambdaQueryWrapper<Blacklist>()
                        .eq(Blacklist::getType, type)
                        .eq(Blacklist::getValue, value)
        );

        // 更新缓存
        String key = type == 1 ? PHONE_BLACKLIST_KEY : IDCARD_BLACKLIST_KEY;
        redisTemplate.opsForSet().remove(key, value);
    }

    @Override
    public List<Blacklist> getBlacklist(Integer type) {
        return blacklistMapper.selectList(
                new LambdaQueryWrapper<Blacklist>()
                        .eq(type != null, Blacklist::getType, type)
                        .orderByDesc(Blacklist::getCreatedAt)
        );
    }
}
