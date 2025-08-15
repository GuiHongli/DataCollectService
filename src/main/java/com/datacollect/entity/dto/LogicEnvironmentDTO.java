package com.datacollect.entity.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LogicEnvironmentDTO {
    
    private Long id;
    private String name;
    private Long executorId;
    
    // 执行机信息
    private String executorName;
    private String executorIpAddress;
    private String executorRegionName;
    
    // UE信息列表
    private List<UeInfo> ueList;
    
    // 逻辑组网信息列表
    private List<NetworkInfo> networkList;
    
    private String description;
    private Integer status;
    private String createBy;
    private String updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    @Data
    public static class UeInfo {
        private Long id;
        private String ueId;
        private String name;
        private String purpose;
        private String networkTypeName;
    }
    
    @Data
    public static class NetworkInfo {
        private Long id;
        private String name;
        private String description;
    }
}
