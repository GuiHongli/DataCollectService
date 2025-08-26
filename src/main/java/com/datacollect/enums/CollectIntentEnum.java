package com.datacollect.enums;

import lombok.Getter;

/**
 * 采集意图枚举
 */
@Getter
public enum CollectIntentEnum {
    
    DATASET("dataset", "训练集"),
    TEST_DATASET("test_dataset", "测试集");
    
    private final String code;
    private final String name;
    
    CollectIntentEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    /**
     * 根据code获取枚举
     */
    public static CollectIntentEnum getByCode(String code) {
        for (CollectIntentEnum intent : values()) {
            if (intent.getCode().equals(code)) {
                return intent;
            }
        }
        return null;
    }
    
    /**
     * 根据name获取枚举
     */
    public static CollectIntentEnum getByName(String name) {
        for (CollectIntentEnum intent : values()) {
            if (intent.getName().equals(name)) {
                return intent;
            }
        }
        return null;
    }
}
