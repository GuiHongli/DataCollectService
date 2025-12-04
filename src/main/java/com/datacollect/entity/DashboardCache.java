package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仪表盘统计数据缓存实体
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@TableName("dashboard_cache")
public class DashboardCache {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 缓存键（OVERALL_STATS或regionId）
     */
    @TableField("cache_key")
    private String cacheKey;
    
    /**
     * 缓存类型（OVERALL-总体统计，REGION-地域统计）
     */
    @TableField("cache_type")
    private String cacheType;
    
    /**
     * 缓存值，JSON格式存储统计数据
     */
    @TableField("cache_value")
    private String cacheValue;
    
    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
    
    /**
     * 缓存类型常量
     */
    public static class CacheType {
        public static final String OVERALL = "OVERALL";
        public static final String REGION = "REGION";
    }
    
    /**
     * 总体统计缓存键
     */
    public static final String OVERALL_STATS_KEY = "OVERALL_STATS";
}






























