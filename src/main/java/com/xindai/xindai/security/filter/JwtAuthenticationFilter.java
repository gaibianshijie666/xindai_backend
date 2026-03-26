package com.xindai.xindai.security.filter;

import com.xindai.xindai.modules.admin.mapper.AdminUserMapper;
import com.xindai.xindai.modules.enterprise.entity.EnterpriseUser;
import com.xindai.xindai.modules.enterprise.mapper.EnterpriseUserMapper;
import com.xindai.xindai.security.jwt.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter implements Ordered {

    private final JwtUtils jwtUtils;
    private final AdminUserMapper adminUserMapper;
    private final EnterpriseUserMapper enterpriseUserMapper;

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
            try {
                Long userId = jwtUtils.getUserId(token);
                String userType = jwtUtils.getUserType(token);

                request.setAttribute("userId", userId);

                switch (userType != null ? userType : "USER") {
                    case "ADMIN" -> handleAdmin(token, request);
                    case "ENTERPRISE", "enterprise" -> handleEnterprise(token, request);
                    default -> handleUser(token, request);
                }
            } catch (Exception e) {
                log.warn("JWT authentication failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private void handleUser(String token, HttpServletRequest request) {
        Long userId = jwtUtils.getUserId(token);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void handleAdmin(String token, HttpServletRequest request) {
        Long userId = jwtUtils.getUserId(token);

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        var admin = adminUserMapper.selectById(userId);
        if (admin != null && "SUPER_ADMIN".equals(admin.getRole())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId, null, authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void handleEnterprise(String token, HttpServletRequest request) {
        Long enterpriseId = jwtUtils.getEnterpriseId(token);
        Long userId = jwtUtils.getUserId(token);

        request.setAttribute("enterpriseId", enterpriseId);

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ENTERPRISE"));

        if (userId != null) {
            EnterpriseUser user = enterpriseUserMapper.selectById(userId);
            if (user != null) {
                if (user.getRole() != null && user.getRole() == 1) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_ENTERPRISE_ADMIN"));
                } else {
                    authorities.add(new SimpleGrantedAuthority("ROLE_ENTERPRISE_OPERATOR"));
                }
            }
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                enterpriseId, null, authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
