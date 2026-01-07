package com.datacollect.dto;

import lombok.Data;

/**
 * 网络侧数据聚合DTO
 * 用于按GPSI+日期+子应用ID分组查询
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
public class NetworkDataGroupDTO {
    
    /**
     * GPSI
     */
    private String gpsi;
    
    /**
     * 日期（年月日，格式：YYYY-MM-DD）
     */
    private String date;
    
    /**
     * 子应用ID
     */
    private String subAppId;
    
    /**
     * 该分组下的数据条数
     */
    private Long count;
}







