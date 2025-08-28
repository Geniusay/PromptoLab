package io.github.timemachinelab.config;

import io.github.timemachinelab.annotation.AutoResp;
import io.github.timemachinelab.entity.resp.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 响应包装拦截器
 * 自动将标注了@AutoResp的Controller方法返回的对象包装成ApiResult
 */
@Slf4j
@RestControllerAdvice
public class ResponseWrapperAdvice implements ResponseBodyAdvice<Object> {
    
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 检查方法或类上是否有@AutoResp注解
        boolean hasAutoResp = AnnotationUtils.findAnnotation(returnType.getMethod(), AutoResp.class) != null ||
                             AnnotationUtils.findAnnotation(returnType.getDeclaringClass(), AutoResp.class) != null;
        
        // 如果没有@AutoResp注解，不进行处理
        if (!hasAutoResp) {
            return false;
        }
        
        // 如果已经是ResponseEntity、SseEmitter或ApiResult，不需要包装
        Class<?> returnTypeClass = returnType.getParameterType();
        return !ResponseEntity.class.isAssignableFrom(returnTypeClass) && 
               !SseEmitter.class.isAssignableFrom(returnTypeClass) &&
               !ApiResult.class.isAssignableFrom(returnTypeClass);
    }
    
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                 Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                 ServerHttpRequest request, ServerHttpResponse response) {
        
        log.debug("包装响应结果: {}", body != null ? body.getClass().getSimpleName() : "null");
        
        // 包装成ApiResult.success
        return ApiResult.success(body);
    }
}