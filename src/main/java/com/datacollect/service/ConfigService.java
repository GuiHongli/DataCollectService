package com.datacollect.service;

/**
 * 系统配置服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface ConfigService {
    
    /**
     * 获取UE使用中是否禁用环境的配置
     * 
     * @return true表示UE使用中时禁用环境，false表示不禁用环境
     */
    Boolean getUeDisableEnvironmentWhenInUse();
    
    /**
     * 设置UE使用中是否禁用环境的配置
     * 
     * @param enabled true表示UE使用中时禁用环境，false表示不禁用环境
     */
    void setUeDisableEnvironmentWhenInUse(Boolean enabled);
}

