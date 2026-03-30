package com.xindai.xindai.modules.enterprise.service;

import com.xindai.xindai.modules.enterprise.dto.EnterpriseInfoVO;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseLoginDTO;
import com.xindai.xindai.modules.enterprise.dto.EnterpriseUserVO;
import com.xindai.xindai.modules.enterprise.dto.PasswordChangeDTO;
import com.xindai.xindai.modules.enterprise.entity.Enterprise;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseUser;

/**
 * 企业认证服务接口
 */
public interface EnterpriseAuthService {

    /**
     * 企业用户登录
     */
    EnterpriseUserVO login(EnterpriseLoginDTO dto);

    /**
     * 获取当前用户信息
     */
    EnterpriseUser getCurrentUser(Long userId);

    /**
     * 获取企业信息
     */
    Enterprise getEnterprise(Long enterpriseId);

    /**
     * 获取当前用户档案VO
     */
    EnterpriseUserVO getUserProfile(Long userId, Long enterpriseId);

    /**
     * 获取企业信息VO
     */
    EnterpriseInfoVO getEnterpriseInfoVO(Long enterpriseId);

    /**
     * 修改密码
     */
    void changePassword(Long userId, PasswordChangeDTO dto);

    /**
     * 退出登录
     */
    void logout(Long userId);
}
