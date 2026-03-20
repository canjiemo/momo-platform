package io.github.canjiemo.momo.framework.annotation;

import io.github.canjiemo.momo.framework.enums.AuthMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限控制注解
 *
 * @author canjiemo@gmail.com
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {

    /**
     * 是否需要登录，默认true
     */
    boolean login() default true;

    /**
     * 需要的权限字符
     */
    String[] permissions() default {};

    /**
     * 需要的角色
     */
    String[] roles() default {};

    /**
     * 权限检查模式：ALL(需要所有权限) 或 ANY(任意一个权限)
     */
    AuthMode mode() default AuthMode.ANY;
}
