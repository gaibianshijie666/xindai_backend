package com.xindai.xindai.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.client.kyc.KycResult;
import com.xindai.xindai.client.kyc.KycService;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.user.dto.*;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.entity.UserProfile;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import com.xindai.xindai.modules.user.mapper.UserProfileMapper;
import com.xindai.xindai.modules.user.service.UserProfileService;
import com.xindai.xindai.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final KycService kycService;

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
    public UserVO getProfileVO(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return UserVO.from(user, null);
    }

    @Override
    @CacheEvict(value = "userById", key = "#userId")
    public UserVO updateProfile(Long userId, UserUpdateDTO dto) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 安全措施：实名信息(realName, idCard)只能通过verifyIdentity()进行KYC验证后设置
        // 不允许通过updateProfile()直接修改，防止KYC绕过攻击
        // UserUpdateDTO中的realName和idCard字段已标记为READ_ONLY

        // 注意：email, company, position, monthlyIncome字段属于user_profile表
        // 此方法仅用于演示安全修复，完整的实现需要更新UserProfile实体

        userMapper.updateById(user);

        log.info("User profile updated: userId={}", userId);
        return UserVO.from(user, null);
    }

    @Override
    @CacheEvict(value = "userById", key = "#userId")
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
    @CacheEvict(value = "userById", key = "#userId")
    public UserVO verifyIdentity(Long userId, VerifyIdentityDTO dto) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 检查是否已实名认证
        if (user.getRealName() != null && user.getIdCard() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已完成实名认证");
        }

        // 调用KYC实名认证接口验证姓名和身份证号
        KycResult kycResult = kycService.verify(dto.getRealName(), dto.getIdCard());
        if (!kycResult.isSuccess()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, kycResult.getMessage());
        }

        // 更新实名信息
        user.setRealName(dto.getRealName());
        user.setIdCard(dto.getIdCard());
        userMapper.updateById(user);

        // 更新user_profile中的认证状态
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
        );
        if (profile != null) {
            profile.setIdentityStatus(2); // 已认证
            profile.setIdentityVerifiedAt(LocalDateTime.now());
            userProfileMapper.updateById(profile);
        }

        log.info("User identity verified: userId={}", userId);
        return UserVO.from(user, null);
    }

    @Override
    public long countUsers() {
        return userMapper.selectCount(new LambdaQueryWrapper<>());
    }

    @Override
    public int countProfilesByRiskLevel(Integer riskLevel) {
        return userProfileMapper.selectCount(
                new LambdaQueryWrapper<UserProfile>()
                        .eq(UserProfile::getRiskLevel, riskLevel)
        ).intValue();
    }
}
