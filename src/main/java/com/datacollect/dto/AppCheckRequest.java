package com.datacollect.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

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
    private String app_name;
    
    /**
     * 是否为iOS应用
     */
    private Boolean is_ios;
}

