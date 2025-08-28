package io.github.timemachinelab.service;

import com.alibaba.fastjson2.JSON;
import io.github.timemachinelab.core.constant.AllPrompt;
import io.github.timemachinelab.core.session.application.MessageProcessingService;
import io.github.timemachinelab.core.session.application.SessionManagementService;
import io.github.timemachinelab.core.session.application.SseNotificationService;
import io.github.timemachinelab.core.session.domain.entity.ConversationSession;
import io.github.timemachinelab.core.session.infrastructure.ai.GenPromptOperation;
import io.github.timemachinelab.core.session.infrastructure.web.dto.GenPromptRequest;
import io.github.timemachinelab.core.session.infrastructure.web.dto.UnifiedAnswerRequest;
import io.github.timemachinelab.entity.User;
import io.github.timemachinelab.entity.req.RetryRequest;
import io.github.timemachinelab.entity.resp.*;
import io.github.timemachinelab.exception.BusinessException;
import io.github.timemachinelab.manager.UserManager;
import io.github.timemachinelab.util.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户交互服务类
 * 处理用户交互相关的业务逻辑
 */
@Slf4j
@Service
public class UserInteractionService {
    
    @Resource
    private MessageProcessingService messageProcessingService;
    @Resource
    private SessionManagementService sessionManagementService;
    @Resource
    private SseNotificationService sseNotificationService;
    @Resource
    private UserManager userManager;
    /**
     * 建立SSE连接
     */
    public SseEmitter establishSseConnection() {
        User user = UserContextUtil.getCurrentUser();
        String userId = user.getUserId();
        log.info("建立SSE连接 - 用户ID: {}", userId);

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        boolean isNewSession = false;
        ConversationSession session;
        
        try {


            // 发送欢迎消息
            Map<String, Object> connectionData = new ConcurrentHashMap<>();
            connectionData.put("userId", userId);
            connectionData.put("timestamp", System.currentTimeMillis());
            
            sseNotificationService.sendWelcomeMessage(user,connectionData);
                 
             // 设置连接事件处理
             emitter.onCompletion(() -> {
                 log.info("SSE连接完成");
             });

             emitter.onTimeout(() -> {
                 log.info("SSE连接超时");
             });
             
             emitter.onError((ex) -> {
                 log.error("SSE连接错误: {}", ex.getMessage());
             });
             
             return emitter;
                 
         } catch (Exception e) {
             throw new BusinessException("创建SSE连接失败", e);
         }
    }
    
    /**
     * 处理重试请求
     */
    public RetryResponse processRetry(RetryRequest request) {
        User user = UserContextUtil.getCurrentUser();
        log.info("收到重试请求 - nodeId: {}, sessionId: {}, whyRetry: {}", request.getNodeId(), request.getSessionId(), request.getWhyretry());

        // 使用应用服务验证节点存在性
        if (!sessionManagementService.validateNodeExists(request.getSessionId(), request.getNodeId())) {
            log.warn("节点不存在 - nodeId: {}, sessionId: {}", request.getNodeId(), request.getSessionId());
            throw new BusinessException(400, "指定的节点不存在");
        }
        
        // 使用应用服务获取问题内容
        String question = sessionManagementService.getNodeQuestion(request.getSessionId(), request.getNodeId());
        if (question == null) {
            log.warn("节点问题内容为空 - nodeId: {}, sessionId: {}", request.getNodeId(), request.getSessionId());
            throw new BusinessException(400, "节点问题内容为空");
        }

        // 获取会话对象
        ConversationSession session = sessionManagementService.getSessionById(request.getSessionId());
        if (session == null) {
            throw new BusinessException(400, "会话不存在");
        }
        
        // 移除要重试的节点（AI会基于parentId重新创建节点）
        boolean nodeRemoved = sessionManagementService.removeNode(request.getSessionId(), request.getNodeId());
        if (!nodeRemoved) {
            log.warn("移除节点失败，但继续处理重试 - sessionId: {}, nodeId: {}", 
                    request.getSessionId(), request.getNodeId());
        }
        
        // 使用MessageProcessingService处理重试消息
        String processedMessage = messageProcessingService.processRetryMessage(
                request.getSessionId(),
                request.getNodeId(),
                request.getWhyretry(),
                session
        );
        
        // 发送处理后的消息给AI服务
        messageProcessingService.processAndSendMessage(user, session, processedMessage);
        
        // 构建响应数据
        RetryResponse response = RetryResponse.builder()
                .nodeId(request.getNodeId())
                .sessionId(request.getSessionId())
                .whyretry(request.getWhyretry())
                .processTime(System.currentTimeMillis())
                .build();
        
        log.info("重试请求处理成功 - nodeId: {}, sessionId: {}", 
                request.getNodeId(), request.getSessionId());
        
        return response;
    }
    
    /**
     * 处理统一答案请求
     */
    public MessageProcessResp processAnswer(UnifiedAnswerRequest request) {
        log.info("接收到答案请求 - 会话ID: {}, 节点ID: {}, 问题类型: {}",
                request.getSessionId(),
                request.getNodeId(),
                request.getQuestionType());

        // 会话管理和验证
        User user = UserContextUtil.getCurrentUser();
        String userId = user.getUserId();

        // 验证会话是否存在
        ConversationSession session = sessionManagementService.validateAndGetSession(userId, request.getSessionId());
        if (session == null) {
            throw new BusinessException(400, "会话不存在或无效");
        }

        // nodeId验证逻辑
        String nodeId = request.getNodeId();
        if (nodeId == null || nodeId.trim().isEmpty()) {
            // nodeId为空，表示这是新建会话的第一个问题
            if (session.getQaTree() != null && session.getQaTree().getRoot() != null) {
                log.warn("会话已存在qaTree，但nodeId为空 - 会话: {}", session.getSessionId());
                throw new BusinessException(400, "现有会话必须提供nodeId");
            }
            log.info("新建会话的第一个问题 - 会话: {}", session.getSessionId());
        } else if ("1".equals(nodeId)) {
            // nodeId为'1'，表示这是根节点的回答
            if (session.getQaTree() == null || session.getQaTree().getRoot() == null) {
                log.info("根节点回答，但qaTree未初始化 - 会话: {}", session.getSessionId());
                // 允许继续处理，后续会创建qaTree
            } else {
                log.info("根节点回答 - 会话: {}", session.getSessionId());
            }
        } else {
            // nodeId不为空且不是'root'，验证是否属于该会话
            if (!sessionManagementService.validateNodeId(session.getSessionId(), nodeId)) {
                log.warn("无效的节点ID - 会话: {}, 节点: {}", session.getSessionId(), nodeId);
                throw new BusinessException(400, "无效的节点ID");
            }
            log.info("更新现有节点 - 会话: {}, 节点: {}", session.getSessionId(), nodeId);
        }

        // 验证答案格式
        if (!messageProcessingService.validateAnswer(request)) {
            log.warn("答案格式验证失败: {}", request);
            throw new BusinessException(400, "答案格式不正确");
        }

        // 处理答案并转换为消息
        String processedMessage = messageProcessingService.preprocessMessage(
                null, // 没有额外的原始消息
                request,
                session
        );

        // 发送处理后的消息给AI服务
        messageProcessingService.processAndSendMessage(user, session, processedMessage);

        return MessageProcessResp.builder()
                .sessionId(request.getSessionId())
                .nodeId(request.getNodeId())
                .questionType(request.getQuestionType())
                .processTime(System.currentTimeMillis())
                .status("success")
                .build();
    }

    /**
     * 生成提示词
     */
    public GenPromptResp generatePrompt(GenPromptRequest request) {
        User user = UserContextUtil.getCurrentUser();
        String userId = user.getUserId();
        log.info("处理生成提示词请求 - userId: {}, sessionId: {}", userId, request.getSessionId());

        // 验证会话存在性
        ConversationSession session = sessionManagementService.getSessionById(request.getSessionId());
        if (session == null) {
            log.warn("会话不存在 - sessionId: {}", request.getSessionId());
            throw new BusinessException("会话不存在");
        }

        GenPromptOperation.GpResponse gpResponse = new GenPromptOperation.GpResponse();
        gpResponse.setGenPrompt(AllPrompt.GEN_PROMPT_AGENT_PROMPT);
        sseNotificationService.sendSseMessage(user, session, JSON.toJSONString(gpResponse));
        
        log.info("生成提示词成功 - sessionId: {}", request.getSessionId());
        
        return GenPromptResp.builder()
                .sessionId(request.getSessionId())
                .genPrompt(AllPrompt.GEN_PROMPT_AGENT_PROMPT)
                .generateTime(System.currentTimeMillis())
                .status("success")
                .build();
    }
    
    /**
     * 获取SSE连接状态
     */
    public SseStatusResp getSseStatus() {
        Map<String, Object> status = sseNotificationService.getSseStatus();
        
        return SseStatusResp.builder()
                .activeConnections((Integer) status.getOrDefault("activeConnections", 0))
                .connectionDetails(status)
                .queryTime(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 构建连接响应数据
     */
    private SseConnectionResp buildConnectionResponse(ConversationSession session, String sessionId, boolean isNewSession) {
        SseConnectionResp.SseConnectionRespBuilder builder = SseConnectionResp.builder()
                .sessionId(sessionId)
                .userId(session.getUserId())
                .isNewSession(isNewSession)
                .timestamp(System.currentTimeMillis());
        
        // 根据会话状态返回nodeId
        if (isNewSession) {
            // 新会话返回根节点ID
            builder.nodeId("1");
            log.info("新会话返回根节点ID: 1 - 会话: {}", sessionId);
        } else if (session.getQaTree() != null && session.getQaTree().getRoot() != null) {
            // 已存在会话，返回根节点ID（因为qaTree只有根节点）
            String rootNodeId = session.getQaTree().getRoot().getId();
            builder.nodeId(rootNodeId);
            log.info("已存在会话返回根节点ID: {} - 会话: {}", rootNodeId, sessionId);
            
            // 返回qaTree
            try {
                String qaTreeJson = io.github.timemachinelab.util.QaTreeSerializeUtil.serialize(session.getQaTree());
                builder.qaTree(qaTreeJson);
            } catch (Exception e) {
                log.error("序列化qaTree失败: {}", e.getMessage());
            }
        } else {
            // 兜底情况，返回根节点ID
            builder.nodeId("1");
            log.info("兜底返回根节点ID: 1 - 会话: {}", sessionId);
        }
        
        return builder.build();
    }
}