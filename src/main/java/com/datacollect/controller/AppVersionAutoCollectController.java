package com.datacollect.controller;

import com.datacollect.common.Result;
import com.datacollect.dto.AppVersionAutoCollectRequest;
import com.datacollect.entity.AppVersionAutoCollect;
import com.datacollect.service.AppVersionAutoCollectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * app版本变更自动采集配置控制器
 * 
 * @author system
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/app-version-auto-collect")
@Validated
public class AppVersionAutoCollectController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppVersionAutoCollectController.class);
    
    @Autowired
    private AppVersionAutoCollectService appVersionAutoCollectService;
    
    /**
     * 保存或更新自动采集配置
     * 
     * @param request 配置请求
     * @return 配置ID
     */
    @PostMapping
    public Result<Map<String, Object>> saveOrUpdateConfig(@Valid @RequestBody AppVersionAutoCollectRequest request) {
        LOGGER.info("保存或更新自动采集配置 - 应用名称: {}, 平台类型: {}, 自动采集: {}", 
                request.getAppName(), request.getPlatformType(), request.getAutoCollect());
        
        try {
            // 如果enabled自动采集但未选择模版，返回错误
            if (request.getAutoCollect() && request.getTemplateId() == null) {
                return Result.error("启用自动采集时必须选择采集任务模版");
            }
            
            Long configId = appVersionAutoCollectService.saveOrUpdateConfig(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("configId", configId);
            result.put("message", "配置保存成功");
            
            LOGGER.info("自动采集配置savesuccess - 配置ID: {}", configId);
            return Result.success(result);
            
        } catch (Exception e) {
            LOGGER.error("save自动采集配置failed - 应用名称: {}, error: {}", request.getAppName(), e.getMessage(), e);
            return Result.error("保存配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量获取自动采集配置
     * 
     * @param appNames 应用名称列表（逗号分隔）
     * @param platformType 平台类型
     * @return 配置列表
     */
    @GetMapping("/batch")
    public Result<Map<String, AppVersionAutoCollect>> getBatchConfigs(
            @RequestParam @NotBlank String appNames,
            @RequestParam @NotNull Boolean platformType) {
        LOGGER.info("批量get自动采集配置 - 应用名称列表: {}, 平台类型: {}", appNames, platformType);
        
        try {
            String[] appNameArray = appNames.split(",");
            Map<String, AppVersionAutoCollect> configMap = new HashMap<>();
            
            for (String appName : appNameArray) {
                if (appName != null && !appName.trim().isEmpty()) {
                    AppVersionAutoCollect config = appVersionAutoCollectService.getByAppNameAndPlatform(
                            appName.trim(), platformType);
                    if (config != null) {
                        configMap.put(appName.trim(), config);
                    }
                }
            }
            
            return Result.success(configMap);
            
        } catch (Exception e) {
            LOGGER.error("批量get自动采集配置failed - error: {}", e.getMessage(), e);
            return Result.error("获取配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据应用名称和平台类型获取配置
     * 
     * @param appName 应用名称
     * @param platformType 平台类型
     * @return 配置信息
     */
    @GetMapping
    public Result<AppVersionAutoCollect> getConfig(
            @RequestParam @NotBlank String appName,
            @RequestParam @NotNull Boolean platformType) {
        LOGGER.info("get自动采集配置 - 应用名称: {}, 平台类型: {}", appName, platformType);
        
        try {
            AppVersionAutoCollect config = appVersionAutoCollectService.getByAppNameAndPlatform(appName, platformType);
            return Result.success(config);
            
        } catch (Exception e) {
            LOGGER.error("get自动采集配置failed - 应用名称: {}, error: {}", appName, e.getMessage(), e);
            return Result.error("获取配置失败: " + e.getMessage());
        }
    }
}

