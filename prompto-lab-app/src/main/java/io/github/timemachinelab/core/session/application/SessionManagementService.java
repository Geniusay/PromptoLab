package io.github.timemachinelab.core.session.application;

import io.github.timemachinelab.core.qatree.QaTree;
import io.github.timemachinelab.core.qatree.QaTreeDomain;
import io.github.timemachinelab.core.session.domain.entity.ConversationSession;
import io.github.timemachinelab.entity.User;
import io.github.timemachinelab.manager.UserManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 会话管理服务
 * 负责用户与会话的映射管理、nodeId验证等功能
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
@Service
@Slf4j
public class SessionManagementService {
    
    // 用户ID到会话ID列表的映射（一对多关系）
    private final Map<String, List<String>> userSessionMap = new ConcurrentHashMap<>();

    // 会话存储
    private final Map<String, ConversationSession> sessions = new ConcurrentHashMap<>();

    @Resource
    private QaTreeDomain qaTreeDomain;

    @Resource
    private UserManager userManager;

    /**
     * 创建新会话
     */
    public ConversationSession createNewSession(String userId) {
        User user = userManager.getUser(userId);
        // 生成新的sessionId
        String newSessionId = UUID.randomUUID().toString();
        
        // 先创建会话对象（qaTree为null）
        ConversationSession session = new ConversationSession(userId, newSessionId, null);
        
        // 使用会话的自增ID创建QaTree，确保根节点ID=1
        QaTree tree = qaTreeDomain.createTree("你好，我有什么可以帮你？", session);
        
        // 设置QaTree到会话中
        session.setQaTree(tree);

        log.info("创建新会话 - 用户: {}, 会话: {}, 根节点ID: 1", userId, session.getSessionId());

        user.addSession(newSessionId, session);
        return session;
    }
    
    /**
     * 验证并获取现有会话
     * 如果会话不存在或不属于该用户，返回null
     */
    public ConversationSession validateAndGetSession(String userId, String sessionId) {
        ConversationSession session = sessions.get(sessionId);
        
        if (session == null) {
            log.warn("会话不存在 - 用户: {}, 请求会话: {}", userId, sessionId);
            return null;
        }
        
        // 验证会话是否属于该用户
        if (!session.getUserId().equals(userId)) {
            log.warn("会话不属于该用户 - 用户: {}, 会话: {}, 会话所有者: {}", 
                    userId, sessionId, session.getUserId());
            return null;
        }
        
        // 确保用户会话映射中包含该会话ID（防止映射不一致）
        List<String> userSessions = userSessionMap.computeIfAbsent(userId, k -> new ArrayList<>());
        if (!userSessions.contains(sessionId)) {
            userSessions.add(sessionId);
        }
        
        log.debug("验证会话成功 - 用户: {}, 会话: {}", userId, sessionId);
        return session;
    }
    
    /**
     * 清理指定会话
     * 
     * @param sessionId 会话ID
     */
    public void removeSession(User user, String sessionId) {
        user.removeSession(sessionId);
        log.info("清理会话 - 用户: {}, 会话: {}", user.getUserId(), sessionId);
    }
    
    /**
     * 清理用户的所有会话
     * 
     * @param userId 用户ID
     */
    public void removeAllUserSessions(String userId) {
        List<String> sessionIds = userSessionMap.remove(userId);
        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                sessions.remove(sessionId);
            }
            log.info("清理用户所有会话 - 用户: {}, 会话数量: {}", userId, sessionIds.size());
        }
    }
    
    /**
     * 根据会话ID获取会话对象
     * 
     * @param sessionId 会话ID
     * @return 会话对象，如果不存在则返回null
     */
    public ConversationSession getSessionById(String sessionId) {
        return sessions.get(sessionId);
    }

    
    /**
     * 获取指定会话中节点的问题内容
     *
     * @param nodeId 节点ID
     * @return 问题内容，如果节点不存在或问题为空则返回null
     */
    public String getNodeQuestion(ConversationSession session, String nodeId) {
        QaTree tree = session.getQaTree();
        return qaTreeDomain.getNodeQuestion(tree, nodeId);
    }
    

    /**
     * 移除指定会话中的节点
     * 
     * @param session 会话ID
     * @param nodeId 节点ID
     * @return 是否移除成功
     */
    public boolean removeNode(ConversationSession session, String nodeId) {
        String sessionId = session.getSessionId();
        QaTree tree = session.getQaTree();
        if (tree == null) {

            log.warn("会话的QaTree不存在: {}", sessionId);
            return false;
        }
        
        boolean removed = qaTreeDomain.removeNode(tree, nodeId);
        if (removed) {
            log.info("成功移除节点 - 会话: {}, 节点: {}", sessionId, nodeId);
        } else {
            log.warn("移除节点失败 - 会话: {}, 节点: {}", sessionId, nodeId);
        }
        
        return removed;
    }
    
    /**
     * 获取会话统计信息
     */
    public Map<String, Object> getSessionStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalSessions", sessions.size());
        stats.put("activeUsers", userSessionMap.size());
        
        // 计算每个用户的会话数量分布
        Map<String, Integer> userSessionCounts = new ConcurrentHashMap<>();
        int totalUserSessions = 0;
        for (Map.Entry<String, List<String>> entry : userSessionMap.entrySet()) {
            int sessionCount = entry.getValue().size();
            userSessionCounts.put(entry.getKey(), sessionCount);
            totalUserSessions += sessionCount;
        }
        
        stats.put("userSessionCounts", userSessionCounts);
        stats.put("averageSessionsPerUser", userSessionMap.isEmpty() ? 0 : (double) totalUserSessions / userSessionMap.size());
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }

    private QaTree createDefaultQaTree() {

        QaTree tree = qaTreeDomain.createTree("default");

        return tree;
    }
}