package com.laimengchao.blog.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 博客本地登录主体。身份来自本库 t_user，权限来自本库角色-权限关联，
 * 与统一账号中心没有任何关系。
 */
@Data
public class SecurityUser implements UserDetails {

    /** t_user.id，文章作者、上传人、操作日志都记它 */
    private Long accountId;
    private String username;
    private String nickname;
    private String avatar;
    private String email;

    /** 超级管理员：越过所有按钮码 */
    private boolean superAdmin;

    private Set<String> permissions = Set.of();
    private Collection<? extends GrantedAuthority> authorities = List.of();

    public static SecurityUser of(Long accountId, String username, String nickname, String avatar,
                                  String email, boolean superAdmin, Set<String> permissions) {
        SecurityUser user = new SecurityUser();
        user.setAccountId(accountId);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setAvatar(avatar);
        user.setEmail(email);
        user.setSuperAdmin(superAdmin);
        user.setPermissions(permissions == null ? Set.of() : permissions);
        user.setAuthorities(user.getPermissions().stream()
                .map(code -> (GrantedAuthority) new SimpleGrantedAuthority("PERM_" + code))
                .collect(Collectors.toList()));
        return user;
    }

    public String getDisplayName() {
        return nickname == null || nickname.isBlank() ? username : nickname;
    }

    public boolean isAdmin() {
        return superAdmin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
