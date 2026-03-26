package com.xindai.xindai.modules.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xindai.xindai.modules.user.dto.UserVO;
import com.xindai.xindai.modules.user.entity.User;
import com.xindai.xindai.modules.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserProfileController.class)
@DisplayName("UserProfileController 单元测试")
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserProfileService userProfileService;

    private User testUser;
    private UserVO userVO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setPhone("13800138000");
        testUser.setStatus(1);

        userVO = new UserVO();
        userVO.setId(1L);
        userVO.setPhone("13800138000");
    }

    @Nested
    @DisplayName("获取用户信息接口测试")
    class ProfileTests {

        @Test
        @DisplayName("获取用户信息 - 需要认证")
        void profile_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/user/profile"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
