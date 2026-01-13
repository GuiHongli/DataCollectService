package com.datacollect.service.impl;

import com.datacollect.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 系统配置服务实现类
 * 使用内存存储配置，默认值为false（不禁用环境）
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class ConfigServiceImpl implements ConfigService {
    
    /**
     * UE使用中是否禁用环境的配置
     * 默认值为false，表示UE使用中时不禁用环境
     */
    private final AtomicBoolean ueDisableEnvironmentWhenInUse = new AtomicBoolean(false);
    
    @Override
    public Boolean getUeDisableEnvironmentWhenInUse() {
        return ueDisableEnvironmentWhenInUse.get();
    }
    
    @Override
    public void setUeDisableEnvironmentWhenInUse(Boolean enabled) {
        if (enabled == null) {
            enabled = false;
        }
        ueDisableEnvironmentWhenInUse.set(enabled);
        log.info("设置UE使用中禁用环境配置 - enabled: {}", enabled);
    }
}

