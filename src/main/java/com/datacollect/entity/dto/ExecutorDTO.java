package com.datacollect.entity.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExecutorDTO {
    
    private Long id;
    private String ipAddress;
    private String name;
    private Long regionId;
    private String regionName;      // 地域名称（完整路径：地域+国家+省份+城市）
    private String description;
    private Integer status;
    private String macAddress;      // MAC地址（从executor_mac_address表获取）
    private Long macAddressId;       // MAC地址ID（关联executor_mac_address表的id）
    private String createBy;
    private String updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
