package com.seer.fitness.framework.utils;


import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 全局 Spring Bean 工具类
 * 在任何地方都可以通过类型或名称获取 Spring 管理的 Bean
 */
@Component
public class SpringBeanUtil implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    /**
     * 通过类型获取 Bean
     */
    public static <T> T getBean(Class<T> clazz) {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext 未初始化！");
        }
        return context.getBean(clazz);
    }

    /**
     * 通过名称获取 Bean
     */
    public static Object getBean(String name) {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext 未初始化！");
        }
        return context.getBean(name);
    }

    /**
     * 获取指定类型的所有 Bean
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext 未初始化！");
        }
        return context.getBeansOfType(clazz);
    }

    /**
     * 获取所有被 @Component 注解的 Bean
     */
    public static Map<String, Object> getAllComponentBeans() {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext 未初始化！");
        }
        return context.getBeansWithAnnotation(Component.class);
    }
}
