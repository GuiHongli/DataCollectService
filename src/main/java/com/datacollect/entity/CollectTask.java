package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采集任务实体
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@TableName("collect_task")
public class CollectTask {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 任务名称
     */
    private String name;
    
    /**
     * 任务描述
     */
    private String description;
    
    /**
     * 采集策略ID
     */
    @TableField("collect_strategy_id")
    private Long collectStrategyId;
    
    /**
     * 采集策略名称
     */
    @TableField("collect_strategy_name")
    private String collectStrategyName;
    
    /**
     * 用例集ID
     */
    @TableField("test_case_set_id")
    private Long testCaseSetId;
    
    /**
     * 用例集名称
     */
    @TableField("test_case_set_name")
    private String testCaseSetName;
    
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
     * 任务状态 (PENDING/RUNNING/COMPLETED/FAILED)
     */
    @TableField("status")
    private String status;
    
    /**
     * 总用例数
     */
    @TableField("total_test_case_count")
    private Integer totalTestCaseCount;
    
    /**
     * 已完成用例数
     */
    @TableField("completed_test_case_count")
    private Integer completedTestCaseCount;
    
    /**
     * 成功用例数
     */
    @TableField("success_test_case_count")
    private Integer successTestCaseCount;
    
    /**
     * 失败用例数
     */
    @TableField("failed_test_case_count")
    private Integer failedTestCaseCount;
    
    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 自定义参数列表（JSON格式）
     */
    @TableField("custom_params")
    private String customParams;
}
