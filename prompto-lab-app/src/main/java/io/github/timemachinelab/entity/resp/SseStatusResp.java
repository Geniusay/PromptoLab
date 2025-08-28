package io.github.timemachinelab.entity.resp;

import lombok.Data;
import lombok.Builder;

import java.util.Map;

/**
 * SSE状态响应实体
 */
@Data
@Builder
public class SseStatusResp {
    
    /**
     * 活跃连接数
     */
    private Integer activeConnections;
    
    /**
     * 连接详情
     */
    private Map<String, Object> connectionDetails;
    
    /**
     * 查询时间戳
     */
    private Long queryTime;
}