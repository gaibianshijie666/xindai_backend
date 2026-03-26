package com.xindai.xindai.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xindai.xindai.modules.user.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RiskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // 使用管理员账号登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone("admin");
        loginRequest.setPassword("admin123");

        MvcResult result = mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        adminToken = objectMapper.readTree(response).path("data").path("token").asText();
    }

    @Test
    void getAssessmentHistory_Success() throws Exception {
        mockMvc.perform(get("/api/v1/risk/history/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void getRiskStats_Success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/risk-stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
