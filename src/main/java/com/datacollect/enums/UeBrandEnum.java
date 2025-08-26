package com.datacollect.enums;

import lombok.Getter;

/**
 * UE品牌枚举
 * 
 * @author system
 * @since 2024-01-01
 */
@Getter
public enum UeBrandEnum {
    
    XIAOMI("xiaomi", "小米"),
    OPPO("oppo", "OPPO"),
    VIVO("vivo", "vivo"),
    SAMSUNG("samsung", "三星"),
    HONOR("honor", "荣耀"),
    HUAWEI("huawei", "华为"),
    APPLE("apple", "苹果"),
    HUAWEI_HISILICON("Huawei-Hisilicon", "华为海思");
    
    private final String code;
    private final String name;
    
    UeBrandEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    /**
     * 根据代码获取品牌名称
     * 
     * @param code 品牌代码
     * @return 品牌名称
     */
    public static String getNameByCode(String code) {
        for (UeBrandEnum brand : values()) {
            if (brand.getCode().equals(code)) {
                return brand.getName();
            }
        }
        return code; // 如果找不到对应的名称，返回代码本身
    }
    
    /**
     * 根据代码获取品牌枚举
     * 
     * @param code 品牌代码
     * @return 品牌枚举
     */
    public static UeBrandEnum getByCode(String code) {
        for (UeBrandEnum brand : values()) {
            if (brand.getCode().equals(code)) {
                return brand;
            }
        }
        return null;
    }
    
    /**
     * 检查品牌代码是否有效
     * 
     * @param code 品牌代码
     * @return 是否有效
     */
    public static boolean isValidCode(String code) {
        return getByCode(code) != null;
    }
    
    /**
     * 获取所有品牌代码
     * 
     * @return 品牌代码数组
     */
    public static String[] getAllCodes() {
        UeBrandEnum[] brands = values();
        String[] codes = new String[brands.length];
        for (int i = 0; i < brands.length; i++) {
            codes[i] = brands[i].getCode();
        }
        return codes;
    }
    
    /**
     * 获取所有品牌名称
     * 
     * @return 品牌名称数组
     */
    public static String[] getAllNames() {
        UeBrandEnum[] brands = values();
        String[] names = new String[brands.length];
        for (int i = 0; i < brands.length; i++) {
            names[i] = brands[i].getName();
        }
        return names;
    }
}
