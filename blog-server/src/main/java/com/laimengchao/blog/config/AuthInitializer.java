package com.laimengchao.blog.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laimengchao.blog.module.auth.entity.*;
import com.laimengchao.blog.module.auth.mapper.*;
import com.laimengchao.blog.security.PermissionCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 首次启动播种：全量权限码、ADMIN/AUTHOR 角色、默认管理员 admin。
 * 全部幂等，已存在的行不覆盖——管理员在运行期改过密码后重启不会被重置。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInitializer implements ApplicationRunner {

    private final SysPermissionMapper permissionMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${blog.admin.default-password:G#191975}")
    private String defaultPassword;

    private static final Map<String, String> NAMES = new LinkedHashMap<>();

    static {
        NAMES.put(PermissionCodes.MENU_DASHBOARD, "概览菜单");
        NAMES.put(PermissionCodes.MENU_ARTICLE, "文章菜单");
        NAMES.put(PermissionCodes.MENU_CATEGORY, "分类菜单");
        NAMES.put(PermissionCodes.MENU_TAG, "标签菜单");
        NAMES.put(PermissionCodes.MENU_COMMENT, "评论菜单");
        NAMES.put(PermissionCodes.MENU_MEDIA, "媒体菜单");
        NAMES.put(PermissionCodes.MENU_THEME, "主题菜单");
        NAMES.put(PermissionCodes.MENU_SETTING, "设置菜单");
        NAMES.put(PermissionCodes.MENU_LOG, "日志菜单");
        NAMES.put(PermissionCodes.MENU_USER, "账号菜单");
        NAMES.put(PermissionCodes.MENU_ROLE, "角色菜单");
        NAMES.put(PermissionCodes.ARTICLE_LIST, "文章列表");
        NAMES.put(PermissionCodes.ARTICLE_CREATE, "新建文章");
        NAMES.put(PermissionCodes.ARTICLE_EDIT, "编辑文章");
        NAMES.put(PermissionCodes.ARTICLE_PUBLISH, "发布文章");
        NAMES.put(PermissionCodes.ARTICLE_DELETE, "删除文章");
        NAMES.put(PermissionCodes.CATEGORY_LIST, "分类列表");
        NAMES.put(PermissionCodes.CATEGORY_EDIT, "编辑分类");
        NAMES.put(PermissionCodes.CATEGORY_DELETE, "删除分类");
        NAMES.put(PermissionCodes.TAG_LIST, "标签列表");
        NAMES.put(PermissionCodes.TAG_EDIT, "编辑标签");
        NAMES.put(PermissionCodes.TAG_DELETE, "删除标签");
        NAMES.put(PermissionCodes.COMMENT_LIST, "评论列表");
        NAMES.put(PermissionCodes.COMMENT_AUDIT, "审核评论");
        NAMES.put(PermissionCodes.COMMENT_DELETE, "删除评论");
        NAMES.put(PermissionCodes.COMMENT_REPLY, "回复评论");
        NAMES.put(PermissionCodes.MEDIA_LIST, "媒体列表");
        NAMES.put(PermissionCodes.MEDIA_EDIT, "编辑媒体");
        NAMES.put(PermissionCodes.MEDIA_DELETE, "删除媒体");
        NAMES.put(PermissionCodes.MEDIA_UPLOAD, "上传文件");
        NAMES.put(PermissionCodes.THEME_LIST, "主题列表");
        NAMES.put(PermissionCodes.THEME_EDIT, "编辑主题");
        NAMES.put(PermissionCodes.THEME_DELETE, "删除主题");
        NAMES.put(PermissionCodes.THEME_PUBLISH, "启用主题");
        NAMES.put(PermissionCodes.SETTING_VIEW, "查看设置");
        NAMES.put(PermissionCodes.SETTING_EDIT, "修改设置");
        NAMES.put(PermissionCodes.LOG_LIST, "操作日志");
        NAMES.put(PermissionCodes.LOG_CLEAN, "清理日志");
        NAMES.put(PermissionCodes.STAT_VIEW, "统计查看");
        NAMES.put(PermissionCodes.USER_LIST, "账号列表");
        NAMES.put(PermissionCodes.USER_CREATE, "新建账号");
        NAMES.put(PermissionCodes.USER_EDIT, "编辑账号");
        NAMES.put(PermissionCodes.USER_DELETE, "删除账号");
        NAMES.put(PermissionCodes.USER_RESET_PWD, "重置密码");
        NAMES.put(PermissionCodes.ROLE_LIST, "角色列表");
        NAMES.put(PermissionCodes.ROLE_CREATE, "新建角色");
        NAMES.put(PermissionCodes.ROLE_EDIT, "编辑角色");
        NAMES.put(PermissionCodes.ROLE_DELETE, "删除角色");
    }

    @Override
    public void run(ApplicationArguments args) {
        initPermissions();
        SysRole adminRole = initRole("ADMIN", "超级管理员", 0);
        initRole("AUTHOR", "作者", 10);
        initAdmin(adminRole);
    }

    private void initPermissions() {
        int created = 0;
        int order = 0;
        for (String code : PermissionCodes.ALL) {
            Long exists = permissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()
                    .eq(SysPermission::getCode, code));
            if (exists != null && exists > 0) {
                continue;
            }
            SysPermission permission = new SysPermission();
            permission.setCode(code);
            permission.setName(NAMES.getOrDefault(code, code));
            permission.setType(code.contains(":") ? "BUTTON" : "MENU");
            permission.setSortNo(++order);
            permissionMapper.insert(permission);
            created++;
        }
        log.info("博客权限码初始化完成，新增 {} 条", created);
    }

    private SysRole initRole(String code, String name, int sort) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, code));
        if (role == null) {
            role = new SysRole();
            role.setCode(code);
            role.setName(name);
            role.setDescription(code.equals("ADMIN") ? "内置超级管理员，拥有全部权限" : "内容作者");
            role.setSortNo(sort);
            roleMapper.insert(role);
        }
        return role;
    }

    private void initAdmin(SysRole adminRole) {
        SysUser admin = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "admin").last("limit 1"));
        if (admin == null) {
            admin = new SysUser();
            admin.setUsername("admin");
            admin.setNickname("管理员");
            admin.setRole("admin");
            admin.setStatus(1);
            admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
            userMapper.insert(admin);
            log.info("已创建博客默认管理员：admin");
        }
        Long count = rolePermissionMapper.selectCount(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, adminRole.getId()));
        if (count == null || count == 0) {
            for (String code : PermissionCodes.ALL) {
                rolePermissionMapper.insert(new SysRolePermission(adminRole.getId(), code));
            }
        }
    }
}
