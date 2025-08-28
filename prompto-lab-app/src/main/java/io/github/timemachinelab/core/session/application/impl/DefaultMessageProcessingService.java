package io.github.timemachinelab.core.session.application.impl;


import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.timemachinelab.core.constant.AllPrompt;
import io.github.timemachinelab.core.qatree.QaTree;
import io.github.timemachinelab.core.qatree.QaTreeDomain;
import io.github.timemachinelab.core.session.application.MessageProcessingService;
import io.github.timemachinelab.core.session.application.SessionManagementService;
import io.github.timemachinelab.core.session.application.ConversationService;
import io.github.timemachinelab.core.session.application.SseNotificationService;
import io.github.timemachinelab.core.session.domain.entity.ConversationSession;
import io.github.timemachinelab.core.session.infrastructure.web.dto.UnifiedAnswerRequest;
import io.github.timemachinelab.entity.User;
import io.github.timemachinelab.util.QaTreeSerializeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 默认消息处理服务实现
 * 提供基础的消息处理逻辑，可根据需要进行扩展
 * 
 * @author suifeng
 * 日期: 2025/1/27
 */
@Service
@Slf4j
public class DefaultMessageProcessingService implements MessageProcessingService {

    @Resource
    SessionManagementService sessionManagementService;
    @Resource
    QaTreeDomain qaTreeDomain;
    @Resource
    ConversationService conversationService;
    @Resource
    SseNotificationService sseNotificationService;

    @Override
    public String processAnswer(UnifiedAnswerRequest request) {
        if (!validateAnswer(request)) {
            log.warn("无效的答案请求: {}", request);
            return "无效的回答格式";
        }
        
        // 获取会话并更新qaTree
        ConversationSession session = sessionManagementService.validateAndGetSession(request.getUserId(), request.getSessionId());
        if (session != null) {
            updateQaTreeWithAnswer(session, request);
        }
        
        // 将答案转换为可读文本
        String readableText = request.toReadableText();
        
        // 构建完整的回答消息
        StringBuilder message = new StringBuilder();
        message.append("用户回答了问题: ");
        message.append(readableText);
        
        log.info("处理答案请求 - 问题类型: {}, 内容: {}", request.getQuestionType(), readableText);
        
        return message.toString();
    }
    
    /**
     * 更新qaTree中的答案
     */
    private void updateQaTreeWithAnswer(ConversationSession session, UnifiedAnswerRequest request) {
        try {
            QaTree qaTree = session.getQaTree();
            if (qaTree == null) {
                log.warn("会话的qaTree为空，无法更新答案 - 会话ID: {}", session.getSessionId());
                return;
            }
            
            String nodeId = request.getNodeId();
            // 如果nodeId为'1'（根节点），使用根节点ID
            if ("1".equals(nodeId) && qaTree.getRoot() != null) {
                nodeId = qaTree.getRoot().getId();
            }
            
            // 根据问题类型准备答案数据
            Object answerData = prepareAnswerData(request);
            
            // 更新节点答案
             boolean updated = qaTreeDomain.updateNodeAnswer(qaTree, nodeId, answerData);
             if (updated) {
                 log.info("成功更新qaTree节点答案 - 会话ID: {}, 节点ID: {}", session.getSessionId(), nodeId);
                 // 会话数据已在内存中更新，无需额外保存操作
             } else {
                 log.warn("更新qaTree节点答案失败 - 会话ID: {}, 节点ID: {}", session.getSessionId(), nodeId);
             }
        } catch (Exception e) {
            log.error("更新qaTree答案时发生异常 - 会话ID: {}, 错误: {}", session.getSessionId(), e.getMessage(), e);
        }
    }
    
    /**
     * 根据问题类型准备答案数据
     */
    private Object prepareAnswerData(UnifiedAnswerRequest request) {
        switch (request.getQuestionType().toLowerCase()) {
            case "input":
                return request.getInputAnswer();
            case "single":
            case "multi":
                return request.getChoiceAnswer();
            case "form":
                return request.getFormAnswer();
            default:
                return request.getAnswerString();
        }
    }
    
    @Override
    public String preprocessMessage(String originalMessage, UnifiedAnswerRequest answerRequest,ConversationSession conversationSession) {
        try {

            JSONObject object = new JSONObject();
            object.put("prompt",AllPrompt.GLOBAL_PROMPT);
            object.put("tree", QaTreeSerializeUtil.serialize(conversationSession.getQaTree()));
            object.put("input", answerRequest.getAnswerString());
            return object.toString();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("输入加工失败",e);
        }

    }
    
    @Override
    public boolean validateAnswer(UnifiedAnswerRequest request) {
        if (request == null) {
            return false;
        }
        
        if (request.getQuestionType() == null || request.getAnswer() == null) {
            return false;
        }
        
        // 根据问题类型验证答案格式
        switch (request.getQuestionType().toLowerCase()) {
            case "single":
            case "multi":
                return request.getChoiceAnswer() != null && !request.getChoiceAnswer().isEmpty();
                
            case "input":
                String input = request.getInputAnswer();
                return input != null && !input.trim().isEmpty();
                
            case "form":
                return request.getFormAnswer() != null && !request.getFormAnswer().isEmpty();
                
            default:
                log.warn("未知的问题类型: {}", request.getQuestionType());
                return false;
        }
    }
    
    @Override
    public String processRetryMessage(String sessionId, String nodeId, String whyRetry, ConversationSession conversationSession) {
        try {
            // 构建重试消息的JSON格式
            JSONObject retryInput = new JSONObject();
            retryInput.put("action", "retry");
            retryInput.put("nodeId", nodeId);
            retryInput.put("whyRetry", whyRetry != null ? whyRetry : "用户要求重新生成问题");
            
            // 获取节点的问题内容
            String preQuestion = sessionManagementService.getNodeQuestion(sessionId, nodeId);
            if (preQuestion != null) {
                retryInput.put("preQuestion", preQuestion);
            }
            
            JSONObject object = new JSONObject();
            object.put("prompt", AllPrompt.GLOBAL_PROMPT);
            object.put("tree", QaTreeSerializeUtil.serialize(conversationSession.getQaTree()));
            object.put("input", retryInput.toString());
            
            log.info("处理重试消息 - 会话: {}, 节点: {}, 原因: {}", sessionId, nodeId, whyRetry);
            return object.toString();
            
        } catch (JsonProcessingException e) {
             log.error("处理重试消息失败 - 会话: {}, 错误: {}", sessionId, e.getMessage(), e);
             throw new RuntimeException("重试消息处理失败", e);
         }
     }
     
     @Override
     public void processAndSendMessage(User user, ConversationSession session, String processedMessage) {
         try {
             log.info("发送消息给AI服务 - 会话: {}, 用户: {}", session.getSessionId(), session.getUserId());
             
             conversationService.processUserMessage(
                     session.getUserId(),
                     processedMessage,
                     response -> {
                         // 1. 先将AI生成的新问题添加到QaTree（只填入question，answer留空）
                         // 使用QaTreeDomain添加新节点，answer字段会自动为空
                         // appendNode方法内部会调用session.getNextNodeId()获取新节点ID
                         QaTree qaTree = qaTreeDomain.appendNode(
                                 session.getQaTree(),
                                 response.getParentId(),
                                 response.getQuestion(),
                                 session
                         );

                         sseNotificationService.sendSseQuestionMessage(user, session, response);
                     }
             );
             
             log.info("消息发送成功 - 会话: {}", session.getSessionId());
         } catch (Exception e) {
             log.error("发送消息失败 - 会话: {}, 错误: {}", session.getSessionId(), e.getMessage(), e);
             throw new RuntimeException("消息发送失败: " + e.getMessage(), e);
         }
     }
 
 }