package io.github.timemachinelab.entity.resp;

import lombok.Data;
import lombok.Builder;

/**
 * 消息处理响应实体
 */
@Data
@Builder
public class MessageProcessResp {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 节点ID
     */
    private String nodeId;
    
    /**
     * 问题类型
     */
    private String questionType;
    
    /**
     * 处理时间戳
     */
    private Long processTime;
    
    /**
     * 处理状态
     */
    private String status;
}