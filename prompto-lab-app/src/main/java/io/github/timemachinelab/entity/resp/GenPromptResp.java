package io.github.timemachinelab.entity.resp;

import lombok.Data;
import lombok.Builder;

/**
 * 生成提示词响应实体
 */
@Data
@Builder
public class GenPromptResp {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 生成的提示词内容
     */
    private String genPrompt;
    
    /**
     * 生成时间戳
     */
    private Long generateTime;
    
    /**
     * 处理状态
     */
    private String status;
}