package com.datacollect.service.schedule;

import com.alibaba.fastjson.JSON;
import com.datacollect.dto.CollectTaskRequest;
import com.datacollect.dto.GetVersionHistoryRequest;
import com.datacollect.dto.GetVersionHistoryResponse;
import com.datacollect.entity.AppVersionAutoCollect;
import com.datacollect.entity.CollectTaskTemplate;
import com.datacollect.service.AppVersionAutoCollectService;
import com.datacollect.service.CollectTaskProcessService;
import com.datacollect.service.CollectTaskTemplateService;
import com.datacollect.service.ExternalApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * app版本变更自动采集定时任务服务
 * 每天3点执行，检查版本变更并自动触发采集任务
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class AppVersionAutoCollectScheduleService {
    
    @Autowired
    private ExternalApiService externalApiService;
    
    @Autowired
    private AppVersionAutoCollectService appVersionAutoCollectService;
    
    @Autowired
    private CollectTaskTemplateService collectTaskTemplateService;
    
    @Autowired
    private CollectTaskProcessService collectTaskProcessService;
    
    /**
     * 每天3点执行自动采集任务
     * cron表达式: 0 0 3 * * ? 表示每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledAutoCollect() {
        log.info("Scheduled task: Starting app version auto collect check...");
        
        try {
            // 检查安卓平台
            checkAndTriggerAutoCollect(false);
            
            // 检查iOS平台
            checkAndTriggerAutoCollect(true);
            
            log.info("Scheduled task: App version auto collect check completed");
            
        } catch (Exception e) {
            log.error("Scheduled task: Failed to check app version auto collect - error: {}", e.getMessage(), e);
            // 不抛出异常，避免影响定时任务继续执行
        }
    }
    
    /**
     * 检查并触发自动采集
     * 
     * @param isIos 是否为iOS平台
     */
    private void checkAndTriggerAutoCollect(boolean isIos) {
        log.info("Checking app version auto collect for platform: {}", isIos ? "iOS" : "Android");
        
        try {
            // 1. 获取版本历史信息
            GetVersionHistoryRequest request = new GetVersionHistoryRequest();
            request.setIsIos(isIos);
            
            GetVersionHistoryResponse response = externalApiService.getVersionHistory(request);
            
            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                log.info("No version history data found for platform: {}", isIos ? "iOS" : "Android");
                return;
            }
            
            // 2. 遍历每个应用，检查版本变更
            for (GetVersionHistoryResponse.VersionHistoryData appData : response.getData()) {
                try {
                    checkAndTriggerForApp(appData, isIos);
                } catch (Exception e) {
                    log.error("Failed to check and trigger auto collect for app: {} - error: {}", 
                            appData.getAppName(), e.getMessage(), e);
                    // 继续处理下一个应用，不中断整个流程
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to check app version auto collect for platform: {} - error: {}", 
                    isIos ? "iOS" : "Android", e.getMessage(), e);
        }
    }
    
    /**
     * 检查单个应用并触发自动采集
     * 
     * @param appData 应用版本数据
     * @param isIos 是否为iOS平台
     */
    private void checkAndTriggerForApp(GetVersionHistoryResponse.VersionHistoryData appData, boolean isIos) {
        String appName = appData.getAppName();
        String appVersion = appData.getAppVersion();
        String dialVersion = appData.getDialVerion();
        
        if (appName == null || appName.trim().isEmpty()) {
            log.warn("App name is empty, skipping...");
            return;
        }
        
        // 检查版本和采集版本是否一致
        if (appVersion != null && appVersion.equals(dialVersion)) {
            log.debug("App version matches dial version for app: {} - version: {}, skipping...", appName, appVersion);
            return;
        }
        
        log.info("App version mismatch detected for app: {} - version: {}, dial version: {}", 
                appName, appVersion, dialVersion);
        
        // 检查是否开启了自动采集
        AppVersionAutoCollect config = appVersionAutoCollectService.getByAppNameAndPlatform(appName, isIos);
        
        if (config == null || !config.getAutoCollect()) {
            log.debug("Auto collect is not enabled for app: {}, skipping...", appName);
            return;
        }
        
        if (config.getTemplateId() == null) {
            log.warn("Auto collect is enabled but no template configured for app: {}, skipping...", appName);
            return;
        }
        
        // 获取模版
        CollectTaskTemplate template = collectTaskTemplateService.getById(config.getTemplateId());
        if (template == null) {
            log.error("Template not found for app: {}, template ID: {}", appName, config.getTemplateId());
            return;
        }
        
        // 从模版创建采集任务
        try {
            createTaskFromTemplate(template, appData, isIos);
            log.info("Auto collect task created successfully for app: {}", appName);
        } catch (Exception e) {
            log.error("Failed to create auto collect task for app: {} - error: {}", appName, e.getMessage(), e);
        }
    }
    
    /**
     * 从模版创建采集任务
     * 
     * @param template 采集任务模版
     * @param appData 应用版本数据
     * @param isIos 是否为iOS平台
     */
    private void createTaskFromTemplate(CollectTaskTemplate template, 
                                       GetVersionHistoryResponse.VersionHistoryData appData, 
                                       boolean isIos) {
        log.info("Creating collect task from template for app: {} - template ID: {}", 
                appData.getAppName(), template.getId());
        
        // 构建任务请求
        CollectTaskRequest request = new CollectTaskRequest();
        
        // 替换任务名称为"应用-版本拨测任务"
        String taskName = String.format("%s-%s拨测任务", appData.getAppName(), appData.getAppVersion());
        request.setName(taskName);
        
        // 替换任务描述为"应用：应用名称，版本：应用版本，类别：应用类别，描述：应用描述"
        StringBuilder description = new StringBuilder();
        description.append("应用：").append(appData.getAppName() != null ? appData.getAppName() : "");
        if (appData.getAppVersion() != null) {
            description.append("，版本：").append(appData.getAppVersion());
        }
        if (appData.getAppCategory() != null) {
            description.append("，类别：").append(appData.getAppCategory());
        }
        if (appData.getAppDescription() != null) {
            description.append("，描述：").append(appData.getAppDescription());
        }
        request.setDescription(description.toString());
        
        // 从模版复制配置
        request.setCollectStrategyId(template.getCollectStrategyId());
        request.setCollectCount(template.getCollectCount());
        request.setRegionId(template.getRegionId());
        request.setCountryId(template.getCountryId());
        request.setProvinceId(template.getProvinceId());
        request.setCityId(template.getCityId());
        request.setNetwork(template.getNetwork());
        
        // 解析网元ID列表
        if (template.getNetworkElementIds() != null && !template.getNetworkElementIds().trim().isEmpty()) {
            try {
                List<Long> networkElementIds = JSON.parseArray(template.getNetworkElementIds(), Long.class);
                request.setNetworkElementIds(networkElementIds);
            } catch (Exception e) {
                log.warn("Failed to parse network element IDs from template: {}", e.getMessage());
            }
        }
        
        // 解析厂商列表
        if (template.getManufacturer() != null && !template.getManufacturer().trim().isEmpty()) {
            try {
                List<String> manufacturer = JSON.parseArray(template.getManufacturer(), String.class);
                request.setManufacturer(manufacturer);
            } catch (Exception e) {
                log.warn("Failed to parse manufacturer from template: {}", e.getMessage());
            }
        }
        
        // 解析逻辑环境ID列表
        if (template.getLogicEnvironmentIds() != null && !template.getLogicEnvironmentIds().trim().isEmpty()) {
            try {
                List<Long> logicEnvironmentIds = JSON.parseArray(template.getLogicEnvironmentIds(), Long.class);
                request.setLogicEnvironmentIds(logicEnvironmentIds);
            } catch (Exception e) {
                log.error("Failed to parse logic environment IDs from template: {}", e.getMessage());
                throw new RuntimeException("Failed to parse logic environment IDs: " + e.getMessage(), e);
            }
        } else {
            log.error("Template has no logic environment IDs configured - template ID: {}", template.getId());
            throw new RuntimeException("Template has no logic environment IDs configured");
        }
        
        // 复制任务级别自定义参数
        request.setTaskCustomParams(template.getTaskCustomParams());
        
        // 复制用例配置
        request.setCustomParams(template.getCustomParams());
        
        // 创建采集任务（使用系统用户）
        String createBy = "system";
        collectTaskProcessService.processCollectTaskCreation(request, createBy);
        
        log.info("Collect task created successfully from template - app: {}, task name: {}", 
                appData.getAppName(), taskName);
    }
    
    /**
     * 手动触发自动采集检查（用于测试或手动刷新）
     */
    public void manualTrigger() {
        log.info("Manual trigger: Starting app version auto collect check...");
        
        try {
            // 检查安卓平台
            checkAndTriggerAutoCollect(false);
            
            // 检查iOS平台
            checkAndTriggerAutoCollect(true);
            
            log.info("Manual trigger: App version auto collect check completed");
            
        } catch (Exception e) {
            log.error("Manual trigger: Failed to check app version auto collect - error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to check app version auto collect: " + e.getMessage(), e);
        }
    }
}

