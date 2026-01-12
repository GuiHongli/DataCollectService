package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采集任务模版实体
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@TableName("collect_task_template")
public class CollectTaskTemplate {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 模版名称
     */
    private String name;
    
    /**
     * 模版描述
     */
    private String description;
    
    /**
     * 网元ID列表（JSON格式，如：[1,2,3]）
     */
    @TableField("network_element_ids")
    private String networkElementIds;
    
    /**
     * 采集策略ID
     */
    @TableField("collect_strategy_id")
    private Long collectStrategyId;
    
    /**
     * 采集次数
     */
    @TableField("collect_count")
    private Integer collectCount;
    
    /**
     * 地域ID
     */
    @TableField("region_id")
    private Long regionId;
    
    /**
     * 国家ID
     */
    @TableField("country_id")
    private Long countryId;
    
    /**
     * 省份ID
     */
    @TableField("province_id")
    private Long provinceId;
    
    /**
     * 城市ID
     */
    @TableField("city_id")
    private Long cityId;
    
    /**
     * 网络类型（normal、weak、congestion、weakcongestion、sunshang）
     */
    @TableField("network")
    private String network;
    
    /**
     * 厂商列表（JSON格式，如：["xiaomi", "oppo", "vivo"]）
     */
    @TableField("manufacturer")
    private String manufacturer;
    
    /**
     * 逻辑环境ID列表（JSON格式，如：[1,2,3]）
     */
    @TableField("logic_environment_ids")
    private String logicEnvironmentIds;
    
    /**
     * 任务级别自定义参数（JSON格式）
     */
    @TableField("task_custom_params")
    private String taskCustomParams;
    
    /**
     * 用例配置列表（JSON格式）
     */
    @TableField("custom_params")
    private String customParams;
    
    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 是否删除（0-未删除，1-已删除）
     */
    @TableField("deleted")
    private Integer deleted;
}

