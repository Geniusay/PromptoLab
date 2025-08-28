package io.github.timemachinelab.service;

import io.github.timemachinelab.entity.User;
import io.github.timemachinelab.entity.resp.UserIdResp;
import io.github.timemachinelab.exception.BusinessException;
import io.github.timemachinelab.manager.UserManager;
import io.github.timemachinelab.util.UserIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户服务类
 * 处理用户相关的业务逻辑
 */
@Slf4j
@Service
public class UserService {
    
    @Resource
    private UserManager userManager;

    /**
     * 生成用户ID
     * 
     * @param request HTTP请求对象
     * @return 用户ID响应对象
     */
    public UserIdResp generateUserId(HttpServletRequest request) {
        log.info("开始生成用户ID");
        
        try {
            String userId = UserIdGenerator.generateUserId(request);
            log.info("成功生成用户ID: {}", userId);
            
            // 创建User对象并添加到UserManager中
            User user = userManager.getOrCreateUser(userId);
            log.info("已创建用户对象 - userId: {}", userId);
            
            return new UserIdResp(userId);
        } catch (Exception e) {
            log.error("生成用户ID时发生异常", e);
            throw new BusinessException(500, "生成用户ID失败", e);
        }
    }
}