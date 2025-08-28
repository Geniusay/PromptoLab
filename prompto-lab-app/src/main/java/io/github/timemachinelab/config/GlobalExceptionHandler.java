package io.github.timemachinelab.config;

import io.github.timemachinelab.entity.resp.ApiResult;
import io.github.timemachinelab.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一处理业务异常和系统异常
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Object>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getReason());
        
        ApiResult<Object> result = ApiResult.error(e.getCode(), e.getReason());
        return ResponseEntity.ok(result);
    }
    
    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Object>> handleException(Exception e) {
        log.error("系统异常: ", e);
        
        ApiResult<Object> result = ApiResult.serverError("系统内部错误");
        return ResponseEntity.ok(result);
    }
}