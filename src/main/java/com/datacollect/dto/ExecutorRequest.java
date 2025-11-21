package com.datacollect.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 执行机请求DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
public class ExecutorRequest {
    
    private Long id;
    
    @NotBlank(message = "执行机名称不能为空")
    private String name;
    
    @NotBlank(message = "执行机IP不能为空")
    private String ipAddress;
    
    private String macAddress;
    
    /**
     * MAC地址ID（用于关联executor_mac_address表）
     */
    private Long macAddressId;
    
    @NotNull(message = "执行机所属地域不能为空")
    private Long regionId;
    
    private String description;
}

