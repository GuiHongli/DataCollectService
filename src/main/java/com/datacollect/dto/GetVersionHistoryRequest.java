package com.datacollect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 获取版本历史请求DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetVersionHistoryRequest {
    
    /**
     * 是否为iOS应用
     */
    @JsonProperty("is_ios")
    private Boolean isIos;
}

