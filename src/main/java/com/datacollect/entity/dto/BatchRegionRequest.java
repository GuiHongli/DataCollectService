package com.datacollect.entity.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class BatchRegionRequest {
    
    // 区域名称（可选，用于区域+国家模式）
    private String regionName;
    
    // 国家名称（必填）
    @NotBlank(message = "国家名称不能为空")
    private String countryName;
    
    // 省份名称（可选）
    private String provinceName;
    
    // 城市名称（可选）
    private String cityName;
    
    private String description;
    
    @NotNull(message = "状态不能为空")
    private Integer status = 1;
}
