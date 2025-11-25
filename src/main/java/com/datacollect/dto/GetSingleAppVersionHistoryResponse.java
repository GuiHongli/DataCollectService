package com.datacollect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 获取单个应用版本历史响应DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetSingleAppVersionHistoryResponse {
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private SingleAppVersionHistoryData data;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SingleAppVersionHistoryData {
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
         * 拨号版本
         */
        @JsonProperty("dial_version")
        private String dialVersion;
        
        /**
         * 版本列表
         */
        @JsonProperty("version")
        private List<VersionInfo> version;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionInfo {
        /**
         * 版本号
         */
        @JsonProperty("version")
        private String version;
        
        /**
         * 更新日志
         */
        @JsonProperty("change_log")
        private String changeLog;
        
        /**
         * 版本更新日期
         */
        @JsonProperty("version_update_date")
        private String versionUpdateDate;
    }
}

