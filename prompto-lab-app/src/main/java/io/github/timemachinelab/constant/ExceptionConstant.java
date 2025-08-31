package io.github.timemachinelab.constant;

import io.github.timemachinelab.exception.BusinessException;

public class ExceptionConstant {
    public static final BusinessException SESSION_NOT_FOUND = new BusinessException("会话不存在");
}
