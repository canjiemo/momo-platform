package com.seer.fitness.system.aspect;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.system.entity.SysOperationLog;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.service.IOperationLogService;
import com.seer.fitness.system.util.SecurityContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.springframework.web.multipart.MultipartFile;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面
 * 自动记录带有@OperationLog注解的方法执行情况
 *
 * @author seer-fitness
 */
@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    @Autowired
    private IOperationLogService operationLogService;

    /**
     * SpEL表达式解析器
     */
    private final SpelExpressionParser parser = new SpelExpressionParser();

    /**
     * 参数名发现器
     */
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 定义切点：所有带有@OperationLog注解的方法
     */
    @Pointcut("@annotation(com.seer.fitness.system.annotation.OperationLog)")
    public void operationLogPointcut() {}

    /**
     * 环绕通知：记录操作日志
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;

        try {
            // 执行目标方法
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            try {
                // 记录操作日志
                long endTime = System.currentTimeMillis();
                int executionTime = (int) (endTime - startTime);
                recordOperationLog(joinPoint, result, exception, executionTime);
            } catch (Exception e) {
                log.error("记录操作日志失败", e);
            }
        }
    }

    /**
     * 记录操作日志
     *
     * @param joinPoint 连接点
     * @param result 方法执行结果
     * @param exception 异常信息
     * @param executionTime 执行耗时
     */
    private void recordOperationLog(JoinPoint joinPoint, Object result, Throwable exception, int executionTime) {
        try {
            // 获取方法和注解
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            OperationLog operationLog = method.getAnnotation(OperationLog.class);

            if (operationLog == null) {
                return;
            }

            // 创建日志对象
            SysOperationLog logEntity = new SysOperationLog();

            // 设置基本信息
            setBasicInfo(logEntity, operationLog);

            // 设置用户信息
            setUserInfo(logEntity);

            // 设置请求信息
            setRequestInfo(logEntity);

            // 设置业务信息
            setBusinessInfo(logEntity, joinPoint, result, operationLog);

            // 设置请求参数
            if (operationLog.recordRequest()) {
                setRequestParams(logEntity, joinPoint, operationLog.excludeParams());
            }

            // 设置响应数据
            if (operationLog.recordResponse() && result != null) {
                setResponseData(logEntity, result);
            }

            // 设置执行结果
            setExecutionResult(logEntity, exception, executionTime);

            // 保存日志
            if (operationLog.async()) {
                saveLogAsync(logEntity);
            } else {
                operationLogService.save(logEntity);
            }

        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    /**
     * 设置基本信息
     */
    private void setBasicInfo(SysOperationLog logEntity, OperationLog operationLog) {
        logEntity.setOperationType(operationLog.type().getCode());
        logEntity.setModuleName(operationLog.module());
        logEntity.setOperationDesc(operationLog.description());
        logEntity.setCreatedAt(LocalDateTime.now());
    }

    /**
     * 设置用户信息
     */
    private void setUserInfo(SysOperationLog logEntity) {
        try {
            String currentUserIdStr = SecurityContextUtil.getCurrentUserId();
            String currentUsername = SecurityContextUtil.getCurrentUsername();
            String currentRealName = SecurityContextUtil.getCurrentRealName();

            if (currentUserIdStr != null) {
                logEntity.setUserId(Long.valueOf(currentUserIdStr));
            }
            logEntity.setUsername(currentUsername);
            logEntity.setRealName(currentRealName);
        } catch (Exception e) {
            log.debug("获取当前用户信息失败", e);
        }
    }

    /**
     * 设置请求信息
     */
    private void setRequestInfo(SysOperationLog logEntity) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                logEntity.setRequestMethod(request.getMethod());
                logEntity.setRequestUrl(request.getRequestURI());
                logEntity.setIpAddress(getClientIpAddress(request));
                logEntity.setUserAgent(request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            log.debug("获取请求信息失败", e);
        }
    }

    /**
     * 设置业务信息
     */
    private void setBusinessInfo(SysOperationLog logEntity, JoinPoint joinPoint, Object result, OperationLog operationLog) {
        try {
            // 创建SpEL上下文
            EvaluationContext context = createEvaluationContext(joinPoint, result);

            // 解析业务ID
            if (StringUtils.hasText(operationLog.businessId())) {
                String businessId = parseSpelExpression(operationLog.businessId(), context);
                logEntity.setBusinessId(businessId);
            }

            // 解析业务名称
            if (StringUtils.hasText(operationLog.businessName())) {
                String businessName = parseSpelExpression(operationLog.businessName(), context);
                logEntity.setBusinessName(businessName);
            }
        } catch (Exception e) {
            log.debug("解析业务信息失败", e);
        }
    }

    /**
     * 设置请求参数
     */
    private void setRequestParams(SysOperationLog logEntity, JoinPoint joinPoint, String[] excludeParams) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = nameDiscoverer.getParameterNames(signature.getMethod());
            Object[] paramValues = joinPoint.getArgs();

            Map<String, Object> params = new HashMap<>();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    String paramName = paramNames[i];
                    Object paramValue = paramValues[i];

                    // 过滤不可序列化的类型
                    if (isFilterType(paramValue)) {
                        params.put(paramName, paramValue.getClass().getSimpleName());
                        continue;
                    }

                    // 排除敏感参数
                    if (!Arrays.asList(excludeParams).contains(paramName)) {
                        params.put(paramName, paramValue);
                    } else {
                        params.put(paramName, "******");
                    }
                }
            }

            // 使用FastJSON2的特性：禁用循环引用检测，忽略错误
            String paramsJson = JSON.toJSONString(params,
                JSONWriter.Feature.IgnoreErrorGetter,
                JSONWriter.Feature.WriteNulls);
            logEntity.setRequestParams(paramsJson);
        } catch (Exception e) {
            log.debug("序列化请求参数失败", e);
            logEntity.setRequestParams("序列化失败");
        }
    }

    /**
     * 设置响应数据
     */
    private void setResponseData(SysOperationLog logEntity, Object result) {
        try {
            String resultJson = JSON.toJSONString(result,
                JSONWriter.Feature.IgnoreErrorGetter,
                JSONWriter.Feature.WriteNulls);
            logEntity.setResponseData(resultJson);
        } catch (Exception e) {
            log.debug("序列化响应数据失败", e);
            logEntity.setResponseData("序列化失败");
        }
    }

    /**
     * 设置执行结果
     */
    private void setExecutionResult(SysOperationLog logEntity, Throwable exception, int executionTime) {
        logEntity.setExecutionTime(executionTime);
        if (exception == null) {
            logEntity.setOperationResult(1);
        } else {
            logEntity.setOperationResult(0);
            logEntity.setErrorMessage(exception.getMessage());
        }
    }

    /**
     * 创建SpEL求值上下文
     */
    private EvaluationContext createEvaluationContext(JoinPoint joinPoint, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 设置方法参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = nameDiscoverer.getParameterNames(signature.getMethod());
        Object[] paramValues = joinPoint.getArgs();

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], paramValues[i]);
            }
        }

        // 设置返回值
        if (result != null) {
            context.setVariable("result", result);
        }

        return context;
    }

    /**
     * 解析SpEL表达式
     */
    private String parseSpelExpression(String expressionString, EvaluationContext context) {
        try {
            Expression expression = parser.parseExpression(expressionString);
            Object value = expression.getValue(context);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.debug("解析SpEL表达式失败: {}", expressionString, e);
            return null;
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * 判断是否为不可序列化的类型
     * 这些类型会导致JSON序列化失败或产生过大的数据
     */
    private boolean isFilterType(Object obj) {
        if (obj == null) {
            return false;
        }
        return obj instanceof HttpServletRequest
                || obj instanceof HttpServletResponse
                || obj instanceof MultipartFile;
    }

    /**
     * 异步保存日志
     */
    @Async
    public void saveLogAsync(SysOperationLog logEntity) {
        try {
            operationLogService.save(logEntity);
        } catch (Exception e) {
            log.error("异步保存操作日志失败", e);
        }
    }
}