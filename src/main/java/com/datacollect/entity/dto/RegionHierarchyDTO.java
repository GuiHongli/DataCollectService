package com.datacollect.entity.dto;

import lombok.Data;

@Data
public class RegionHierarchyDTO {
    
    private Long id;
    private String regionName;      // 地域名称（level=1）
    private String countryName;     // 国家/省份名称（level=2）
    private String provinceName;    // 省份名称（level=3）
    private String cityName;        // 城市名称（level=4）
    private Integer status;         // 状态
    private String createTime;      // 创建时间
}
