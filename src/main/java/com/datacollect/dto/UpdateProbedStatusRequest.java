package com.datacollect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 更新探测状态请求DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProbedStatusRequest {
    
    /**
     * 应用名称列表
     */
    @JsonProperty("app_names")
    private List<String> appNames;
}
