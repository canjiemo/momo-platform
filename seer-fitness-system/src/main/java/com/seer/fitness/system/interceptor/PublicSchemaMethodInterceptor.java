package com.seer.fitness.system.interceptor;

import com.seer.fitness.framework.annotation.PublicSchema;
import com.seer.fitness.system.tenant.PublicSchemaContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Public Schema 方法级别拦截器
 * <p>
 * 作用：拦截所有标记了 @PublicSchema 注解的方法，在方法执行期间强制路由到 public schema
 * <p>
 * 工作流程：
 * 1. 方法执行前：检测到 @PublicSchema 注解 → 调用 PublicSchemaContext.enter()
 * 2. 方法执行中：所有数据库操作自动路由到 public schema
 * 3. 方法执行后：调用 PublicSchemaContext.exit() 恢复上下文
 * <p>
 * 拦截器顺序：
 * - Order=5：在 TenantInterceptor(Order=1) 之后执行
 * - 确保租户上下文已设置，但可以被 PublicSchema 覆盖
 * <p>
 * 使用场景：
 * 1. Service方法级别：
 *    {@code @PublicSchema(reason = "查询全局字典")}
 *    public List<DictData> getGlobalDict() { }
 *
 * 2. Controller方法级别：
 *    {@code @PublicSchema(reason = "管理员查看所有租户统计")}
 *    {@code @GetMapping("/admin/statistics")}
 *    public Result getAllTenantsStats() { }
 *
 * 3. 嵌套调用支持：
 *    method1() 标记 @PublicSchema → 调用 method2() (也标记 @PublicSchema)
 *    → 通过 PublicSchemaContext 的计数器机制正确处理
 *
 * @author seer-fitness
 */
@Aspect
@Component
@Order(5)
@Slf4j
public class PublicSchemaMethodInterceptor {

    /**
     * 定义切点：所有标记了 @PublicSchema 注解的方法
     */
    @Pointcut("@annotation(com.seer.fitness.framework.annotation.PublicSchema)")
    public void publicSchemaMethod() {
    }

    /**
     * 环绕通知：在方法执行前后设置/清除 PublicSchemaContext
     *
     * @param joinPoint 切点信息
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("publicSchemaMethod()")
    public Object aroundPublicSchemaMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取 @PublicSchema 注解
        PublicSchema annotation = method.getAnnotation(PublicSchema.class);
        String reason = annotation.reason();
        boolean propagate = annotation.propagate();

        // 获取方法信息（用于日志）
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();

        try {
            // 检查是否传播
            if (!propagate) {
                log.debug("@PublicSchema(propagate=false): {}.{}, 不传播到内部调用",
                        className, methodName);
                // TODO: 未来可以实现更细粒度的控制
                // 当前简化实现：propagate=false 时仍然传播，但记录日志
            }

            // 进入 public schema 上下文
            PublicSchemaContext.enter();

            log.debug("@PublicSchema method entered: {}.{}, reason={}, nested_level={}",
                    className, methodName, reason, PublicSchemaContext.getNestedLevel());

            // 执行目标方法
            return joinPoint.proceed();

        } finally {
            // 退出 public schema 上下文（必须在 finally 块中，确保一定执行）
            PublicSchemaContext.exit();

            log.debug("@PublicSchema method exited: {}.{}, nested_level={}",
                    className, methodName, PublicSchemaContext.getNestedLevel());
        }
    }
}
