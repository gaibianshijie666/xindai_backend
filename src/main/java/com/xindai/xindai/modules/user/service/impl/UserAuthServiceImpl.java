package com.xindai.xindai.modules.user.service.impl;

import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.user.dto.UserLoginDTO;
import com.xindai.xindai.modules.user.dto.UserRegisterDTO;
import com.xindai.xindai.modules.user.dto.UserVO;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.mapper.UserMapper;
import com.xindai.xindai.modules.user.service.UserAuthService;
import com.xindai.xindai.modules.user.service.UserProfileService;
import com.xindai.xindai.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final UserProfileService userProfileService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    @CacheEvict(value = {"userByPhone", "userById"}, allEntries = true)
    public UserVO register(UserRegisterDTO dto) {
        // 检查手机号是否已注册
        User existUser = userProfileService.getByPhone(dto.getPhone());
        if (existUser != null) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

        // 创建用户
        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(1);
        userMapper.insert(user);

        // 生成 token
        String token = jwtUtils.generateToken(user.getId(), user.getPhone());

        log.info("User registered: userId={}, phone={}", user.getId(), user.getPhone());
        return buildUserVO(user, token);
    }

    @Override
    public UserVO login(UserLoginDTO dto) {
        User user = userProfileService.getByPhone(dto.getPhone());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        String token = jwtUtils.generateToken(user.getId(), user.getPhone());
        log.info("User logged in: userId={}", user.getId());
        return buildUserVO(user, token);
    }

    @Override
    public void logout(Long userId) {
        // 使该用户的Token失效
        jwtUtils.invalidateToken(userId);
        log.info("User logged out: userId={}", userId);
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
