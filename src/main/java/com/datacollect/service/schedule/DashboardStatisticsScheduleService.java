package com.datacollect.service.schedule;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datacollect.entity.Region;
import com.datacollect.service.DashboardCacheService;
import com.datacollect.service.DashboardStatisticsService;
import com.datacollect.service.RegionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计数据定时任务服务
 * 每30分钟执行一次统计并更新缓存
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class DashboardStatisticsScheduleService {
    
    @Autowired
    private DashboardStatisticsService dashboardStatisticsService;
    
    @Autowired
    private DashboardCacheService dashboardCacheService;
    
    @Autowired
    private RegionService regionService;
    
    /**
     * 系统启动时立即执行一次统计
     */
    @PostConstruct
    public void init() {
        log.info("Initializing dashboard statistics cache...");
        updateAllStatistics();
    }
    
    /**
     * 每30分钟执行一次仪表盘统计数据更新
     * cron表达式: 0 */30 * * * ? 表示每30分钟执行一次
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void scheduledUpdateStatistics() {
        log.info("Scheduled task: Starting dashboard statistics update...");
        updateAllStatistics();
        log.info("Scheduled task: Dashboard statistics update completed");
    }
    
    /**
     * 更新所有统计数据
     */
    private void updateAllStatistics() {
        try {
            // 1. 更新总体统计数据
            log.info("Updating dashboard overall statistics...");
            Map<String, Object> stats = dashboardStatisticsService.calculateStats();
            dashboardCacheService.updateStats(stats);
            log.info("Dashboard overall statistics updated successfully");
            
            // 2. 更新所有地域统计数据
            log.info("Updating region statistics...");
            updateAllRegionStatistics();
            log.info("All region statistics updated successfully");
            
        } catch (Exception e) {
            log.error("Failed to update dashboard statistics - error: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 更新所有地域的统计数据
     */
    private void updateAllRegionStatistics() {
        try {
            // 获取所有城市和国家级别的地域（level=2或level=4）
            QueryWrapper<Region> regionQuery = new QueryWrapper<>();
            regionQuery.in("level", 2, 4);
            regionQuery.eq("deleted", 0);
            List<Region> regions = regionService.list(regionQuery);
            
            log.info("Found {} regions to update statistics", regions.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (Region region : regions) {
                try {
                    Map<String, Object> stats = dashboardStatisticsService.calculateRegionStats(region.getId());
                    dashboardCacheService.updateRegionStats(region.getId(), stats);
                    successCount++;
                    log.debug("Region statistics updated - region ID: {}, name: {}", 
                            region.getId(), region.getName());
                } catch (Exception e) {
                    failCount++;
                    log.warn("Failed to update region statistics - region ID: {}, name: {}, error: {}", 
                            region.getId(), region.getName(), e.getMessage());
                }
            }
            
            log.info("Region statistics update completed - success: {}, failed: {}", successCount, failCount);
            
        } catch (Exception e) {
            log.error("Failed to update all region statistics - error: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 手动触发统计更新（用于测试或手动刷新）
     */
    public void manualUpdate() {
        log.info("Manual trigger: Starting dashboard statistics update...");
        updateAllStatistics();
        log.info("Manual trigger: Dashboard statistics update completed");
    }
}

