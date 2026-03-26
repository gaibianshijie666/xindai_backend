package com.xindai.xindai.modules.user.service;

import com.xindai.xindai.modules.user.dto.UserProfileUpdateDTO;
import com.xindai.xindai.modules.user.dto.UserProfileVO;

/**
 * 用户画像服务接口
 */
public interface UserProfileDetailService {
    /**
     * 获取用户画像详情
     */
    UserProfileVO getProfileDetail(Long userId);

    /**
     * 更新用户画像信息
     */
    UserProfileVO updateProfileDetail(Long userId, UserProfileUpdateDTO dto);
}
