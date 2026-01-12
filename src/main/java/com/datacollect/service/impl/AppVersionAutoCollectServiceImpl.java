package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.dto.AppVersionAutoCollectRequest;
import com.datacollect.entity.AppVersionAutoCollect;
import com.datacollect.mapper.AppVersionAutoCollectMapper;
import com.datacollect.service.AppVersionAutoCollectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * app版本变更自动采集配置服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class AppVersionAutoCollectServiceImpl extends ServiceImpl<AppVersionAutoCollectMapper, AppVersionAutoCollect> implements AppVersionAutoCollectService {
    
    @Override
    public Long saveOrUpdateConfig(AppVersionAutoCollectRequest request) {
        log.info("保存或更新自动采集配置 - 应用名称: {}, 平台类型: {}, 自动采集: {}", 
                request.getAppName(), request.getPlatformType(), request.getAutoCollect());
        
        // 查询是否已存在配置
        QueryWrapper<AppVersionAutoCollect> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_name", request.getAppName());
        queryWrapper.eq("platform_type", request.getPlatformType() ? 1 : 0);
        queryWrapper.eq("deleted", 0);
        
        AppVersionAutoCollect existingConfig = getOne(queryWrapper);
        
        AppVersionAutoCollect config;
        if (existingConfig != null) {
            // 更新现有配置
            config = existingConfig;
            config.setAutoCollect(request.getAutoCollect());
            config.setTemplateId(request.getTemplateId());
            config.setUpdateTime(LocalDateTime.now());
            
            boolean success = updateById(config);
            if (success) {
                log.info("自动采集配置更新成功 - 配置ID: {}", config.getId());
                return config.getId();
            } else {
                log.error("自动采集配置更新失败 - 应用名称: {}", request.getAppName());
                throw new RuntimeException("自动采集配置更新失败");
            }
        } else {
            // 创建新配置
            config = new AppVersionAutoCollect();
            config.setAppName(request.getAppName());
            config.setPlatformType(request.getPlatformType());
            config.setAutoCollect(request.getAutoCollect());
            config.setTemplateId(request.getTemplateId());
            config.setDeleted(0);
            
            LocalDateTime now = LocalDateTime.now();
            config.setCreateTime(now);
            config.setUpdateTime(now);
            
            boolean success = save(config);
            if (success) {
                log.info("自动采集配置创建成功 - 配置ID: {}", config.getId());
                return config.getId();
            } else {
                log.error("自动采集配置创建失败 - 应用名称: {}", request.getAppName());
                throw new RuntimeException("自动采集配置创建失败");
            }
        }
    }
    
    @Override
    public AppVersionAutoCollect getByAppNameAndPlatform(String appName, Boolean platformType) {
        QueryWrapper<AppVersionAutoCollect> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_name", appName);
        queryWrapper.eq("platform_type", platformType ? 1 : 0);
        queryWrapper.eq("deleted", 0);
        
        return getOne(queryWrapper);
    }
}

