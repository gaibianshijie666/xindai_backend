package com.xindai.xindai.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.admin.dto.AdminLoginDTO;
import com.xindai.xindai.modules.admin.dto.AdminRegisterDTO;
import com.xindai.xindai.modules.admin.entity.AdminUser;
import com.xindai.xindai.modules.admin.mapper.AdminUserMapper;
import com.xindai.xindai.modules.admin.service.AdminAuthService;
import com.xindai.xindai.modules.admin.vo.AdminLoginVO;
import com.xindai.xindai.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public AdminLoginVO login(AdminLoginDTO dto) {
        AdminUser admin = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, dto.getUsername())
        );

        if (admin == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "管理员账号不存在");
        }

        if (admin.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "管理员账号已被禁用");
        }

        if (!passwordEncoder.matches(dto.getPassword(), admin.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        String token = jwtUtils.generateToken(admin.getId(), admin.getUsername(), "ADMIN", null);

        return buildAdminLoginVO(admin, token);
    }

    @Override
    public AdminLoginVO register(AdminRegisterDTO dto) {
        // 检查用户名是否已存在
        AdminUser existAdmin = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, dto.getUsername())
        );

        if (existAdmin != null) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS, "用户名已存在");
        }

        // 创建管理员账号
        AdminUser admin = new AdminUser();
        admin.setUsername(dto.getUsername());
        admin.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        admin.setRealName(dto.getRealName());
        admin.setPhone(dto.getPhone());
        admin.setRole("ADMIN"); // 默认为普通管理员
        admin.setStatus(1);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        adminUserMapper.insert(admin);

        String token = jwtUtils.generateToken(admin.getId(), admin.getUsername(), "ADMIN", null);

        log.info("Admin registered: username={}", dto.getUsername());
        return buildAdminLoginVO(admin, token);
    }

    private AdminLoginVO buildAdminLoginVO(AdminUser admin, String token) {
        AdminLoginVO vo = new AdminLoginVO();
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setRealName(admin.getRealName());
        vo.setRole(admin.getRole());
        vo.setToken(token);
        return vo;
    }
}
