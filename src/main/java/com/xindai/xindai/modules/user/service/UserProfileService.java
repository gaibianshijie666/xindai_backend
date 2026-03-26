package com.xindai.xindai.modules.user.service;

import com.xindai.xindai.modules.user.dto.*;
import com.xindai.xindai.modules.user.entity.User;

/**
 * 用户资料服务接口
 */
public interface UserProfileService {
    /**
     * 根据ID获取用户
     */
    User getById(Long id);

    /**
     * 根据手机号获取用户
     */
    User getByPhone(String phone);

    /**
     * 获取用户档案VO
     */
    UserVO getProfileVO(Long userId);

    /**
     * 更新用户资料
     */
    UserVO updateProfile(Long userId, UserUpdateDTO dto);

    /**
     * 修改密码
     */
    void changePassword(Long userId, PasswordChangeDTO dto);

    /**
     * 实名认证
     */
    UserVO verifyIdentity(Long userId, VerifyIdentityDTO dto);

    /**
     * 统计用户总数
     */
    long countUsers();

    /**
     * 根据风险等级统计用户画像数量
     */
    int countProfilesByRiskLevel(Integer riskLevel);
}
