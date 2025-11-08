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
     * 日期，格式：YYYY-MM-DD
     */
    @JsonProperty("date")
    private String date;
    
    /**
     * 市场品牌：appstore, googleplay, huawei, xiaomi
     */
    @JsonProperty("market_brand")
    private String marketBrand;
}

