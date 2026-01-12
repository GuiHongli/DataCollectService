package com.datacollect.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * app版本变更自动采集配置请求DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
public class AppVersionAutoCollectRequest {
    
    /**
     * 配置ID（更新时使用）
     */
    private Long id;
    
    /**
     * 应用名称
     */
    @NotBlank(message = "应用名称不能为空")
    private String appName;
    
    /**
     * 平台类型（false-安卓，true-iOS）
     */
    @NotNull(message = "平台类型不能为空")
    private Boolean platformType;
    
    /**
     * 是否自动采集（false-否，true-是）
     */
    @NotNull(message = "是否自动采集不能为空")
    private Boolean autoCollect;
    
    /**
     * 采集任务模版ID（当autoCollect为true时，如果未绑定模版，需要选择）
     */
    private Long templateId;
}

