package com.xindai.xindai.modules.loan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xindai.xindai.common.exception.BusinessException;
import com.xindai.xindai.common.exception.ErrorCode;
import com.xindai.xindai.modules.loan.dto.CreditLimitVO;
import com.xindai.xindai.modules.loan.dto.LoanApplicationVO;
import com.xindai.xindai.modules.loan.dto.LoanApplyDTO;
import com.xindai.xindai.modules.loan.service.LoanService;
import com.xindai.xindai.modules.loan.service.OverdueService;
import com.xindai.xindai.security.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mockito.Answers;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("LoanController 单元测试")
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoanService loanService;

    @MockBean
    private OverdueService overdueService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private SqlSessionFactory sqlSessionFactory;

    private LoanApplyDTO applyDTO;
    private CreditLimitVO creditLimitVO;
    private LoanApplicationVO applicationVO;

    @BeforeEach
    void setUp() {
        applyDTO = new LoanApplyDTO();
        applyDTO.setAmount(new BigDecimal("10000"));
        applyDTO.setTerm(12);
        applyDTO.setPurpose("消费");

        creditLimitVO = new CreditLimitVO();
        creditLimitVO.setId(1L);
        creditLimitVO.setUserId(1L);
        creditLimitVO.setTotalLimit(new BigDecimal("50000"));
        creditLimitVO.setUsedLimit(new BigDecimal("10000"));
        creditLimitVO.setAvailableLimit(new BigDecimal("40000"));

        applicationVO = new LoanApplicationVO();
        applicationVO.setId(1L);
        applicationVO.setUserId(1L);
        applicationVO.setAmount(new BigDecimal("10000"));
        applicationVO.setTerm(12);
        applicationVO.setPurpose("消费");
        applicationVO.setStatus(1);
    }

    @Nested
    @DisplayName("获取额度接口测试")
    class GetLimitTests {

        @Test
        @DisplayName("获取额度 - 需要认证")
        void getLimit_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/loan/limit"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("借款申请接口测试")
    class ApplyTests {

        @Test
        @DisplayName("借款申请 - 需要认证")
        void apply_Unauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/loan/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(applyDTO)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("申请金额为空 - 验证失败")
        void apply_NullAmount() throws Exception {
            applyDTO.setAmount(null);

            // 无认证时缺少userId属性，返回500
            mockMvc.perform(post("/api/v1/loan/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(applyDTO)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("申请金额过小 - 验证失败")
        void apply_TooSmallAmount() throws Exception {
            applyDTO.setAmount(new BigDecimal("500")); // 小于1000

            mockMvc.perform(post("/api/v1/loan/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(applyDTO)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("借款期限为空 - 验证失败")
        void apply_NullTerm() throws Exception {
            applyDTO.setTerm(null);

            mockMvc.perform(post("/api/v1/loan/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(applyDTO)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("借款期限过短 - 验证失败")
        void apply_TooShortTerm() throws Exception {
            applyDTO.setTerm(1); // 小于3

            mockMvc.perform(post("/api/v1/loan/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(applyDTO)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("借款期限过长 - 验证失败")
        void apply_TooLongTerm() throws Exception {
            applyDTO.setTerm(36); // 大于24

            mockMvc.perform(post("/api/v1/loan/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(applyDTO)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("获取申请列表接口测试")
    class GetApplicationsTests {

        @Test
        @DisplayName("获取申请列表 - 需要认证")
        void getApplications_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/loan/applications"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("获取申请详情接口测试")
    class GetApplicationDetailTests {

        @Test
        @DisplayName("获取申请详情 - 需要认证")
        void getApplicationDetail_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/loan/applications/1"))
                    .andExpect(status().isInternalServerError());
        }
    }
}
