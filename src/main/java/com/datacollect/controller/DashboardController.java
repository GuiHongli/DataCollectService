package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datacollect.common.Result;
import com.datacollect.entity.CollectTask;
import com.datacollect.entity.Executor;
import com.datacollect.entity.Region;
import com.datacollect.entity.TestCase;
import com.datacollect.entity.Ue;
import com.datacollect.entity.TestCaseExecutionInstance;
import com.datacollect.service.CollectTaskService;
import com.datacollect.service.ExecutorService;
import com.datacollect.service.RegionService;
import com.datacollect.service.TestCaseExecutionInstanceService;
import com.datacollect.service.TestCaseService;
import com.datacollect.service.UeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private RegionService regionService;

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private UeService ueService;

    @Autowired
    private CollectTaskService collectTaskService;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private TestCaseExecutionInstanceService testCaseExecutionInstanceService;

    /**
     * 获取仪表盘统计数据
     * 
     * @return 统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        log.info("Start getting dashboard statistics");
        
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 统计地域数量（排除已删除的记录）
            QueryWrapper<Region> regionQuery = new QueryWrapper<>();
            regionQuery.eq("deleted", 0);
            long regionCount = regionService.count(regionQuery);
            stats.put("regionCount", regionCount);
            log.info("Region count: {}", regionCount);
            
            // 统计执行机数量（排除已删除的记录）
            QueryWrapper<Executor> executorQuery = new QueryWrapper<>();
            executorQuery.eq("deleted", 0);
            long executorCount = executorService.count(executorQuery);
            stats.put("executorCount", executorCount);
            log.info("Executor count: {}", executorCount);
            
            // 统计UE数量（排除已删除的记录）
            QueryWrapper<Ue> ueQuery = new QueryWrapper<>();
            ueQuery.eq("deleted", 0);
            long ueCount = ueService.count(ueQuery);
            stats.put("ueCount", ueCount);
            log.info("UE count: {}", ueCount);
            
            // 统计任务数量（CollectTask表没有deleted字段，直接统计所有记录）
            long taskCount = collectTaskService.count();
            stats.put("taskCount", taskCount);
            log.info("Task count: {}", taskCount);
            
            log.info("Dashboard statistics retrieved successfully");
            return Result.success(stats);
            
        } catch (Exception e) {
            log.error("Failed to get dashboard statistics - error: {}", e.getMessage(), e);
            return Result.error("Failed to get dashboard statistics: " + e.getMessage());
        }
    }

    /**
     * 获取地域统计数据（app数量、采集次数、执行机数量）
     * 
     * @param regionId 地域ID（城市ID或国家ID）
     * @return 统计数据
     */
    @GetMapping("/region-stats")
    public Result<Map<String, Object>> getRegionStats(@RequestParam @NotNull Long regionId) {
        log.info("Start getting region statistics - region ID: {}", regionId);
        
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 获取地域信息
            Region region = regionService.getById(regionId);
            if (region == null) {
                return Result.error("Region not found: " + regionId);
            }
            
            Integer level = region.getLevel();
            stats.put("regionName", region.getName());
            stats.put("level", level);
            
            // 根据层级确定查询条件
            QueryWrapper<CollectTask> taskQuery = new QueryWrapper<>();
            if (level == 4) {
                // 城市级别
                taskQuery.eq("city_id", regionId);
            } else if (level == 2) {
                // 国家级别 - 查询该国家下的所有城市对应的任务
                QueryWrapper<Region> cityQuery = new QueryWrapper<>();
                cityQuery.eq("parent_id", regionId);
                cityQuery.eq("level", 4);
                cityQuery.eq("deleted", 0);
                List<Region> cities = regionService.list(cityQuery);
                if (!cities.isEmpty()) {
                    List<Long> cityIds = cities.stream().map(Region::getId).collect(Collectors.toList());
                    taskQuery.in("city_id", cityIds);
                } else {
                    taskQuery.eq("city_id", -1); // 空结果
                }
            } else {
                return Result.error("Unsupported region level: " + level + ". Only city (level=4) and country (level=2) are supported.");
            }
            
            // 1. 获取该地域下的所有执行机
            QueryWrapper<Executor> executorQuery = new QueryWrapper<>();
            executorQuery.eq("region_id", regionId);
            executorQuery.eq("deleted", 0);
            List<Executor> executors = executorService.list(executorQuery);
            long executorCount = executors.size();
            stats.put("executorCount", executorCount);
            log.info("Executor count for region {}: {}", regionId, executorCount);
            
            // 2. 获取这些执行机的所有执行例次
            List<String> executorIps = executors.stream()
                    .map(Executor::getIpAddress)
                    .collect(Collectors.toList());
            
            // 3. 统计APP详细信息及采集次数（基于执行例次）
            Map<String, AppStatInfo> appStatsMap = new LinkedHashMap<>();
            int totalCollectCount = 0;
            
            // 获取该地域下所有任务的执行例次
            List<CollectTask> tasks = collectTaskService.list(taskQuery);
            log.info("Found {} tasks for region {}", tasks.size(), regionId);
            
            if (!executorIps.isEmpty() && !tasks.isEmpty()) {
                List<Long> taskIds = tasks.stream()
                        .map(CollectTask::getId)
                        .collect(Collectors.toList());
                
                // 获取这些任务的所有执行例次，并筛选出属于该地域执行机的
                QueryWrapper<TestCaseExecutionInstance> instanceQuery = new QueryWrapper<>();
                instanceQuery.in("collect_task_id", taskIds);
                instanceQuery.in("executor_ip", executorIps);
                List<TestCaseExecutionInstance> instances = testCaseExecutionInstanceService.list(instanceQuery);
                
                log.info("Found {} execution instances for region {} executors (executor IPs: {})", 
                        instances.size(), regionId, executorIps);
                
                // 统计每个APP的采集次数
                int instanceWithAppCount = 0;
                for (TestCaseExecutionInstance instance : instances) {
                    // 获取用例信息
                    TestCase testCase = testCaseService.getById(instance.getTestCaseId());
                    if (testCase != null) {
                        String appName = testCase.getApp();
                        if (appName != null && !appName.trim().isEmpty()) {
                            appName = appName.trim();
                            AppStatInfo appStat = appStatsMap.getOrDefault(appName, new AppStatInfo(appName));
                            // 每个执行例次算一次采集
                            appStat.addCollectCount(1);
                            appStatsMap.put(appName, appStat);
                            totalCollectCount++;
                            instanceWithAppCount++;
                        } else {
                            log.debug("TestCase {} has no app name", instance.getTestCaseId());
                        }
                    } else {
                        log.warn("TestCase not found for instance {}, testCaseId: {}", instance.getId(), instance.getTestCaseId());
                    }
                }
                
                log.info("Statistics: {} instances with app name out of {} total instances", 
                        instanceWithAppCount, instances.size());
            } else {
                if (executorIps.isEmpty()) {
                    log.info("No executors found for region {}", regionId);
                }
                if (tasks.isEmpty()) {
                    log.info("No tasks found for region {}", regionId);
                }
            }
            
            // 转换为列表，按采集次数降序排序
            List<Map<String, Object>> appList = new ArrayList<>();
            for (AppStatInfo appStat : appStatsMap.values()) {
                Map<String, Object> appInfo = new HashMap<>();
                appInfo.put("appName", appStat.getAppName());
                appInfo.put("collectCount", appStat.getCollectCount());
                appList.add(appInfo);
            }
            
            // 按采集次数降序排序
            appList.sort((a, b) -> {
                Integer countA = (Integer) a.get("collectCount");
                Integer countB = (Integer) b.get("collectCount");
                return countB.compareTo(countA);
            });
            
            stats.put("appCount", appStatsMap.size());
            stats.put("collectCount", totalCollectCount);
            stats.put("appList", appList);
            
            log.info("Region statistics - region ID: {}, app count: {}, collect count: {}, executor count: {}", 
                    regionId, appStatsMap.size(), totalCollectCount, executorCount);
            
            return Result.success(stats);
            
        } catch (Exception e) {
            log.error("Failed to get region statistics - region ID: {}, error: {}", regionId, e.getMessage(), e);
            return Result.error("Failed to get region statistics: " + e.getMessage());
        }
    }
    
    /**
     * APP统计信息内部类
     */
    private static class AppStatInfo {
        private String appName;
        private int collectCount;
        
        public AppStatInfo(String appName) {
            this.appName = appName;
            this.collectCount = 0;
        }
        
        public void addCollectCount(int count) {
            this.collectCount += count;
        }
        
        public String getAppName() {
            return appName;
        }
        
        public int getCollectCount() {
            return collectCount;
        }
    }
}

