package io.github.timemachinelab.entity.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户ID响应对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserIdResp {
    
    /**
     * 用户唯一标识
     */
    private String userId;
}