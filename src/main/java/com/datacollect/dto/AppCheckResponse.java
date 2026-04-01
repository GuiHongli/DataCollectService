package com.datacollect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 应用检查响应DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppCheckResponse {
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private List<AppCheckData> data;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppCheckData {
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
        
        /**
         * 是否为新应用
         */
        @JsonProperty("is_new")
        private Boolean isNew;
    }
}

