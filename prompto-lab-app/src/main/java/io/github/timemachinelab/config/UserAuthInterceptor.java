package io.github.timemachinelab.config;

import io.github.timemachinelab.annotation.UserCheck;
import io.github.timemachinelab.entity.User;
import io.github.timemachinelab.exception.BusinessException;
import io.github.timemachinelab.manager.UserManager;
import io.github.timemachinelab.util.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用户认证拦截器
 * 检查标记了@UserCheck注解的接口是否携带有效的userId
 */
@Slf4j
@Component
public class UserAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private UserManager userManager;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只处理Controller方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // 检查方法或类上是否有@UserCheck注解
        boolean needUserCheck = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), UserCheck.class) != null ||
                               AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), UserCheck.class) != null;
        
        if (!needUserCheck) {
            return true;
        }
        
        // 从Cookie中获取userId
        String userId = getUserIdFromCookie(request);
        
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("用户认证失败: Cookie中未找到userId - URI: {}", request.getRequestURI());
            throw new BusinessException(401, "用户未认证，请先获取用户Token");
        }

        User user = userManager.getUser(userId);
        if (user == null) {
            log.warn("用户认证失败: 用户不存在 - userId={}, URI={}", userId, request.getRequestURI());
            throw new BusinessException(401, "用户不存在，请重新获取用户Token");
        }

        // 将用户信息存储到ThreadLocal
        UserContextUtil.setUser(user);
        log.debug("用户认证成功: userId={}, URI={}", userId, request.getRequestURI());
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清除ThreadLocal，避免内存泄漏
        UserContextUtil.clear();
    }
    
    /**
     * 从Cookie中获取userId
     */
    private String getUserIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        
        for (Cookie cookie : cookies) {
            if ("userId".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        
        return null;
    }
}