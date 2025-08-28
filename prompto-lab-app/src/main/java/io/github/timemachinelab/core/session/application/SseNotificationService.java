package io.github.timemachinelab.core.session.application;

import io.github.timemachinelab.core.session.domain.entity.ConversationSession;
import io.github.timemachinelab.core.session.infrastructure.ai.QuestionGenerationOperation;
import io.github.timemachinelab.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE通知服务
 * 负责管理SSE连接和发送消息给客户端
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
@Service
@Slf4j
public class SseNotificationService {


    public void sendSseMessage(User user, ConversationSession session, Object message) {
        SseEmitter emitter = user.getEmitter();
        if(emitter==null){
            //TODO 兜底功能
            log.warn("SSE连接不存在 - 用户: {}", user.getUserId());
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(message));
            log.info("SSE消息发送成功 - 会话: {}", session.getSessionId());
        }catch (IOException e){
            //TODO 移除用户类的emiter
        }

    }

    /**
     * 发送SSE消息给客户端
     *
     * @param response 消息响应对象
     */
    public void sendSseQuestionMessage(User user, ConversationSession session, QuestionGenerationOperation.QuestionGenerationResponse response) {
        String sessionId = session.getSessionId();

        // 获取刚刚创建的节点ID（当前计数器的值）
        String currentNodeId = String.valueOf(session.getNodeIdCounter().get());
        // 2. 创建修改后的响应对象，包含currentNodeId和parentNodeId
        Map<String, Object> modifiedResponse = new HashMap<>();
        modifiedResponse.put("question", response.getQuestion());
        modifiedResponse.put("currentNodeId", currentNodeId);
        modifiedResponse.put("parentNodeId", response.getParentId());
        this.sendSseMessage(user, session, response);
    }
    
    /**
     * 获取SSE连接状态
     * 
     * @return 连接状态信息
     */
    public Map<String, Object> getSseStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("connectedSessions", 10);
        status.put("totalConnections", 10);
        status.put("timestamp", System.currentTimeMillis());
        return status;
    }
    
    /**
     * 发送欢迎消息
     * 
     * @param user 用户
     * @param connectionData 欢迎消息内容
     */
    public void sendWelcomeMessage(User user, Map<String, Object> connectionData) {
        SseEmitter sseEmitters = user.getEmitter();

        if (sseEmitters != null) {
            try {
                sseEmitters.send(SseEmitter.event()
                    .name("connected")
                    .data(connectionData));
                log.info("欢迎消息发送成功");
            } catch (IOException e) {
                log.error("欢迎消息发送失败, error:{}", e.getMessage());
                //TODO  移除用户emitter
            }
        }
    }
}