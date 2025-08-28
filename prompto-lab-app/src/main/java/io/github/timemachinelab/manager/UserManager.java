package io.github.timemachinelab.manager;

import io.github.timemachinelab.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户管理器
 * 负责维护和管理所有用户对象
 */
@Slf4j
@Component
public class UserManager {
    
    /**
     * 用户映射表，userId -> User
     */
    private final Map<String, User> userMap = new ConcurrentHashMap<>();
    
    /**
     * 创建用户
     * 
     * @param userId 用户ID
     * @param userName 用户名称
     * @return 创建的用户对象
     */
    public User createUser(String userId, String userName) {
        User user = new User(userId, userName);
        userMap.put(userId, user);
        log.info("创建用户成功 - userId: {}, userName: {}", userId, userName);
        return user;
    }
    
    /**
     * 创建用户（仅使用userId）
     * 
     * @param userId 用户ID
     * @return 创建的用户对象
     */
    public User createUser(String userId) {
        return createUser(userId, userId);
    }
    
    /**
     * 根据用户ID获取用户
     * 
     * @param userId 用户ID
     * @return 用户对象，不存在则返回null
     */
    public User getUser(String userId) {
        return userMap.get(userId);
    }
    
    /**
     * 检查用户是否存在
     * 
     * @param userId 用户ID
     * @return 是否存在
     */
    public boolean userExists(String userId) {
        return userMap.containsKey(userId);
    }
    
    /**
     * 更新用户信息
     * 
     * @param user 用户对象
     */
    public void updateUser(User user) {
        if (user != null && user.getUserId() != null) {
            userMap.put(user.getUserId(), user);
            log.info("更新用户信息成功 - userId: {}", user.getUserId());
        }
    }
    
    /**
     * 删除用户
     * 
     * @param userId 用户ID
     * @return 被删除的用户对象，不存在则返回null
     */
    public User removeUser(String userId) {
        User removedUser = userMap.remove(userId);
        if (removedUser != null) {
            log.info("删除用户成功 - userId: {}", userId);
        }
        return removedUser;
    }
    
    /**
     * 获取所有用户数量
     * 
     * @return 用户总数
     */
    public int getUserCount() {
        return userMap.size();
    }
    
    /**
     * 清空所有用户
     */
    public void clearAllUsers() {
        int count = userMap.size();
        userMap.clear();
        log.info("清空所有用户，共清空 {} 个用户", count);
    }
    
    /**
     * 获取或创建用户
     * 如果用户不存在则创建新用户
     * 
     * @param userId 用户ID
     * @return 用户对象
     */
    public User getOrCreateUser(String userId) {
        User user = getUser(userId);
        if (user == null) {
            user = createUser(userId);
        }
        return user;
    }
}