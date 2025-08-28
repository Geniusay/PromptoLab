package io.github.timemachinelab.config;

import io.github.timemachinelab.annotation.Log;
import io.github.timemachinelab.util.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Log interceptor for automatic logging
 * Handles methods annotated with @Log
 */
@Slf4j
@Component
public class LogInterceptor implements HandlerInterceptor {
    
    private static final String START_TIME_ATTR = "startTime";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Log logAnnotation = handlerMethod.getMethodAnnotation(Log.class);
            
            if (logAnnotation != null) {
                // Record start time
                request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
            }
        }
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Log logAnnotation = handlerMethod.getMethodAnnotation(Log.class);
            
            if (logAnnotation != null) {
                Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
                if (startTime != null) {
                    long executionTime = System.currentTimeMillis() - startTime;
                    String url = request.getRequestURI();
                    String userId = UserContextUtil.getCurrentUserId();
                    
                    if (userId != null && !userId.trim().isEmpty()) {
                        log.info("API Access - URL: {}, UserId: {}, ExecutionTime: {}ms", url, userId, executionTime);
                    } else {
                        log.info("API Access - URL: {}, ExecutionTime: {}ms", url, executionTime);
                    }
                }
            }
        }
    }
}