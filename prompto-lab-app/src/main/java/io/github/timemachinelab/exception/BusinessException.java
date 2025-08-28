package io.github.timemachinelab.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务异常类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {
    
    /**
     * 异常编码，默认500
     */
    private int code = 500;
    
    /**
     * 异常原因，默认内部错误
     */
    private String reason = "内部错误";
    
    public BusinessException() {
        super("内部错误");
    }
    
    public BusinessException(String message) {
        super(message);
        this.reason = message;
    }
    
    public BusinessException(int code, String reason) {
        super(reason);
        this.code = code;
        this.reason = reason;
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.reason = message;
    }
    
    public BusinessException(int code, String reason, Throwable cause) {
        super(reason, cause);
        this.code = code;
        this.reason = reason;
    }
}