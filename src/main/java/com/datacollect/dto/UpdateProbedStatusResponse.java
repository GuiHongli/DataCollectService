package com.datacollect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 更新探测状态响应DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProbedStatusResponse {
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private UpdateProbedStatusData data;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProbedStatusData {
        /**
         * 更新数量
         */
        @JsonProperty("updata_count")
        private Integer updataCount;
    }
}


