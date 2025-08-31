package io.github.timemachinelab.core.session.domain.entity;

import io.github.timemachinelab.core.qatree.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class ConversationSession {
    
    private final String sessionId;
    private final String userId;
    /**
     * -- SETTER --
     *  设置QaTree（仅用于初始化）
     *
     * @param qaTree QA树对象
     */
    @Setter
    private QaTree qaTree; // 移除final，允许后续设置
    private final LocalDateTime createTime;
    private LocalDateTime updateTime;

    private String user; //用户画像
    private String userTarget; // 用户目标
    private String aiModel; // AI模型

    private String genPrompt; // 生成提示词


    
    // 节点ID自增计数器，从1开始
    private final AtomicInteger nodeIdCounter = new AtomicInteger(0);
    
    public ConversationSession(String userId, String sessionId, QaTree qaTree) {
        this.qaTree = qaTree;
        this.sessionId = sessionId;
        this.userId = userId;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 获取下一个节点ID（自增）
     * @return 自增的节点ID字符串
     */
    public String getNextNodeId() {
        return String.valueOf(nodeIdCounter.incrementAndGet());
    }
    
    /**
     * 获取节点ID计数器
     * @return 节点ID计数器
     */
    public AtomicInteger getNodeIdCounter() {
        return nodeIdCounter;
    }

    public boolean hasNodeId(String nodeId){
        return qaTree.getNodeById(nodeId) != null;
    }
}