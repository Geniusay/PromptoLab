package io.github.timemachinelab.context;

import io.github.timemachinelab.entity.User;
import lombok.Data;

/**
 * 用户上下文信息
 */
@Data
public class UserContext {

    private User user;

    /**
     * 请求时间戳
     */
    private Long timestamp;
    
    public UserContext(User user) {
        this.user = user;
        this.timestamp = System.currentTimeMillis();
    }
}