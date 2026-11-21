package com.laimengchao.blog.module.auth.controller;

import com.laimengchao.blog.common.BizException;
import com.laimengchao.blog.common.ErrorCode;
import com.laimengchao.blog.common.R;
import com.laimengchao.blog.module.auth.dto.ChangePasswordRequest;
import com.laimengchao.blog.module.auth.dto.LoginRequest;
import com.laimengchao.blog.module.auth.dto.LoginVO;
import com.laimengchao.blog.module.auth.service.AuthService;
import com.laimengchao.blog.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 博客自有登录：账号来自本库 t_user，令牌由本服务签发。 */
@RestController
@RequestMapping("/api/blog/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "博客本地账号登录与当前用户信息")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "账号密码登录")
    public R<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "当前登录人")
    public R<Map<String, Object>> me() {
        return R.ok(SecurityUtils.currentUser()
                .map(user -> Map.<String, Object>of(
                        "userId", user.getAccountId(),
                        "username", user.getUsername(),
                        "nickname", user.getDisplayName(),
                        "avatar", user.getAvatar() == null ? "" : user.getAvatar(),
                        "superAdmin", user.isSuperAdmin(),
                        "permissions", user.getPermissions()))
                .orElse(Map.of()));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录（无状态令牌由前端丢弃）")
    public R<Void> logout() {
        return R.ok();
    }

    @PostMapping("/change-password")
    @Operation(summary = "当前登录人修改密码")
    public R<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        authService.changePassword(userId, request);
        return R.ok();
    }
}
