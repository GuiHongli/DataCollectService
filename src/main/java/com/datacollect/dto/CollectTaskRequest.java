package com.datacollect.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 采集任务请求DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
public class CollectTaskRequest {
    
    /**
     * 任务名称
     */
    @NotBlank(message = "任务名称不能为空")
    private String name;
    
    /**
     * 任务描述
     */
    private String description;
    
    /**
     * 采集策略ID
     */
    @NotNull(message = "采集策略ID不能为空")
    private Long collectStrategyId;
    
    /**
     * 采集次数
     */
    @NotNull(message = "采集次数不能为空")
    private Integer collectCount;
    
    /**
     * 地域ID
     */
    private Long regionId;
    
    /**
     * 国家ID
     */
    private Long countryId;
    
    /**
     * 省份ID
     */
    private Long provinceId;
    
    /**
     * 城市ID
     */
    private Long cityId;
    
    /**
     * 选中的逻辑环境ID列表
     */
    @NotNull(message = "逻辑环境列表不能为空")
    private List<Long> logicEnvironmentIds;
    
    /**
     * 自定义参数列表（JSON格式）
     */
    private String customParams;
}
