package io.github.timemachinelab.entity;


import io.github.timemachinelab.core.session.domain.entity.ConversationSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户实体类
 * 维护用户的基本信息和会话状态
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    /**
     * 用户唯一标识
     */
    private String userId;
    
    /**
     * 用户名称
     */
    private String userName;
    
    /**
     * SSE连接对象
     */
    private SseEmitter emitter;
    
    /**
     * 会话映射表，sessionId -> ConversationSession
     */
    private final Map<String, ConversationSession> sessionMap = new ConcurrentHashMap<>();

    @Getter
    private ConversationSession latestSession;
    
    /**
     * 构造函数，初始化sessionMap
     */
    public User(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }
    
    /**
     * 添加会话
     */
    public void addSession(String sessionId, ConversationSession session) {
        sessionMap.put(sessionId, session);
        latestSession = session;
    }
    
    /**
     * 移除会话
     */
    public void removeSession(String sessionId) {
        sessionMap.remove(sessionId);
    }
    
    /**
     * 获取会话
     */
    public ConversationSession getSession(String sessionId) {
        return sessionMap.get(sessionId);
    }
    
    /**
     * 检查是否拥有指定会话
     */
    public boolean hasSession(String sessionId) {
        return sessionMap.containsKey(sessionId);
    }
}