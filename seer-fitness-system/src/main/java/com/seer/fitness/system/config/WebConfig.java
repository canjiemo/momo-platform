package com.seer.fitness.system.config;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import com.seer.fitness.system.security.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Web配置类
 * - 配置认证拦截器
 * - 配置 FastJSON2 序列化（Long → String 解决前端精度丢失）
 *
 * @author seer-fitness
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    /**
     * 添加拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**");
    }

    /**
     * 配置 FastJSON2 消息转换器
     * 核心功能：Long 类型序列化为字符串，解决前端 JavaScript 精度丢失问题
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();

        // FastJSON2 配置
        FastJsonConfig config = new FastJsonConfig();

        // ⭐ 序列化配置：Long 类型序列化为字符串（解决前端精度丢失）
        config.setWriterFeatures(
            JSONWriter.Feature.WriteLongAsString,
            JSONWriter.Feature.WriteNulls,
            JSONWriter.Feature.WriteMapNullValue
        );

        // 日期格式
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");

        converter.setFastJsonConfig(config);
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));

        // 添加到转换器列表的首位
        converters.add(0, converter);
    }
}