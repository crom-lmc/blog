package com.laimengchao.blog.module.auth.controller;

import com.laimengchao.blog.common.R;
import com.laimengchao.blog.module.auth.dto.UserSaveRequest;
import com.laimengchao.blog.module.auth.entity.SysUser;
import com.laimengchao.blog.module.auth.service.AuthService;
import com.laimengchao.blog.security.Permit;
import com.laimengchao.blog.security.PermissionCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blog/admin/users")
@RequiredArgsConstructor
@Tag(name = "后台-账号", description = "博客自有账号管理")
public class AdminUserController {

    private final AuthService authService;

    @GetMapping
    @Operation(summary = "账号列表")
    @Permit(PermissionCodes.USER_LIST)
    public R<List<SysUser>> list() {
        return R.ok(authService.listUsers());
    }

    @PostMapping
    @Operation(summary = "新增或编辑账号")
    @Permit({PermissionCodes.USER_CREATE, PermissionCodes.USER_EDIT})
    public R<Long> save(@RequestBody UserSaveRequest request) {
        return R.ok(authService.saveUser(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除账号")
    @Permit(PermissionCodes.USER_DELETE)
    public R<Void> delete(@PathVariable Long id) {
        authService.deleteUser(id);
        return R.ok();
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "重置密码")
    @Permit(PermissionCodes.USER_RESET_PWD)
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        authService.resetPassword(id, body == null ? null : body.get("password"));
        return R.ok();
    }
}
