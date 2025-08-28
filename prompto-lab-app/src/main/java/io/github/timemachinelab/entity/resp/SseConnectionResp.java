package io.github.timemachinelab.entity.resp;

import lombok.Data;
import lombok.Builder;

/**
 * SSE连接响应实体
 */
@Data
@Builder
public class SseConnectionResp {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 是否为新会话
     */
    private Boolean isNewSession;
    
    /**
     * 节点ID
     */
    private String nodeId;
    
    /**
     * QA树JSON字符串
     */
    private String qaTree;
    
    /**
     * 连接建立时间戳
     */
    private Long timestamp;
}