package com.xindai.xindai.modules.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xindai.xindai.modules.admin.dto.UserQueryDTO;
import com.xindai.xindai.modules.admin.dto.UserStatusUpdateDTO;
import com.xindai.xindai.modules.admin.vo.AdminUserDetailVO;
import com.xindai.xindai.modules.admin.vo.AdminUserVO;

/**
 * 管理端用户服务接口
 */
public interface AdminUserService {

    /**
     * 获取用户列表
     */
    Page<AdminUserVO> getUserList(UserQueryDTO queryDTO);

    /**
     * 获取用户详情
     */
    AdminUserDetailVO getUserDetail(Long id);

    /**
     * 更新用户状态
     */
    void updateUserStatus(Long id, UserStatusUpdateDTO updateDTO);
}
