package io.github.timemachinelab.util;

import io.github.timemachinelab.context.UserContext;
import io.github.timemachinelab.entity.User;

/**
 * 用户上下文工具类
 * 提供便捷的用户信息存储和获取方法
 */
public class UserContextUtil {
    
    private static final ThreadLocal<UserContext> USER_CONTEXT_HOLDER = new ThreadLocal<>();
    
    /**
     * 设置用户上下文
     */
    public static void setUserContext(UserContext userContext) {
        USER_CONTEXT_HOLDER.set(userContext);
    }



    public static void setUser(User user) {
        USER_CONTEXT_HOLDER.set(new UserContext(user));
    }


    /**
     * 获取用户上下文
     */
    public static UserContext getUserContext() {
        return USER_CONTEXT_HOLDER.get();
    }
    
    /**
     * 获取当前用户ID
     */
    public static String getCurrentUserId() {
        UserContext context = USER_CONTEXT_HOLDER.get();
        return context != null ? context.getUser().getUserId() : null;
    }
    /**
     * 获取当前用户ID
     */
    public static User getCurrentUser() {
        UserContext context = USER_CONTEXT_HOLDER.get();
        return context != null ? context.getUser() : null;
    }



    /**
     * 清除用户上下文
     */
    public static void clear() {
        USER_CONTEXT_HOLDER.remove();
    }
    
    /**
     * 检查是否存在用户上下文
     */
    public static boolean hasUserContext() {
        return USER_CONTEXT_HOLDER.get() != null;
    }
}