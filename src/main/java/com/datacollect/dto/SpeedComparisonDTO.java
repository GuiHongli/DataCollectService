package com.datacollect.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 速率对比数据传输对象
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
public class SpeedComparisonDTO {
    
    /**
     * 端侧速率数据列表
     */
    private List<ClientSpeedData> clientSpeedList;
    
    /**
     * 网络侧速率数据列表
     */
    private List<NetworkSpeedData> networkSpeedList;
    
    /**
     * 当前保存的网络侧开始时间（用户选择的）
     */
    private String networkStartTime;
    
    /**
     * 端侧速率数据
     */
    @Data
    public static class ClientSpeedData {
        /**
         * 序号
         */
        private String sequenceNumber;
        
        /**
         * 速率（Kbps）
         */
        private BigDecimal speed;
        
        /**
         * 时间戳（用于图表展示）
         */
        private String timeStamp;
    }
    
    /**
     * 网络侧速率数据
     */
    @Data
    public static class NetworkSpeedData {
        /**
         * 时间戳
         */
        private String timeStamp;
        
        /**
         * 开始时间（用于选择匹配）
         */
        private String startTime;
        
        /**
         * 上行带宽（Kbps，已除以1024）
         */
        private BigDecimal uplinkBandwidth;
        
        /**
         * 下行带宽（Kbps，已除以1024）
         */
        private BigDecimal downlinkBandwidth;
    }
}

