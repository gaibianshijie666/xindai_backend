package com.xindai.xindai.modules.admin.service;

import com.xindai.xindai.modules.admin.dto.AdminLoginDTO;
import com.xindai.xindai.modules.admin.dto.AdminRegisterDTO;
import com.xindai.xindai.modules.admin.vo.AdminLoginVO;

public interface AdminAuthService {
    AdminLoginVO login(AdminLoginDTO dto);
    AdminLoginVO register(AdminRegisterDTO dto);
}
