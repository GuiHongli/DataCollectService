package com.datacollect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 获取每日排名响应DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetDailyRankResponse {
    
    /**
     * 响应消息
     */
    @JsonProperty("message")
    private String message;
    
    /**
     * 应用数据列表
     */
    @JsonProperty("data")
    private List<AppRankData> data;
    
    /**
     * 应用排名数据
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppRankData {
        
        /**
         * 应用名称
         */
        @JsonProperty("app_name")
        private String appName;
        
        /**
         * 日期
         */
        @JsonProperty("date")
        private String date;
        
        /**
         * 应用类型
         */
        @JsonProperty("app_type")
        private String appType;
        
        /**
         * 应用版本
         */
        @JsonProperty("app_version")
        private String appVersion;
        
        /**
         * 应用描述
         */
        @JsonProperty("app_description")
        private String appDescription;
    }
}

