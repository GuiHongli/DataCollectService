package com.datacollect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 获取单个应用版本历史请求DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetSingleAppVersionHistoryRequest {
    
    /**
     * 应用名称
     */
    @JsonProperty("app_name")
    private String appName;
    
    /**
     * 是否为iOS应用
     */
    @JsonProperty("is_ios")
    private String isIos;
}

