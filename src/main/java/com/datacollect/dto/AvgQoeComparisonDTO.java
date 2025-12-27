package com.datacollect.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 平均QOE对比数据传输对象
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
public class AvgQoeComparisonDTO {
    
    /**
     * 端侧平均QOE数据列表
     */
    private List<ClientAvgQoeData> clientAvgQoeList;
    
    /**
     * 网络侧平均QOE数据列表
     */
    private List<NetworkAvgQoeData> networkAvgQoeList;
    
    /**
     * 当前保存的网络侧开始时间（用户选择的）
     */
    private String networkStartTime;
    
    /**
     * 端侧平均QOE数据
     */
    @Data
    public static class ClientAvgQoeData {
        /**
         * 序号
         */
        private String sequenceNumber;
        
        /**
         * 平均QOE
         */
        private BigDecimal avgQoe;
        
        /**
         * 时间戳（用于图表展示）
         */
        private String timeStamp;
    }
    
    /**
     * 网络侧平均QOE数据
     */
    @Data
    public static class NetworkAvgQoeData {
        /**
         * 时间戳
         */
        private String timeStamp;
        
        /**
         * 开始时间（用于选择匹配）
         */
        private String startTime;
        
        /**
         * 平均QOE
         */
        private BigDecimal avgQoe;
    }
}

