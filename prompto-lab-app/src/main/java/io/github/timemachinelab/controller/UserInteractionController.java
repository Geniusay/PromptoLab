package io.github.timemachinelab.controller;

import io.github.timemachinelab.annotation.AutoResp;
import io.github.timemachinelab.annotation.Log;
import io.github.timemachinelab.annotation.UserCheck;
import io.github.timemachinelab.core.session.infrastructure.web.dto.GenPromptRequest;
import io.github.timemachinelab.core.session.infrastructure.web.dto.UnifiedAnswerRequest;
import io.github.timemachinelab.entity.req.RetryRequest;
import io.github.timemachinelab.entity.resp.*;
import io.github.timemachinelab.service.UserInteractionService;
import io.github.timemachinelab.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 用户交互控制器
 * 提供用户交互相关的API接口
 * 
 * @author suifeng
 * @date 2025/1/20
 */
@Slf4j
@RestController
@RequestMapping("/api/user-interaction")
@Validated
public class UserInteractionController {
    
    @Resource
    private UserService userService;

    @Resource
    private UserInteractionService userInteractionService;

    /**
     * 获取用户Token
     * 
     * @param request HTTP请求对象
     * @return 用户ID响应对象
     */
    @Log
    @AutoResp
    @PostMapping("/user/token")
    public UserIdResp getUserToken(HttpServletRequest request) {
        return userService.generateUserId(request);
    }

    /**
     * 建立SSE连接
     */
    @UserCheck
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamConversation() {
        return userInteractionService.establishSseConnection();
    }

    /**
     * 重试接口
     * 
     * @param request 重试请求参数
     * @return 重试结果
     */
    @Log
    @UserCheck
    @AutoResp
    @PostMapping("/retry")
    public RetryResponse retry(@Valid @RequestBody RetryRequest request) {
        return userInteractionService.processRetry(request);
    }

    /**
     * 处理统一答案请求
     * 支持单选、多选、输入框、表单等多种问题类型的回答
     */
    @Log
    @UserCheck
    @AutoResp
    @PostMapping("/message")
    public MessageProcessResp processAnswer(@Validated @RequestBody UnifiedAnswerRequest request) {
        return userInteractionService.processAnswer(request);
    }


    /**
     * 生成提示词接口
     * 
     * @param request 生成提示词请求参数
     * @return 生成提示词结果
     */
    @Log
    @UserCheck
    @AutoResp
    @PostMapping("/gen-prompt")
    public GenPromptResp genPrompt(@RequestBody GenPromptRequest request) {
        return userInteractionService.generatePrompt(request);
    }

    
    /**
     * 获取SSE连接状态
     * 
     * @return SSE连接状态信息
     */
    @Log
    @UserCheck
    @AutoResp
    @GetMapping("/sse/status")
    public SseStatusResp getSseStatus() {
        return userInteractionService.getSseStatus();
    }
}