package com.jbm.cluster.core.annotation;

import java.lang.annotation.*;

/**
 * 内部 Feign 调用标记注解。
 * <p>
 * Token 合法性由全局 {@code SaOAuthFilterAuthStrategy} 统一校验（用户 AccessToken 或服务 ClientToken），
 * 本注解不再做 from-source / Id-Token 等额外控制；仅在 ClientToken 场景下跳过 {@code @SaCheckPermission}。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InnerAuth {
    /**
     * 是否从请求头恢复用户信息
     */
    boolean isUser() default false;
}
