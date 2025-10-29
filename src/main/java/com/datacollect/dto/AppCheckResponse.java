package com.datacollect.dto;

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
        private String app_name;
        
        /**
         * 是否为iOS应用
         */
        private Boolean is_ios;
        
        /**
         * 是否为新应用
         */
        private Boolean is_new;
    }
}
