package com.datacollect.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * RTT对比数据传输对象
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
public class RttComparisonDTO {
    
    /**
     * 端侧RTT数据列表
     */
    private List<ClientRttData> clientRttList;
    
    /**
     * 网络侧RTT数据列表
     */
    private List<NetworkRttData> networkRttList;
    
    /**
     * 当前保存的网络侧开始时间（用户选择的）
     */
    private String networkStartTime;
    
    /**
     * 端侧RTT数据
     */
    @Data
    public static class ClientRttData {
        /**
         * 序号
         */
        private String sequenceNumber;
        
        /**
         * RTT值
         */
        private BigDecimal rtt;
        
        /**
         * 时间戳（用于图表展示）
         */
        private String timeStamp;
    }
    
    /**
     * 网络侧RTT数据
     */
    @Data
    public static class NetworkRttData {
        /**
         * 时间戳
         */
        private String timeStamp;
        
        /**
         * 开始时间（用于选择匹配）
         */
        private String startTime;
        
        /**
         * 服务时延
         */
        private BigDecimal serviceDelay;
    }
}

