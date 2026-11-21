package com.laimengchao.blog.module.auth.controller;

import com.laimengchao.blog.common.R;
import com.laimengchao.blog.module.auth.dto.RoleSaveRequest;
import com.laimengchao.blog.module.auth.entity.SysPermission;
import com.laimengchao.blog.module.auth.entity.SysRole;
import com.laimengchao.blog.module.auth.service.AuthService;
import com.laimengchao.blog.security.Permit;
import com.laimengchao.blog.security.PermissionCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blog/admin/roles")
@RequiredArgsConstructor
@Tag(name = "后台-角色", description = "博客自有角色与按钮级权限分配")
public class AdminRoleController {

    private final AuthService authService;

    @GetMapping
    @Operation(summary = "角色列表（含已授权限）")
    @Permit(PermissionCodes.ROLE_LIST)
    public R<List<SysRole>> list() {
        return R.ok(authService.listRoles());
    }

    @PostMapping
    @Operation(summary = "新增或编辑角色")
    @Permit({PermissionCodes.ROLE_CREATE, PermissionCodes.ROLE_EDIT})
    public R<Long> save(@RequestBody RoleSaveRequest request) {
        return R.ok(authService.saveRole(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    @Permit(PermissionCodes.ROLE_DELETE)
    public R<Void> delete(@PathVariable Long id) {
        authService.deleteRole(id);
        return R.ok();
    }

    @GetMapping("/permissions")
    @Operation(summary = "权限码清单（角色授权用）")
    @Permit(PermissionCodes.ROLE_LIST)
    public R<List<SysPermission>> permissions() {
        return R.ok(authService.listPermissions());
    }
}
