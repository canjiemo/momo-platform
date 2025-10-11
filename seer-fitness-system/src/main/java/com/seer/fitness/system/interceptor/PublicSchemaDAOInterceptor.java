package com.seer.fitness.system.interceptor;

import com.seer.fitness.framework.annotation.PublicSchema;
import com.seer.fitness.system.tenant.PublicSchemaContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Public Schema DAO级别拦截器
 * <p>
 * 作用：拦截 BaseDao 的所有方法，检查操作的实体类是否标记了 @PublicSchema 注解
 * 如果实体类标记了注解，则该次 DAO 操作路由到 public schema
 * <p>
 * 工作流程：
 * 1. 拦截 BaseDao 方法调用
 * 2. 检查方法参数中的实体类是否有 @PublicSchema 注解
 * 3. 如果有，调用 PublicSchemaContext.enter()
 * 4. 执行 DAO 操作
 * 5. 调用 PublicSchemaContext.exit() 恢复上下文
 * <p>
 * 拦截器顺序：
 * - Order=6：在 PublicSchemaMethodInterceptor(Order=5) 之后执行
 * - 确保方法级别的 @PublicSchema 优先级更高
 * <p>
 * 使用场景：
 * 1. 实体类级别：
 *    {@code @PublicSchema(reason = "全局字典数据")}
 *    {@code @Entity}
 *    public class SysDictData { }
 *
 *    // 开发者调用时无需关心路由
 *    baseDao.queryListForSql(sql, params, SysDictData.class);  // 自动路由到 public
 *
 * 2. 优先级：
 *    - 方法级别 @PublicSchema > 实体类级别 @PublicSchema > 租户上下文
 *    - 如果外层方法已经设置了 PublicSchemaContext，则跳过实体类检查
 *
 * @author seer-fitness
 */
@Aspect
@Component
@Order(6)
@Slf4j
public class PublicSchemaDAOInterceptor {

    /**
     * 定义切点：拦截 BaseDao 的所有 public 方法
     * <p>
     * 拦截目标：
     * - io.github.mocanjie.base.myjpa.dao.BaseDao 的所有方法
     * - 包括 queryListForSql, querySingleForSql, insertPO, updatePO 等
     */
    @Pointcut("execution(public * io.github.mocanjie.base.myjpa.dao.BaseDao+.*(..))")
    public void baseDaoMethod() {
    }

    /**
     * 环绕通知：检查实体类注解并设置上下文
     *
     * @param joinPoint 切点信息
     * @return DAO 方法执行结果
     * @throws Throwable DAO 方法执行异常
     */
    @Around("baseDaoMethod()")
    public Object aroundBaseDaoMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // 检查是否已经在 PublicSchemaContext 中
        // 如果方法级别已经设置了 @PublicSchema，则无需检查实体类
        if (PublicSchemaContext.isForcePublic()) {
            log.debug("PublicSchemaContext already active (method level), skip entity check");
            return joinPoint.proceed();
        }

        // 检查方法参数中是否有实体类，并且实体类是否标记了 @PublicSchema
        Class<?> entityClass = findEntityClassInArguments(joinPoint.getArgs());

        if (entityClass != null && entityClass.isAnnotationPresent(PublicSchema.class)) {
            // 实体类标记了 @PublicSchema，进入 public schema 上下文
            PublicSchema annotation = entityClass.getAnnotation(PublicSchema.class);
            String reason = annotation.reason();

            try {
                PublicSchemaContext.enter();

                log.debug("@PublicSchema entity detected: {}, reason={}, executing DAO operation in public schema",
                        entityClass.getSimpleName(), reason);

                // 执行 DAO 操作
                return joinPoint.proceed();

            } finally {
                PublicSchemaContext.exit();
            }

        } else {
            // 实体类未标记 @PublicSchema，正常执行（路由到租户 schema）
            return joinPoint.proceed();
        }
    }

    /**
     * 从方法参数中查找实体类
     * <p>
     * 检查规则：
     * 1. 如果参数是 Class 类型，且该 Class 有 @PublicSchema 注解，返回该 Class
     * 2. 如果参数是实体对象，且该对象的 Class 有 @PublicSchema 注解，返回该 Class
     *
     * @param args 方法参数列表
     * @return 实体类（如果找到），否则返回 null
     */
    private Class<?> findEntityClassInArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        for (Object arg : args) {
            if (arg == null) {
                continue;
            }

            // 情况1：参数本身就是 Class 对象（如 baseDao.queryListForSql(sql, params, SysDictData.class)）
            if (arg instanceof Class) {
                Class<?> clazz = (Class<?>) arg;
                if (clazz.isAnnotationPresent(PublicSchema.class)) {
                    return clazz;
                }
            }

            // 情况2：参数是实体对象（如 baseDao.insertPO(dictData, true)）
            Class<?> argClass = arg.getClass();
            if (argClass.isAnnotationPresent(PublicSchema.class)) {
                return argClass;
            }
        }

        return null;
    }
}
