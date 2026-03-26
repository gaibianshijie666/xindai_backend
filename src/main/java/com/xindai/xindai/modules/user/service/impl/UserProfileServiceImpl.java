package com.xindai.xindai.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.user.dto.*;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import com.xindai.xindai.modules.user.service.UserProfileService;
import com.xindai.xindai.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    @Cacheable(value = "userById", key = "#id", unless = "#result == null")
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    @Cacheable(value = "userByPhone", key = "#phone", unless = "#result == null")
    public User getByPhone(String phone) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
        );
    }

    @Override
    @CacheEvict(value = {"userById", "userByPhone"}, allEntries = true)
    public UserVO updateProfile(Long userId, UserUpdateDTO dto) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 更新用户信息
        if (dto.getRealName() != null) {
            user.setRealName(dto.getRealName());
        }
        if (dto.getIdCard() != null) {
            user.setIdCard(dto.getIdCard());
        }

        userMapper.updateById(user);

        log.info("User profile updated: userId={}", userId);
        return buildUserVO(user, null);
    }

    @Override
    @CacheEvict(value = {"userById", "userByPhone"}, allEntries = true)
    public void changePassword(Long userId, PasswordChangeDTO dto) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 验证旧密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "旧密码错误");
        }

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);

        // 使该用户的所有Token失效（可以通过Redis黑名单实现）
        jwtUtils.invalidateToken(userId);

        log.info("User password changed: userId={}", userId);
    }

    @Override
    @CacheEvict(value = {"userById", "userByPhone"}, allEntries = true)
    public UserVO verifyIdentity(Long userId, VerifyIdentityDTO dto) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 检查是否已实名认证
        if (user.getRealName() != null && user.getIdCard() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已完成实名认证");
        }

        // TODO: 调用第三方实名认证接口验证姓名和身份证号

        // 更新实名信息
        user.setRealName(dto.getRealName());
        user.setIdCard(dto.getIdCard());
        userMapper.updateById(user);

        log.info("User identity verified: userId={}", userId);
        return buildUserVO(user, null);
    }

    private UserVO buildUserVO(User user, String token) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setRealName(user.getRealName());
        vo.setIdCard(user.getIdCard());
        vo.setStatus(user.getStatus());
        vo.setToken(token);
        return vo;
    }
}
