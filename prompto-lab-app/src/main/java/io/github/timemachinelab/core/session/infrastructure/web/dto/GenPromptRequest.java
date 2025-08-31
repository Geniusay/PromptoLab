package io.github.timemachinelab.core.session.infrastructure.web.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenPromptRequest {


    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;


    /**
     * 原始答案数据
     * - 单选/多选：List<String>
     * - 输入框：String
     * - 表单：List<FormAnswerItem>
     */
    private Object answer;
}
