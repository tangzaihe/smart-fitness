package com.aseantec.smartfitness.api.controller;

import com.aseantec.smartfitness.auth.dto.LoginRequest;
import com.aseantec.smartfitness.auth.dto.RefreshRequest;
import com.aseantec.smartfitness.auth.dto.RegisterRequest;
import com.aseantec.smartfitness.auth.service.AuthService;
import com.aseantec.smartfitness.auth.vo.TokenVO;
import com.aseantec.smartfitness.common.context.UserContext;
import com.aseantec.smartfitness.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 注册 / 登录 / 刷新。除本资源外均需 Bearer。
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "邮箱注册并返回双 token")
    public Result<TokenVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "登录；失败累计锁定")
    public Result<TokenVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "refresh tokens")
    public Result<TokenVO> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "作废 refresh；access 仍待 TTL")
    public Result<Void> logout(@RequestBody(required = false) RefreshRequest request) {
        String refresh = request == null ? null : request.getRefreshToken();
        authService.logout(UserContext.require(), refresh);
        return Result.ok();
    }
}
