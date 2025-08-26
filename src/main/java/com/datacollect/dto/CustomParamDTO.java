package com.datacollect.dto;

import lombok.Data;

/**
 * 自定义参数DTO
 */
@Data
public class CustomParamDTO {
    
    /**
     * 参数键
     */
    private String key;
    
    /**
     * 参数值
     */
    private String value;
    
    public CustomParamDTO() {}
    
    public CustomParamDTO(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
