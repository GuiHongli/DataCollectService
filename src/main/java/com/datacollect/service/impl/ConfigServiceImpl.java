package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.SystemConfig;
import com.datacollect.mapper.SystemConfigMapper;
import com.datacollect.service.ConfigService;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 系统配置服务实现类
 * 使用数据库存储配置
 * 
 * @author system
 * @since 2024-01-01
 */
@Service
public class ConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements ConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigServiceImpl.class);
    
    /**
     * 获取系统配置（只允许一条记录）
     * 
     * @return 系统配置对象
     */
    private SystemConfig getSystemConfig() {
        QueryWrapper<SystemConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("config_key", "default");
        queryWrapper.last("LIMIT 1");
        SystemConfig config = getOne(queryWrapper);
        
        // 如果不存在，创建默认配置
        if (config == null) {
            config = new SystemConfig();
            config.setConfigKey("default");
            config.setUeDisableEnvironmentWhenInUse(false);
            config.setCreateBy("system");
            save(config);
            LOGGER.info("创建默认系统配置");
        }
        
        return config;
    }
    
    @Override
    public Boolean getUeDisableEnvironmentWhenInUse() {
        SystemConfig config = getSystemConfig();
        Boolean value = config.getUeDisableEnvironmentWhenInUse();
        // 如果为null，返回默认值false
        return value != null ? value : false;
    }
    
    @Override
    public void setUeDisableEnvironmentWhenInUse(Boolean enabled) {
        if (enabled == null) {
            enabled = false;
        }
        
        SystemConfig config = getSystemConfig();
        config.setUeDisableEnvironmentWhenInUse(enabled);
        config.setUpdateBy("system");
        updateById(config);
        
        LOGGER.info("Set UE disable environment when in use configuration - enabled: {}", enabled);
    }
}

