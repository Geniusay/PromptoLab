package io.github.timemachinelab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * Web配置类
 * 注册拦截器
 */
@Configuration
public class PromptLabWebConfig implements WebMvcConfigurer {
    
    @Resource
    private UserAuthInterceptor userAuthInterceptor;
    
    @Resource
    private LogInterceptor logInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册日志拦截器
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/api/**");
        
        // 注册用户认证拦截器
        registry.addInterceptor(userAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/user-interaction/user/token");
    }
}