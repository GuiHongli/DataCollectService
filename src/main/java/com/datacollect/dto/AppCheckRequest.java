package com.datacollect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 应用检查请求DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppCheckRequest {
    
    /**
     * 应用名称
     */
    @JsonProperty("app_name")
    private String appName;
    
    /**
     * 是否为iOS应用
     */
    @JsonProperty("is_ios")
    private Boolean isIos;
}

