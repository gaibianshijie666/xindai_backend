package com.xindai.xindai.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.admin.dto.AdminCreateDTO;
import com.xindai.xindai.modules.admin.dto.AdminResetPasswordDTO;
import com.xindai.xindai.modules.admin.dto.AdminRoleDTO;
import com.xindai.xindai.modules.admin.dto.UserStatusUpdateDTO;
import com.xindai.xindai.modules.admin.entity.AdminUser;
import com.xindai.xindai.modules.admin.mapper.AdminUserMapper;
import com.xindai.xindai.modules.admin.service.AdminAccountService;
import com.xindai.xindai.modules.admin.vo.AdminAccountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端管理员账号服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAccountServiceImpl implements AdminAccountService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<AdminAccountVO> getAdminList(Integer page, Integer size, String keyword) {
        Page<AdminUser> userPage = new Page<>(page, size);

        LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(AdminUser::getUsername, keyword)
                    .or()
                    .like(AdminUser::getRealName, keyword)
                    .or()
                    .like(AdminUser::getPhone, keyword)
            );
        }
        wrapper.orderByDesc(AdminUser::getCreatedAt);

        Page<AdminUser> resultPage = adminUserMapper.selectPage(userPage, wrapper);

        // 转换为VO
        Page<AdminAccountVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<AdminAccountVO> voList = resultPage.getRecords().stream()
                .map(this::convertToAccountVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public void createAdmin(AdminCreateDTO createDTO, Long currentAdminId) {
        // 检查用户名是否已存在
        AdminUser existAdmin = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, createDTO.getUsername())
        );

        if (existAdmin != null) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS, "用户名已存在");
        }

        // 创建管理员账号
        AdminUser admin = new AdminUser();
        admin.setUsername(createDTO.getUsername());
        admin.setPasswordHash(passwordEncoder.encode(createDTO.getPassword()));
        admin.setRealName(createDTO.getRealName());
        admin.setPhone(createDTO.getPhone());
        admin.setRole(createDTO.getRole());
        admin.setStatus(1);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());

        adminUserMapper.insert(admin);

        log.info("创建管理员账号: username={}, role={}, operatorId={}",
                createDTO.getUsername(), createDTO.getRole(), currentAdminId);
    }

    @Override
    public void updateAdminRole(Long id, AdminRoleDTO roleDTO, Long currentAdminId) {
        // 不能修改自己的角色
        if (id.equals(currentAdminId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能修改自己的角色");
        }

        AdminUser admin = adminUserMapper.selectById(id);
        if (admin == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "管理员不存在");
        }

        LambdaUpdateWrapper<AdminUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AdminUser::getId, id)
                .set(AdminUser::getRole, roleDTO.getRole())
                .set(AdminUser::getUpdatedAt, LocalDateTime.now());

        adminUserMapper.update(null, wrapper);

        log.info("修改管理员角色: adminId={}, newRole={}, operatorId={}",
                id, roleDTO.getRole(), currentAdminId);
    }

    @Override
    public void updateAdminStatus(Long id, UserStatusUpdateDTO statusDTO, Long currentAdminId) {
        // 不能禁用自己
        if (id.equals(currentAdminId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能禁用自己的账号");
        }

        AdminUser admin = adminUserMapper.selectById(id);
        if (admin == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "管理员不存在");
        }

        LambdaUpdateWrapper<AdminUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AdminUser::getId, id)
                .set(AdminUser::getStatus, statusDTO.getStatus())
                .set(AdminUser::getUpdatedAt, LocalDateTime.now());

        adminUserMapper.update(null, wrapper);

        log.info("修改管理员状态: adminId={}, newStatus={}, operatorId={}",
                id, statusDTO.getStatus(), currentAdminId);
    }

    @Override
    public void resetAdminPassword(Long id, AdminResetPasswordDTO resetDTO) {
        AdminUser admin = adminUserMapper.selectById(id);
        if (admin == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "管理员不存在");
        }

        String encodedPassword = passwordEncoder.encode(resetDTO.getNewPassword());

        LambdaUpdateWrapper<AdminUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AdminUser::getId, id)
                .set(AdminUser::getPasswordHash, encodedPassword)
                .set(AdminUser::getUpdatedAt, LocalDateTime.now());

        adminUserMapper.update(null, wrapper);

        log.info("重置管理员密码: adminId={}", id);
    }

    private AdminAccountVO convertToAccountVO(AdminUser admin) {
        AdminAccountVO vo = new AdminAccountVO();
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setRealName(admin.getRealName());
        vo.setPhone(admin.getPhone());
        vo.setRole(admin.getRole());
        vo.setStatus(admin.getStatus());
        vo.setCreatedAt(admin.getCreatedAt());
        vo.setUpdatedAt(admin.getUpdatedAt());
        return vo;
    }
}
