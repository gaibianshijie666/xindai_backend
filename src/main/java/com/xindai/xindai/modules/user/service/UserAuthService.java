package com.xindai.xindai.modules.user.service;

import com.xindai.xindai.modules.user.dto.UserLoginDTO;
import com.xindai.xindai.modules.user.dto.UserRegisterDTO;
import com.xindai.xindai.modules.user.dto.UserVO;

/**
 * 用户认证服务接口
 */
public interface UserAuthService {
    /**
     * 用户注册
     */
    UserVO register(UserRegisterDTO dto);

    /**
     * 用户登录
     */
    UserVO login(UserLoginDTO dto);

    /**
     * 用户登出
     */
    void logout(Long userId);
}
