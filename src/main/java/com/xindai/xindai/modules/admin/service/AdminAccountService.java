package com.xindai.xindai.modules.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.admin.dto.AdminCreateDTO;
import com.xindai.xindai.modules.admin.dto.AdminResetPasswordDTO;
import com.xindai.xindai.modules.admin.dto.AdminRoleDTO;
import com.xindai.xindai.modules.admin.dto.UserStatusUpdateDTO;
import com.xindai.xindai.modules.admin.vo.AdminAccountVO;

/**
 * 管理端管理员账号服务接口
 */
public interface AdminAccountService {

    /**
     * 获取管理员账号列表
     */
    Page<AdminAccountVO> getAdminList(Integer page, Integer size, String keyword);

    /**
     * 创建管理员账号（仅超级管理员）
     */
    void createAdmin(AdminCreateDTO createDTO, Long currentAdminId);

    /**
     * 修改管理员角色（仅超级管理员）
     */
    void updateAdminRole(Long id, AdminRoleDTO roleDTO, Long currentAdminId);

    /**
     * 修改管理员状态（仅超级管理员）
     */
    void updateAdminStatus(Long id, UserStatusUpdateDTO statusDTO, Long currentAdminId);

    /**
     * 重置管理员密码（仅超级管理员）
     */
    void resetAdminPassword(Long id, AdminResetPasswordDTO resetDTO);
}
