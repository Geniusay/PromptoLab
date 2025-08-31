package io.github.timemachinelab.core.session.application;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.github.timemachinelab.core.constant.AllPrompt;
import com.suifeng.sfchain.core.AIService;
import io.github.timemachinelab.core.session.domain.entity.ConversationSession;
import io.github.timemachinelab.core.session.infrastructure.ai.GenPromptOperation;
import io.github.timemachinelab.core.session.infrastructure.ai.QuestionGenerationOperation;
import io.github.timemachinelab.util.QaTreeSerializeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    @Resource
    private final AIService aiService;
    @Resource
    private SessionManagementService sessionManagementService;

    
    public void processUserMessage(String userMessage, Consumer<QuestionGenerationOperation.QuestionGenerationResponse> sseCallback) {
        processAIResponse(userMessage, sseCallback);
    }

    public void genPrompt(String sessionId, Consumer<GenPromptOperation.GpResponse> sseCallback){
        ConversationSession session = sessionManagementService.getSessionById(sessionId);
        GenPromptOperation.GpRequest req = new GenPromptOperation.GpRequest();

        try {
            req.setPrompt(AllPrompt.GEN_PROMPT_AGENT_PROMPT);
            req.setUser(session.getUser());
            req.setUserTarget(session.getUserTarget());
            req.setAiModel(session.getAiModel());
            req.setUserConversation(QaTreeSerializeUtil.serialize(session.getQaTree()));

            GenPromptOperation.GpResponse aiResponse = aiService.execute("PromptGenMaster", req);
            sseCallback.accept(aiResponse);
        }catch (Exception e){
            log.error("生成提示词失败 :{}",e.getMessage());
            GenPromptOperation.GpResponse resp = new GenPromptOperation.GpResponse();
            resp.setGenPrompt("生成提示词失败，请重试");
            sseCallback.accept(resp);
        }

    }

    private void processAIResponse(String userMessage, Consumer<QuestionGenerationOperation.QuestionGenerationResponse> sseCallback) {
        try {

            JSONObject object = JSON.parseObject(userMessage);

            // 创建AI请求
            QuestionGenerationOperation.QuestionGenerationRequest request = new QuestionGenerationOperation.QuestionGenerationRequest(object.getString("prompt"),object.getString("tree"),object.getString("input"));
            // 调用AI服务
            QuestionGenerationOperation.QuestionGenerationResponse aiResponse = aiService.execute("QUESTION_GENERATION_OP", request);

            sseCallback.accept(aiResponse);
            log.info("AI服务调用成功: {}", aiResponse);
            
        } catch (Exception e) {
            log.error("AI服务调用失败: {}", e.getMessage(), e);
            // 降级处理
            String fallbackResponse = "抱歉，我暂时无法处理您的请求，请稍后再试。";
            String nodeId = "ai_" + System.currentTimeMillis();
        }
    }


}