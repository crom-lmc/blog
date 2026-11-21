package com.laimengchao.blog.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** 当前登录主体工具类。 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<SecurityUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static Long currentUserId() {
        return currentUser().map(SecurityUser::getAccountId).orElse(null);
    }

    public static Optional<String> currentUsername() {
        return currentUser().map(SecurityUser::getUsername);
    }

    public static boolean isSuperAdmin() {
        return currentUser().map(SecurityUser::isSuperAdmin).orElse(false);
    }

    public static boolean isAdmin() {
        return currentUser().map(SecurityUser::isAdmin).orElse(false);
    }
}
