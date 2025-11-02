package com.datacollect.controller;

import com.datacollect.common.Result;
import com.datacollect.service.DashboardCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 仪表盘控制器
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardCacheService dashboardCacheService;

    /**
     * 获取仪表盘统计数据（从缓存读取）
     * 
     * @return 统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        log.debug("Getting dashboard statistics from cache");
        
        try {
            Map<String, Object> stats = dashboardCacheService.getStats();
            
            if (stats == null || stats.isEmpty()) {
                log.warn("Dashboard statistics cache is empty, returning empty result");
                return Result.success(new java.util.HashMap<>());
            }
            
            log.debug("Dashboard statistics retrieved from cache successfully");
            return Result.success(stats);
            
        } catch (Exception e) {
            log.error("Failed to get dashboard statistics from cache - error: {}", e.getMessage(), e);
            return Result.error("Failed to get dashboard statistics: " + e.getMessage());
        }
    }

    /**
     * 获取地域统计数据（从缓存读取）
     * 
     * @param regionId 地域ID（城市ID或国家ID）
     * @return 统计数据
     */
    @GetMapping("/region-stats")
    public Result<Map<String, Object>> getRegionStats(@RequestParam @NotNull Long regionId) {
        log.debug("Getting region statistics from cache - region ID: {}", regionId);
        
        try {
            Map<String, Object> stats = dashboardCacheService.getRegionStats(regionId);
            
            if (stats == null || stats.isEmpty()) {
                log.warn("Region statistics cache is empty for region ID: {}", regionId);
                return Result.error("Region statistics not found in cache for region ID: " + regionId + ". Please wait for the scheduled task to update.");
            }
            
            log.debug("Region statistics retrieved from cache successfully - region ID: {}", regionId);
            return Result.success(stats);
            
        } catch (Exception e) {
            log.error("Failed to get region statistics from cache - region ID: {}, error: {}", regionId, e.getMessage(), e);
            return Result.error("Failed to get region statistics: " + e.getMessage());
        }
    }
}

