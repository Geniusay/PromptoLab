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
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.timemachinelab.constant.ExceptionConstant.SESSION_NOT_FOUND;

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
        user.setEmitter(emitter);

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
        String sessionId = request.getSessionId();
        String nodeId = request.getNodeId();

        ConversationSession session = user.getSession(sessionId);
        if (session == null) {
            throw SESSION_NOT_FOUND;
        }


        log.info("收到重试请求 - nodeId: {}, sessionId: {}, whyRetry: {}", nodeId, sessionId, request.getWhyretry());

        // 使用应用服务验证节点存在性
        if(session.hasNodeId(nodeId)){
            log.warn("节点不存在 - nodeId: {}, sessionId: {}", nodeId, sessionId);
            throw new BusinessException(400, "指定的节点不存在");
        }
        
        // 使用应用服务获取问题内容
        String question = sessionManagementService.getNodeQuestion(session, nodeId);
        if (question == null) {
            log.warn("节点问题内容为空 - nodeId: {}, sessionId: {}", nodeId, sessionId);
            throw new BusinessException(400, "节点问题内容为空");
        }

        // 移除要重试的节点（AI会基于parentId重新创建节点）
        boolean nodeRemoved = sessionManagementService.removeNode(session, nodeId);
        if (!nodeRemoved) {
            log.warn("移除节点失败，但继续处理重试 - sessionId: {}, nodeId: {}", sessionId, nodeId);
        }
        
        // 使用MessageProcessingService处理重试消息
        String processedMessage = messageProcessingService.processRetryMessage(
                session,
                nodeId,
                request.getWhyretry()
        );
        
        // 发送处理后的消息给AI服务
        messageProcessingService.processAndSendMessage(user, session, processedMessage);

        // 构建响应数据
        RetryResponse response = RetryResponse.builder()
                .nodeId(nodeId)
                .sessionId(sessionId)
                .whyretry(request.getWhyretry())
                .processTime(System.currentTimeMillis())
                .build();
        
        log.info("重试请求处理成功 - nodeId: {}, sessionId: {}", nodeId, sessionId);
        
        return response;
    }
    
    /**
     * 处理统一答案请求
     */
    public MessageProcessResp processAnswer(UnifiedAnswerRequest request) {
        String sessionId = request.getSessionId();

        log.info("接收到答案请求 - 会话ID: {}, 节点ID: {}, 问题类型: {}",
                sessionId,
                request.getNodeId(),
                request.getQuestionType());

        // 会话管理和验证
        User user = UserContextUtil.getCurrentUser();
        String nodeId = request.getNodeId();

        assert user != null;
        ConversationSession session;
        if(StringUtils.isEmpty(sessionId)){
            session = sessionManagementService.createNewSession(user.getUserId());
            session.setUser(request.getUser());
            log.info("会话ID为空，创建新会话，会话ID: {}", sessionId);
            nodeId = session.getQaTree().getLatestNode().getId();
        }else{
            session = user.getSession(sessionId);
        }
        // 验证会话是否存在
        if (session == null) {
            throw SESSION_NOT_FOUND;
        }
        // nodeId验证逻辑
        if (nodeId == null || !session.hasNodeId(nodeId)) {
            log.warn("无效的节点ID - 会话: {}, 节点: {}", session.getSessionId(), nodeId);
            throw new BusinessException(400, "无效的节点ID");
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

        messageProcessingService.processAnswer(session, request);

        return MessageProcessResp.builder()
                .sessionId(sessionId)
                .nodeId(nodeId)
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
        String sessionId = request.getSessionId();
        log.info("处理生成提示词请求 - userId: {}, sessionId: {}", userId, sessionId);
        ConversationSession session = user.getSession(sessionId);

        // 验证会话存在性
        if (session == null) {
            throw SESSION_NOT_FOUND;
        }

        GenPromptOperation.GpResponse gpResponse = new GenPromptOperation.GpResponse();
        gpResponse.setGenPrompt(AllPrompt.GEN_PROMPT_AGENT_PROMPT);
        sseNotificationService.sendSseMessage(user, session, JSON.toJSONString(gpResponse));
        
        log.info("生成提示词成功 - sessionId: {}", sessionId);
        
        return GenPromptResp.builder()
                .sessionId(sessionId)
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
}