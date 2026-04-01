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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * app版本变更自动采集定时任务服务
 * 每天3点执行，检查版本变更并自动触发采集任务
 * 
 * @author system
 * @since 2024-01-01
 */
@Service
public class AppVersionAutoCollectScheduleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppVersionAutoCollectScheduleService.class);
    
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
        LOGGER.info("Scheduled task: Starting app version auto collect check...");
        
        try {
            // check安卓平台
            checkAndTriggerAutoCollect(false);
            
            // checkiOS平台
            checkAndTriggerAutoCollect(true);
            
            LOGGER.info("Scheduled task: App version auto collect check completed");
            
        } catch (Exception e) {
            LOGGER.error("Scheduled task: Failed to check app version auto collect - error: {}", e.getMessage(), e);
            // 不抛出异常，避免影响定时任务continue执行
        }
    }
    
    /**
     * check并触发自动采集
     * 
     * @param isIos 是否为iOS平台
     */
    private void checkAndTriggerAutoCollect(boolean isIos) {
        LOGGER.info("Checking app version auto collect for platform: {}", isIos ? "iOS" : "Android");
        
        try {
            // 1. 获取版本历史信息
            GetVersionHistoryRequest request = new GetVersionHistoryRequest();
            request.setIsIos(isIos);
            
            GetVersionHistoryResponse response = externalApiService.getVersionHistory(request);
            
            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                LOGGER.info("No version history data found for platform: {}", isIos ? "iOS" : "Android");
                return;
            }
            
            // 2. 遍历每个应用，check版本变更
            for (GetVersionHistoryResponse.VersionHistoryData appData : response.getData()) {
                try {
                    checkAndTriggerForApp(appData, isIos);
                } catch (Exception e) {
                    LOGGER.error("Failed to check and trigger auto collect for app: {} - error: {}", 
                            appData.getAppName(), e.getMessage(), e);
                    // 继续处理下一个应用，不中断整个流程
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to check app version auto collect for platform: {} - error: {}", 
                    isIos ? "iOS" : "Android", e.getMessage(), e);
        }
    }
    
    /**
     * check单个应用并触发自动采集
     * 
     * @param appData 应用版本数据
     * @param isIos 是否为iOS平台
     */
    private void checkAndTriggerForApp(GetVersionHistoryResponse.VersionHistoryData appData, boolean isIos) {
        String appName = appData.getAppName();
        String appVersion = appData.getAppVersion();
        String dialVersion = appData.getDialVerion();
        
        if (appName == null || appName.trim().isEmpty()) {
            LOGGER.warn("App name is empty, skipping...");
            return;
        }
        
        // 检查版本和采集版本是否一致
        if (appVersion != null && appVersion.equals(dialVersion)) {
            LOGGER.debug("App version matches dial version for app: {} - version: {}, skipping...", appName, appVersion);
            return;
        }
        
        LOGGER.info("App version mismatch detected for app: {} - version: {}, dial version: {}", 
                appName, appVersion, dialVersion);
        
        // check是否开启了自动采集
        AppVersionAutoCollect config = appVersionAutoCollectService.getByAppNameAndPlatform(appName, isIos);
        
        if (config == null || !config.getAutoCollect()) {
            LOGGER.debug("Auto collect is not enabled for app: {}, skipping...", appName);
            return;
        }
        
        if (config.getTemplateId() == null) {
            LOGGER.warn("Auto collect is enabled but no template configured for app: {}, skipping...", appName);
            return;
        }
        
        // get模版
        CollectTaskTemplate template = collectTaskTemplateService.getById(config.getTemplateId());
        if (template == null) {
            LOGGER.error("Template not found for app: {}, template ID: {}", appName, config.getTemplateId());
            return;
        }
        
        // 从模版创建采集任务
        try {
            createTaskFromTemplate(template, appData, isIos);
            LOGGER.info("Auto collect task created successfully for app: {}", appName);
        } catch (Exception e) {
            LOGGER.error("Failed to create auto collect task for app: {} - error: {}", appName, e.getMessage(), e);
        }
    }
    
    /**
     * 从模版create采集任务
     * 
     * @param template 采集任务模版
     * @param appData 应用版本数据
     * @param isIos 是否为iOS平台
     */
    private void createTaskFromTemplate(CollectTaskTemplate template, 
                                       GetVersionHistoryResponse.VersionHistoryData appData, 
                                       boolean isIos) {
        LOGGER.info("Creating collect task from template for app: {} - template ID: {}", 
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
                LOGGER.warn("Failed to parse network element IDs from template: {}", e.getMessage());
            }
        }
        
        // 解析厂商列表
        if (template.getManufacturer() != null && !template.getManufacturer().trim().isEmpty()) {
            try {
                List<String> manufacturer = JSON.parseArray(template.getManufacturer(), String.class);
                request.setManufacturer(manufacturer);
            } catch (Exception e) {
                LOGGER.warn("Failed to parse manufacturer from template: {}", e.getMessage());
            }
        }
        
        // 解析逻辑环境ID列表
        if (template.getLogicEnvironmentIds() != null && !template.getLogicEnvironmentIds().trim().isEmpty()) {
            try {
                List<Long> logicEnvironmentIds = JSON.parseArray(template.getLogicEnvironmentIds(), Long.class);
                request.setLogicEnvironmentIds(logicEnvironmentIds);
            } catch (Exception e) {
                LOGGER.error("Failed to parse logic environment IDs from template: {}", e.getMessage());
                throw new RuntimeException("Failed to parse logic environment IDs: " + e.getMessage(), e);
            }
        } else {
            LOGGER.error("Template has no logic environment IDs configured - template ID: {}", template.getId());
            throw new RuntimeException("Template has no logic environment IDs configured");
        }
        
        // 复制任务级别自定义参数
        request.setTaskCustomParams(template.getTaskCustomParams());
        
        // 复制用例配置
        request.setCustomParams(template.getCustomParams());
        
        // 创建采集任务（使用系统用户）
        String createBy = "system";
        collectTaskProcessService.processCollectTaskCreation(request, createBy);
        
        LOGGER.info("Collect task created successfully from template - app: {}, task name: {}", 
                appData.getAppName(), taskName);
    }
    
    /**
     * 手动触发自动采集check（用于测试或手动刷新）
     */
    public void manualTrigger() {
        LOGGER.info("Manual trigger: Starting app version auto collect check...");
        
        try {
            // 检查安卓平台
            checkAndTriggerAutoCollect(false);
            
            // 检查iOS平台
            checkAndTriggerAutoCollect(true);
            
            LOGGER.info("Manual trigger: App version auto collect check completed");
            
        } catch (Exception e) {
            LOGGER.error("Manual trigger: Failed to check app version auto collect - error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to check app version auto collect: " + e.getMessage(), e);
        }
    }
}

