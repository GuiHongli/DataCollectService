package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.dto.AppVersionAutoCollectRequest;
import com.datacollect.entity.AppVersionAutoCollect;

/**
 * app版本变更自动采集配置服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface AppVersionAutoCollectService extends IService<AppVersionAutoCollect> {
    
    /**
     * 保存或更新自动采集配置
     * 
     * @param request 配置请求
     * @return 配置ID
     */
    Long saveOrUpdateConfig(AppVersionAutoCollectRequest request);
    
    /**
     * 根据应用名称和平台类型获取配置
     * 
     * @param appName 应用名称
     * @param platformType 平台类型
     * @return 配置信息
     */
    AppVersionAutoCollect getByAppNameAndPlatform(String appName, Boolean platformType);
}

