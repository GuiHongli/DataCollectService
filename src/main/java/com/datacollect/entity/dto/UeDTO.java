package com.datacollect.entity.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UeDTO {
    
    private Long id;
    private String ueId;
    private String name;
    private String purpose;
    private Long networkTypeId;
    private String networkTypeName; // 网络类型名称
    private String description;
    private Integer status;
    private String createBy;
    private String updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
