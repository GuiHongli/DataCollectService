package com.datacollect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 获取每日排名请求DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetDailyRankRequest {
    
    /**
     * 周期类型：daily（每日）, weekly（每周）, monthly（每月）, quarterly（每季度）
     */
    @JsonProperty("period_type")
    private String periodType;
    
    /**
     * 周期值，格式根据period_type不同而不同：
     * daily: YYYY-MM-DD
     * weekly: YYYY-WW（年份-周数）
     * monthly: YYYY-MM
     * quarterly: YYYY-Q（年份-季度）
     */
    @JsonProperty("period_value")
    private String periodValue;
    
    /**
     * 市场品牌：appstore, googleplay, huawei, xiaomi
     */
    @JsonProperty("market_brand")
    private String marketBrand;
    
    /**
     * 应用类别：application（应用）, game（游戏）
     */
    @JsonProperty("category")
    private String category;
    
    /**
     * 每页显示多少数据，默认0表示不分页
     */
    @JsonProperty("num")
    private Integer num = 0;
    
    /**
     * 页码，从1开始
     */
    @JsonProperty("page")
    private Integer page;
}



import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 获取每日排名请求DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetDailyRankRequest {
    
    /**
     * 周期类型：daily（每日）, weekly（每周）, monthly（每月）, quarterly（每季度）
     */
    @JsonProperty("period_type")
    private String periodType;
    
    /**
     * 周期值，格式根据period_type不同而不同：
     * daily: YYYY-MM-DD
     * weekly: YYYY-WW（年份-周数）
     * monthly: YYYY-MM
     * quarterly: YYYY-Q（年份-季度）
     */
    @JsonProperty("period_value")
    private String periodValue;
    
    /**
     * 市场品牌：appstore, googleplay, huawei, xiaomi
     */
    @JsonProperty("market_brand")
    private String marketBrand;
    
    /**
     * 应用类别：application（应用）, game（游戏）
     */
    @JsonProperty("category")
    private String category;
    
    /**
     * 每页显示多少数据，默认0表示不分页
     */
    @JsonProperty("num")
    private Integer num = 0;
    
    /**
     * 页码，从1开始
     */
    @JsonProperty("page")
    private Integer page;
}







