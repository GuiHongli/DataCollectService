package com.datacollect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 获取版本历史响应DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetVersionHistoryResponse {
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private List<VersionHistoryData> data;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionHistoryData {
        /**
         * 应用名称
         */
        @JsonProperty("app_name")
        private String appName;
        
        /**
         * 应用图标
         */
        @JsonProperty("icon")
        private String icon;
        
        /**
         * 应用类别
         */
        @JsonProperty("app_category")
        private String appCategory;
        
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
        
        /**
         * 版本更新日期
         */
        @JsonProperty("version_update_date")
        private String versionUpdateDate;
        
        /**
         * 更新日志
         */
        @JsonProperty("change_log")
        private String changeLog;
        
        /**
         * 拨号版本
         */
        @JsonProperty("dial_verion")
        private String dialVerion;
    }
}

