package com.xindai.xindai.modules.enterprise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseInfoVO;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseLoginDTO;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseUserVO;
import com.xindai.xindai.modules.enterprise.dto.PasswordChangeDTO;
import com.xindai.xindai.modules.enterprise.entity.Enterprise;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseUser;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseMapper;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseUserMapper;
import com.xindai.xindai.modules.enterprise.service.EnterpriseAuthService;
import com.xindai.xindai.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 企业认证服务实现
 */
@Service
@RequiredArgsConstructor
public class EnterpriseAuthServiceImpl implements EnterpriseAuthService {

    private final EnterpriseUserMapper enterpriseUserMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public EnterpriseUserVO login(EnterpriseLoginDTO dto) {
        // 1. 查询企业
        Enterprise enterprise = enterpriseMapper.selectOne(
            new LambdaQueryWrapper<Enterprise>()
                .eq(Enterprise::getEnterpriseNo, dto.getEnterpriseNo())
        );
        if (enterprise == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }
        if (enterprise.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ENTERPRISE_DISABLED);
        }

        // 2. 查询用户
        EnterpriseUser user = enterpriseUserMapper.selectOne(
            new LambdaQueryWrapper<EnterpriseUser>()
                .eq(EnterpriseUser::getEnterpriseId, enterprise.getId())
                .eq(EnterpriseUser::getUsername, dto.getUsername())
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_USER_NOT_FOUND);
        }
        if (user.getStatus() != 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 3. 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 4. 生成Token
        String token = jwtUtils.generateToken(
            user.getId(), user.getUsername(), "ENTERPRISE", enterprise.getId()
        );

        // 5. 构建VO - 显式设置字段，避免使用BeanUtils.copyProperties防止敏感字段泄露
        EnterpriseUserVO vo = new EnterpriseUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setRole(user.getRole());
        vo.setEnterpriseId(enterprise.getId());
        vo.setEnterpriseName(enterprise.getName());
        vo.setToken(token);
        return vo;
    }

    @Override
    public EnterpriseUser getCurrentUser(Long userId) {
        EnterpriseUser user = enterpriseUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    public Enterprise getEnterprise(Long enterpriseId) {
        Enterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }
        return enterprise;
    }

    @Override
    public EnterpriseUserVO getUserProfile(Long userId, Long enterpriseId) {
        EnterpriseUser user = getCurrentUser(userId);
        Enterprise enterprise = getEnterprise(enterpriseId);

        EnterpriseUserVO vo = new EnterpriseUserVO();
        vo.setId(user.getId());
        vo.setEnterpriseId(enterprise.getId());
        vo.setEnterpriseName(enterprise.getName());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setRole(user.getRole());
        return vo;
    }

    @Override
    public EnterpriseInfoVO getEnterpriseInfoVO(Long enterpriseId) {
        Enterprise enterprise = getEnterprise(enterpriseId);
        EnterpriseInfoVO vo = new EnterpriseInfoVO();
        vo.setName(enterprise.getName());
        vo.setEnterpriseNo(enterprise.getEnterpriseNo());
        vo.setApiKey(enterprise.getApiKey());
        return vo;
    }

    @Override
    public void changePassword(Long userId, PasswordChangeDTO dto) {
        EnterpriseUser user = enterpriseUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_USER_NOT_FOUND);
        }

        // 验证旧密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "旧密码错误");
        }

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        enterpriseUserMapper.updateById(user);
    }

    @Override
    public void logout(Long userId) {
        jwtUtils.invalidateToken(userId);
    }
}
