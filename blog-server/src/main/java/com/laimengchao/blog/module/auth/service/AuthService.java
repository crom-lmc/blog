package com.laimengchao.blog.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laimengchao.blog.common.BizException;
import com.laimengchao.blog.common.BlogErrorCode;
import com.laimengchao.blog.common.ErrorCode;
import com.laimengchao.blog.module.auth.dto.ChangePasswordRequest;
import com.laimengchao.blog.module.auth.dto.LoginRequest;
import com.laimengchao.blog.module.auth.dto.LoginVO;
import com.laimengchao.blog.module.auth.dto.RoleSaveRequest;
import com.laimengchao.blog.module.auth.dto.UserSaveRequest;
import com.laimengchao.blog.module.auth.entity.*;
import com.laimengchao.blog.module.auth.mapper.*;
import com.laimengchao.blog.security.JwtService;
import com.laimengchao.blog.security.PermissionCodes;
import com.laimengchao.blog.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 博客自有账号与 RBAC。权限每次现查库，撤权即时生效。 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginVO login(LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername())
                || !StringUtils.hasText(request.getPassword())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户名或密码不能为空");
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername().trim()).last("limit 1"));
        if (user == null) {
            throw new BizException(BlogErrorCode.USER_NOT_FOUND);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(BlogErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(BlogErrorCode.PASSWORD_ERROR);
        }
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        Set<String> permissions = permissionsOf(user);
        boolean superAdmin = isSuperAdmin(user);
        return new LoginVO(jwtService.issue(user.getId(), user.getUsername()), user.getId(),
                user.getUsername(), user.getNickname(), user.getAvatar(), superAdmin, permissions);
    }

    /** 供 JWT 过滤器每次请求重建主体；用户被禁用时直接返回 null。 */
    public SecurityUser loadPrincipal(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            return null;
        }
        return SecurityUser.of(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(),
                user.getEmail(), isSuperAdmin(user), permissionsOf(user));
    }

    private boolean isSuperAdmin(SysUser user) {
        if ("admin".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        List<SysRole> roles = rolesOf(user.getId());
        return roles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()));
    }

    private Set<String> permissionsOf(SysUser user) {
        if (isSuperAdmin(user)) {
            return Set.of(PermissionCodes.ALL);
        }
        List<SysRole> roles = rolesOf(user.getId());
        if (roles.isEmpty()) {
            return Set.of();
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                        .in(SysRolePermission::getRoleId, roleIds)).stream()
                .map(SysRolePermission::getPermissionCode)
                .collect(Collectors.toSet());
    }

    private List<SysRole> rolesOf(Long userId) {
        List<SysUserRole> links = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        if (links.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectBatchIds(links.stream().map(SysUserRole::getRoleId).toList());
    }

    /* ==================== 用户管理 ==================== */

    public List<SysUser> listUsers() {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .orderByAsc(SysUser::getId));
        users.forEach(u -> {
            u.setPasswordHash(null);
            u.setRoleCodes(rolesOf(u.getId()).stream().map(SysRole::getCode).toList());
        });
        return users;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveUser(UserSaveRequest req) {
        if (!StringUtils.hasText(req.getUsername())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户名不能为空");
        }
        SysUser user = req.getId() == null ? new SysUser() : userMapper.selectById(req.getId());
        if (user == null) {
            throw new BizException(BlogErrorCode.USER_NOT_FOUND);
        }
        SysUser exists = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername().trim())
                .ne(user.getId() != null, SysUser::getId, user.getId()).last("limit 1"));
        if (exists != null) {
            throw new BizException(BlogErrorCode.USERNAME_EXISTS);
        }
        user.setUsername(req.getUsername().trim());
        user.setNickname(req.getNickname());
        user.setEmail(req.getEmail());
        if (req.getStatus() != null) user.setStatus(req.getStatus());
        if (StringUtils.hasText(req.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }
        if (user.getId() == null) {
            if (!StringUtils.hasText(req.getPassword())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "新建账号必须设置密码");
            }
            user.setStatus(user.getStatus() == null ? 1 : user.getStatus());
            user.setRole("author");
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        if (req.getRoleIds() != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
            req.getRoleIds().stream().filter(Objects::nonNull).forEach(roleId ->
                    userRoleMapper.insert(new SysUserRole(user.getId(), roleId)));
        }
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(BlogErrorCode.USER_NOT_FOUND);
        }
        if ("admin".equalsIgnoreCase(user.getUsername()) || "admin".equalsIgnoreCase(user.getRole())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "内置管理员账号不允许删除");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String password) {
        if (!StringUtils.hasText(password) || password.length() < 6) {
            throw new BizException(ErrorCode.PARAM_ERROR, "密码至少 6 位");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(BlogErrorCode.USER_NOT_FOUND);
        }
        user.setPasswordHash(passwordEncoder.encode(password));
        userMapper.updateById(user);
    }

    /** 当前登录人自助修改密码：先校验原密码，再更新为新密码。 */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest req) {
        if (req == null || !StringUtils.hasText(req.getOldPassword()) || !StringUtils.hasText(req.getNewPassword())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "原密码和新密码均不能为空");
        }
        if (req.getNewPassword().length() < 6) {
            throw new BizException(ErrorCode.PARAM_ERROR, "新密码至少 6 位");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(BlogErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
            throw new BizException(BlogErrorCode.OLD_PASSWORD_ERROR);
        }
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);
    }

    /* ==================== 角色管理 ==================== */

    public List<SysRole> listRoles() {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSortNo));
        roles.forEach(role -> role.setPermissions(permissionCodesOf(role.getId())));
        return roles;
    }

    private List<String> permissionCodesOf(Long roleId) {
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, roleId)).stream()
                .map(SysRolePermission::getPermissionCode).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveRole(RoleSaveRequest req) {
        if (!StringUtils.hasText(req.getCode()) || !StringUtils.hasText(req.getName())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色编码与名称不能为空");
        }
        SysRole role = req.getId() == null ? new SysRole() : roleMapper.selectById(req.getId());
        if (role == null) {
            throw new BizException(BlogErrorCode.DATA_NOT_FOUND);
        }
        role.setCode(req.getCode().trim().toUpperCase(Locale.ROOT));
        role.setName(req.getName());
        role.setDescription(req.getDescription());
        role.setSortNo(req.getSortNo() == null ? 0 : req.getSortNo());
        if (role.getId() == null) {
            roleMapper.insert(role);
        } else {
            roleMapper.updateById(role);
        }
        if (req.getPermissions() != null && !"ADMIN".equalsIgnoreCase(role.getCode())) {
            rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                    .eq(SysRolePermission::getRoleId, role.getId()));
            req.getPermissions().stream().filter(StringUtils::hasText).forEach(code ->
                    rolePermissionMapper.insert(new SysRolePermission(role.getId(), code)));
        }
        return role.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(BlogErrorCode.DATA_NOT_FOUND);
        }
        if ("ADMIN".equalsIgnoreCase(role.getCode())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "内置超级管理员角色不允许删除");
        }
        roleMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
    }

    public List<SysPermission> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSortNo));
    }
}
