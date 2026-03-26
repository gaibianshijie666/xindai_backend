package com.xindai.xindai.security.filter;

import com.xindai.xindai.modules.admin.entity.AdminUser;
import com.xindai.xindai.modules.admin.mapper.AdminUserMapper;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseUser;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseUserMapper;
import com.xindai.xindai.security.jwt.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtAuthenticationFilter 单元测试")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AdminUserMapper adminUserMapper;

    @Mock
    private EnterpriseUserMapper enterpriseUserMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String INVALID_TOKEN = "invalid.token";
    private static final Long USER_ID = 1L;
    private static final Long ENTERPRISE_ID = 10L;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("普通用户Token认证测试")
    class UserTokenTests {

        @Test
        @DisplayName("普通用户Token - 设置ROLE_USER角色")
        void doFilter_withUserToken_setsRoleUser() throws Exception {
            // Given
            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            when(jwtUtils.validateToken(VALID_TOKEN)).thenReturn(true);
            when(jwtUtils.getUserId(VALID_TOKEN)).thenReturn(USER_ID);
            when(jwtUtils.getUserType(VALID_TOKEN)).thenReturn("USER");

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(authentication, "Authentication should be set");
            assertEquals(USER_ID, authentication.getPrincipal());
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_USER")),
                    "Should have ROLE_USER authority");
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("管理员Token认证测试")
    class AdminTokenTests {

        @Test
        @DisplayName("管理员Token - 设置ROLE_ADMIN角色")
        void doFilter_withAdminToken_setsRoleAdmin() throws Exception {
            // Given
            AdminUser adminUser = new AdminUser();
            adminUser.setId(USER_ID);
            adminUser.setRole("ADMIN");

            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            when(jwtUtils.validateToken(VALID_TOKEN)).thenReturn(true);
            when(jwtUtils.getUserId(VALID_TOKEN)).thenReturn(USER_ID);
            when(jwtUtils.getUserType(VALID_TOKEN)).thenReturn("ADMIN");
            when(adminUserMapper.selectById(USER_ID)).thenReturn(adminUser);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(authentication, "Authentication should be set");
            assertEquals(USER_ID, authentication.getPrincipal());
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")),
                    "Should have ROLE_ADMIN authority");
            assertFalse(authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN")),
                    "Should NOT have ROLE_SUPER_ADMIN authority");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("超级管理员Token - 设置ROLE_SUPER_ADMIN角色")
        void doFilter_withSuperAdminToken_setsRoleSuperAdmin() throws Exception {
            // Given
            AdminUser superAdmin = new AdminUser();
            superAdmin.setId(USER_ID);
            superAdmin.setRole("SUPER_ADMIN");

            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            when(jwtUtils.validateToken(VALID_TOKEN)).thenReturn(true);
            when(jwtUtils.getUserId(VALID_TOKEN)).thenReturn(USER_ID);
            when(jwtUtils.getUserType(VALID_TOKEN)).thenReturn("ADMIN");
            when(adminUserMapper.selectById(USER_ID)).thenReturn(superAdmin);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(authentication, "Authentication should be set");
            assertEquals(USER_ID, authentication.getPrincipal());
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN")),
                    "Should have ROLE_SUPER_ADMIN authority");
            assertFalse(authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")),
                    "Should NOT have ROLE_ADMIN authority");
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("企业用户Token认证测试")
    class EnterpriseTokenTests {

        @Test
        @DisplayName("企业用户Token - 设置ROLE_ENTERPRISE和ROLE_ENTERPRISE_OPERATOR角色")
        void doFilter_withEnterpriseToken_setsRoleEnterprise() throws Exception {
            // Given
            EnterpriseUser enterpriseUser = new EnterpriseUser();
            enterpriseUser.setId(USER_ID);
            enterpriseUser.setEnterpriseId(ENTERPRISE_ID);
            enterpriseUser.setRole(0); // operator

            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            when(jwtUtils.validateToken(VALID_TOKEN)).thenReturn(true);
            when(jwtUtils.getUserId(VALID_TOKEN)).thenReturn(USER_ID);
            when(jwtUtils.getUserType(VALID_TOKEN)).thenReturn("ENTERPRISE");
            when(jwtUtils.getEnterpriseId(VALID_TOKEN)).thenReturn(ENTERPRISE_ID);
            when(enterpriseUserMapper.selectById(USER_ID)).thenReturn(enterpriseUser);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(authentication, "Authentication should be set");
            assertEquals(ENTERPRISE_ID, authentication.getPrincipal());
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ENTERPRISE")),
                    "Should have ROLE_ENTERPRISE authority");
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ENTERPRISE_OPERATOR")),
                    "Should have ROLE_ENTERPRISE_OPERATOR authority");
            verify(request).setAttribute("enterpriseId", ENTERPRISE_ID);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("企业管理员Token - 设置ROLE_ENTERPRISE和ROLE_ENTERPRISE_ADMIN角色")
        void doFilter_withEnterpriseAdminToken_setsRoleEnterpriseAdmin() throws Exception {
            // Given
            EnterpriseUser enterpriseAdmin = new EnterpriseUser();
            enterpriseAdmin.setId(USER_ID);
            enterpriseAdmin.setEnterpriseId(ENTERPRISE_ID);
            enterpriseAdmin.setRole(1); // admin

            when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
            when(jwtUtils.validateToken(VALID_TOKEN)).thenReturn(true);
            when(jwtUtils.getUserId(VALID_TOKEN)).thenReturn(USER_ID);
            when(jwtUtils.getUserType(VALID_TOKEN)).thenReturn("ENTERPRISE");
            when(jwtUtils.getEnterpriseId(VALID_TOKEN)).thenReturn(ENTERPRISE_ID);
            when(enterpriseUserMapper.selectById(USER_ID)).thenReturn(enterpriseAdmin);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(authentication, "Authentication should be set");
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ENTERPRISE")),
                    "Should have ROLE_ENTERPRISE authority");
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ENTERPRISE_ADMIN")),
                    "Should have ROLE_ENTERPRISE_ADMIN authority");
            assertFalse(authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ENTERPRISE_OPERATOR")),
                    "Should NOT have ROLE_ENTERPRISE_OPERATOR authority");
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("无效Token测试")
    class InvalidTokenTests {

        @Test
        @DisplayName("无效Token - 不设置认证信息")
        void doFilter_withInvalidToken_doesNotSetAuthentication() throws Exception {
            // Given
            when(request.getHeader("Authorization")).thenReturn("Bearer " + INVALID_TOKEN);
            when(jwtUtils.validateToken(INVALID_TOKEN)).thenReturn(false);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertNull(authentication, "Authentication should NOT be set for invalid token");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("无Authorization头 - 不设置认证信息")
        void doFilter_noAuthorizationHeader_doesNotSetAuthentication() throws Exception {
            // Given
            when(request.getHeader("Authorization")).thenReturn(null);

            // When
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertNull(authentication, "Authentication should NOT be set when no Authorization header");
            verify(filterChain).doFilter(request, response);
        }
    }
}
