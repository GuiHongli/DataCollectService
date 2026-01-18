package com.datacollect.controller;

import com.datacollect.common.Result;
import com.datacollect.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 系统配置控制器
 * 
 * @author system
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/config")
@Validated
public class ConfigController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigController.class);
    
    @Autowired
    private ConfigService configService;
    
    /**
     * 获取UE使用中是否禁用环境的配置
     * 
     * @return 配置值（true表示禁用环境，false表示不禁用）
     */
    @GetMapping("/ue-disable-environment-when-in-use")
    public Result<Boolean> getUeDisableEnvironmentWhenInUse() {
        try {
            Boolean enabled = configService.getUeDisableEnvironmentWhenInUse();
            return Result.success(enabled);
        } catch (Exception e) {
            LOGGER.error("Failed to get configuration - error: {}", e.getMessage(), e);
            return Result.error("获取配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置UE使用中是否禁用环境的配置
     * 
     * @param request 配置请求，包含enabled字段
     * @return 设置结果
     */
    @PutMapping("/ue-disable-environment-when-in-use")
    public Result<Boolean> setUeDisableEnvironmentWhenInUse(@RequestBody Map<String, Boolean> request) {
        try {
            Boolean enabled = request.get("enabled");
            if (enabled == null) {
                return Result.error("参数enabled不能为空");
            }
            
            configService.setUeDisableEnvironmentWhenInUse(enabled);
            LOGGER.info("Set UE disable environment when in use configuration - enabled: {}", enabled);
            return Result.success(enabled);
        } catch (Exception e) {
            LOGGER.error("Failed to set configuration - error: {}", e.getMessage(), e);
            return Result.error("设置配置失败: " + e.getMessage());
        }
    }
}

