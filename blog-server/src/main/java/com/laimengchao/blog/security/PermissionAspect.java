package com.laimengchao.blog.security;

import com.laimengchao.blog.common.BizException;
import com.laimengchao.blog.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/**
 * 按钮级权限切面：未登录 401 → 超级管理员放行 → 按 {@link Permit#logical()} 判定权限码。
 * 权限码唯一真相源是博客自己的 t_sys_permission / t_sys_role_permission。
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(com.laimengchao.blog.security.Permit)")
    public void check(JoinPoint joinPoint) {
        SecurityUser user = SecurityUtils.currentUser().orElse(null);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (user.isSuperAdmin()) {
            return;
        }
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Permit permit = method.getAnnotation(Permit.class);
        Set<String> owned = user.getPermissions();
        boolean ok = switch (permit.logical()) {
            case ALL -> Arrays.stream(permit.value()).allMatch(owned::contains);
            case ANY -> Arrays.stream(permit.value()).anyMatch(owned::contains);
        };
        if (!ok) {
            log.warn("权限不足：账户={} 需要={} 拥有={}", user.getUsername(),
                    Arrays.toString(permit.value()), owned);
            throw new BizException(ErrorCode.FORBIDDEN, "无权限：" + String.join("/", permit.value()));
        }
    }
}
