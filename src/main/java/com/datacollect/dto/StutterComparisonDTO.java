package com.datacollect.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 卡顿对比数据传输对象
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
public class StutterComparisonDTO {
    
    /**
     * 端侧卡顿数据列表
     */
    private List<ClientStutterData> clientStutterList;
    
    /**
     * 网络侧卡顿数据列表
     */
    private List<NetworkStutterData> networkStutterList;
    
    /**
     * 当前保存的网络侧开始时间（用户选择的）
     */
    private String networkStartTime;
    
    /**
     * 端侧卡顿数据
     */
    @Data
    public static class ClientStutterData {
        /**
         * 序号
         */
        private String sequenceNumber;
        
        /**
         * 卡顿占比
         */
        private BigDecimal stutterRatio;
        
        /**
         * 时间戳（用于图表展示）
         */
        private String timeStamp;
    }
    
    /**
     * 网络侧卡顿数据
     */
    @Data
    public static class NetworkStutterData {
        /**
         * 时间戳
         */
        private String timeStamp;
        
        /**
         * 开始时间（用于选择匹配）
         */
        private String startTime;
        
        /**
         * 卡顿次数/10
         */
        private BigDecimal stallingNumberDiv10;
    }
}

