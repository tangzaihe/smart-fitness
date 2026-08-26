package com.aseantec.smartfitness.api.security;

import com.aseantec.smartfitness.auth.service.AuthService;
import com.aseantec.smartfitness.auth.service.JwtService;
import com.aseantec.smartfitness.common.context.CurrentUser;
import com.aseantec.smartfitness.common.context.UserContext;
import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import com.aseantec.smartfitness.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/v1/auth/")
                || path.equals("/v1/health")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new BizException(ErrorCode.UNAUTHORIZED);
            }
            boolean admin = request.getRequestURI().startsWith("/v1/admin");
            CurrentUser user = jwtService.parse(header.substring(7), admin);
            if (authService.isBlacklisted(user.getTokenId())) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "token revoked");
            }
            UserContext.set(user);
            filterChain.doFilter(request, response);
        } catch (BizException ex) {
            response.setStatus(ex.getErrorCode().getHttpStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Result.fail(ex.getErrorCode(), ex.getMessage()));
        } finally {
            UserContext.clear();
        }
    }
}
