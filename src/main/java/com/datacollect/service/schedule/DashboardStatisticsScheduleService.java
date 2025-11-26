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
     * 从最小地域（level=4，城市）开始统计，然后向上汇总到level=3（省份）、level=2（国家）、level=1（片区）
     */
    private void updateAllRegionStatistics() {
        try {
            // 第一步：统计最小地域（level=4，城市）
            log.info("Step 1: Updating statistics for level 4 regions (cities)...");
            updateRegionsByLevel(4);
            
            // 第二步：汇总level=3（省份）的统计数据
            log.info("Step 2: Aggregating statistics for level 3 regions (provinces)...");
            aggregateRegionsByLevel(3, 4);
            
            // 第三步：汇总level=2（国家）的统计数据
            log.info("Step 3: Aggregating statistics for level 2 regions (countries)...");
            aggregateRegionsByLevel(2, 4);
            
            // 第四步：汇总level=1（片区）的统计数据
            log.info("Step 4: Aggregating statistics for level 1 regions (regions)...");
            aggregateRegionsByLevel(1, 2);
            
            log.info("All region statistics update completed");
            
        } catch (Exception e) {
            log.error("Failed to update all region statistics - error: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 更新指定层级的地域统计数据（直接计算，不汇总）
     * 
     * @param level 地域层级
     */
    private void updateRegionsByLevel(Integer level) {
        try {
            QueryWrapper<Region> regionQuery = new QueryWrapper<>();
            regionQuery.eq("level", level);
            regionQuery.eq("deleted", 0);
            List<Region> regions = regionService.list(regionQuery);
            
            log.info("Found {} level {} regions to update statistics", regions.size(), level);
            
            int successCount = 0;
            int failCount = 0;
            
            for (Region region : regions) {
                try {
                    Map<String, Object> stats = dashboardStatisticsService.calculateRegionStats(region.getId());
                    dashboardCacheService.updateRegionStats(region.getId(), stats);
                    successCount++;
                    log.debug("Region statistics updated - region ID: {}, name: {}, level: {}", 
                            region.getId(), region.getName(), level);
                } catch (Exception e) {
                    failCount++;
                    log.warn("Failed to update region statistics - region ID: {}, name: {}, level: {}, error: {}", 
                            region.getId(), region.getName(), level, e.getMessage());
                }
            }
            
            log.info("Level {} region statistics update completed - success: {}, failed: {}", 
                    level, successCount, failCount);
            
        } catch (Exception e) {
            log.error("Failed to update level {} region statistics - error: {}", level, e.getMessage(), e);
        }
    }
    
    /**
     * 汇总指定层级的地域统计数据（从子级汇总）
     * 
     * @param parentLevel 父级地域层级
     * @param childLevel 子级地域层级
     */
    private void aggregateRegionsByLevel(Integer parentLevel, Integer childLevel) {
        try {
            QueryWrapper<Region> parentQuery = new QueryWrapper<>();
            parentQuery.eq("level", parentLevel);
            parentQuery.eq("deleted", 0);
            List<Region> parentRegions = regionService.list(parentQuery);
            
            log.info("Found {} level {} regions to aggregate from level {} regions", 
                    parentRegions.size(), parentLevel, childLevel);
            
            int successCount = 0;
            int failCount = 0;
            
            for (Region parentRegion : parentRegions) {
                try {
                    Map<String, Object> aggregatedStats = dashboardStatisticsService.aggregateChildRegionStats(
                            parentRegion.getId(), childLevel);
                    
                    // 设置地域基本信息
                    aggregatedStats.put("regionName", parentRegion.getName());
                    aggregatedStats.put("level", parentLevel);
                    
                    dashboardCacheService.updateRegionStats(parentRegion.getId(), aggregatedStats);
                    successCount++;
                    log.debug("Region statistics aggregated - region ID: {}, name: {}, level: {}", 
                            parentRegion.getId(), parentRegion.getName(), parentLevel);
                } catch (Exception e) {
                    failCount++;
                    log.warn("Failed to aggregate region statistics - region ID: {}, name: {}, level: {}, error: {}", 
                            parentRegion.getId(), parentRegion.getName(), parentLevel, e.getMessage());
                }
            }
            
            log.info("Level {} region statistics aggregation completed - success: {}, failed: {}", 
                    parentLevel, successCount, failCount);
            
        } catch (Exception e) {
            log.error("Failed to aggregate level {} region statistics - error: {}", 
                    parentLevel, e.getMessage(), e);
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
     * 从最小地域（level=4，城市）开始统计，然后向上汇总到level=3（省份）、level=2（国家）、level=1（片区）
     */
    private void updateAllRegionStatistics() {
        try {
            // 第一步：统计最小地域（level=4，城市）
            log.info("Step 1: Updating statistics for level 4 regions (cities)...");
            updateRegionsByLevel(4);
            
            // 第二步：汇总level=3（省份）的统计数据
            log.info("Step 2: Aggregating statistics for level 3 regions (provinces)...");
            aggregateRegionsByLevel(3, 4);
            
            // 第三步：汇总level=2（国家）的统计数据
            log.info("Step 3: Aggregating statistics for level 2 regions (countries)...");
            aggregateRegionsByLevel(2, 4);
            
            // 第四步：汇总level=1（片区）的统计数据
            log.info("Step 4: Aggregating statistics for level 1 regions (regions)...");
            aggregateRegionsByLevel(1, 2);
            
            log.info("All region statistics update completed");
            
        } catch (Exception e) {
            log.error("Failed to update all region statistics - error: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 更新指定层级的地域统计数据（直接计算，不汇总）
     * 
     * @param level 地域层级
     */
    private void updateRegionsByLevel(Integer level) {
        try {
            QueryWrapper<Region> regionQuery = new QueryWrapper<>();
            regionQuery.eq("level", level);
            regionQuery.eq("deleted", 0);
            List<Region> regions = regionService.list(regionQuery);
            
            log.info("Found {} level {} regions to update statistics", regions.size(), level);
            
            int successCount = 0;
            int failCount = 0;
            
            for (Region region : regions) {
                try {
                    Map<String, Object> stats = dashboardStatisticsService.calculateRegionStats(region.getId());
                    dashboardCacheService.updateRegionStats(region.getId(), stats);
                    successCount++;
                    log.debug("Region statistics updated - region ID: {}, name: {}, level: {}", 
                            region.getId(), region.getName(), level);
                } catch (Exception e) {
                    failCount++;
                    log.warn("Failed to update region statistics - region ID: {}, name: {}, level: {}, error: {}", 
                            region.getId(), region.getName(), level, e.getMessage());
                }
            }
            
            log.info("Level {} region statistics update completed - success: {}, failed: {}", 
                    level, successCount, failCount);
            
        } catch (Exception e) {
            log.error("Failed to update level {} region statistics - error: {}", level, e.getMessage(), e);
        }
    }
    
    /**
     * 汇总指定层级的地域统计数据（从子级汇总）
     * 
     * @param parentLevel 父级地域层级
     * @param childLevel 子级地域层级
     */
    private void aggregateRegionsByLevel(Integer parentLevel, Integer childLevel) {
        try {
            QueryWrapper<Region> parentQuery = new QueryWrapper<>();
            parentQuery.eq("level", parentLevel);
            parentQuery.eq("deleted", 0);
            List<Region> parentRegions = regionService.list(parentQuery);
            
            log.info("Found {} level {} regions to aggregate from level {} regions", 
                    parentRegions.size(), parentLevel, childLevel);
            
            int successCount = 0;
            int failCount = 0;
            
            for (Region parentRegion : parentRegions) {
                try {
                    Map<String, Object> aggregatedStats = dashboardStatisticsService.aggregateChildRegionStats(
                            parentRegion.getId(), childLevel);
                    
                    // 设置地域基本信息
                    aggregatedStats.put("regionName", parentRegion.getName());
                    aggregatedStats.put("level", parentLevel);
                    
                    dashboardCacheService.updateRegionStats(parentRegion.getId(), aggregatedStats);
                    successCount++;
                    log.debug("Region statistics aggregated - region ID: {}, name: {}, level: {}", 
                            parentRegion.getId(), parentRegion.getName(), parentLevel);
                } catch (Exception e) {
                    failCount++;
                    log.warn("Failed to aggregate region statistics - region ID: {}, name: {}, level: {}, error: {}", 
                            parentRegion.getId(), parentRegion.getName(), parentLevel, e.getMessage());
                }
            }
            
            log.info("Level {} region statistics aggregation completed - success: {}, failed: {}", 
                    parentLevel, successCount, failCount);
            
        } catch (Exception e) {
            log.error("Failed to aggregate level {} region statistics - error: {}", 
                    parentLevel, e.getMessage(), e);
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






