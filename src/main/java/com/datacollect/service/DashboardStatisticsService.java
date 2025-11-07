package com.datacollect.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datacollect.entity.CollectTask;
import com.datacollect.entity.Executor;
import com.datacollect.entity.Region;
import com.datacollect.entity.TestCase;
import com.datacollect.entity.TestCaseExecutionInstance;
import com.datacollect.entity.Ue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 仪表盘统计服务
 * 负责实际的数据统计计算
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class DashboardStatisticsService {
    
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
     * 计算仪表盘总体统计数据
     * 
     * @return 统计数据
     */
    public Map<String, Object> calculateStats() {
        log.info("Start calculating dashboard statistics");
        
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
        
        log.info("Dashboard statistics calculated successfully");
        return stats;
    }
    
    /**
     * 计算地域统计数据
     * 
     * @param regionId 地域ID
     * @return 统计数据
     */
    public Map<String, Object> calculateRegionStats(Long regionId) {
        log.info("Start calculating region statistics - region ID: {}", regionId);
        
        Map<String, Object> stats = new HashMap<>();
        
        // 获取地域信息
        Region region = regionService.getById(regionId);
        if (region == null) {
            log.warn("Region not found: {}", regionId);
            return stats;
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
            log.warn("Unsupported region level: {}", level);
            return stats;
        }
        
        // 1. 获取该地域下的所有执行机
        QueryWrapper<Executor> executorQuery = new QueryWrapper<>();
        executorQuery.eq("region_id", regionId);
        executorQuery.eq("deleted", 0);
        List<Executor> executors = executorService.list(executorQuery);
        long executorCount = executors.size();
        stats.put("executorCount", executorCount);
        log.info("Executor count for region {}: {}", regionId, executorCount);
        
        // 2. 获取这些执行机的IP地址列表
        List<String> executorIps = executors.stream()
                .map(Executor::getIpAddress)
                .collect(Collectors.toList());
        
        // 3. 通过执行机IP查询所有相关的执行例次，获取采集任务信息，再获取用例信息
        Map<String, AppStatInfo> appStatsMap = new LinkedHashMap<>();
        int totalCollectCount = 0;
        
        if (!executorIps.isEmpty()) {
            // 第一步：根据执行机IP查询所有执行例次
            QueryWrapper<TestCaseExecutionInstance> instanceQuery = new QueryWrapper<>();
            instanceQuery.in("executor_ip", executorIps);
            List<TestCaseExecutionInstance> instances = testCaseExecutionInstanceService.list(instanceQuery);
            
            log.debug("Found {} execution instances for executors in region {} (executor IPs: {})", 
                    instances.size(), regionId, executorIps);
            
            if (!instances.isEmpty()) {
                // 第二步：从执行例次中获取采集任务ID（去重）
                Set<Long> collectTaskIds = instances.stream()
                        .map(TestCaseExecutionInstance::getCollectTaskId)
                        .filter(taskId -> taskId != null)
                        .collect(Collectors.toSet());
                
                log.debug("Found {} unique collect tasks from execution instances", collectTaskIds.size());
                
                // 第三步：遍历每个采集任务，获取用例信息
                for (Long taskId : collectTaskIds) {
                    CollectTask task = collectTaskService.getById(taskId);
                    if (task != null && task.getTestCaseSetId() != null) {
                        log.debug("Processing task {} with test case set ID {}", taskId, task.getTestCaseSetId());
                        
                        // 第四步：通过采集任务的用例集ID获取所有用例
                        List<TestCase> testCases = testCaseService.getByTestCaseSetId(task.getTestCaseSetId());
                        log.debug("Task {} has {} test cases in test case set", taskId, testCases.size());
                        
                        // 第五步：统计该任务下每个用例的执行例次数（只统计属于该地域执行机的）
                        Map<Long, Integer> testCaseInstanceCountMap = new HashMap<>();
                        for (TestCaseExecutionInstance instance : instances) {
                            if (taskId.equals(instance.getCollectTaskId())) {
                                Long testCaseId = instance.getTestCaseId();
                                testCaseInstanceCountMap.put(testCaseId, 
                                        testCaseInstanceCountMap.getOrDefault(testCaseId, 0) + 1);
                            }
                        }
                        
                        // 第六步：遍历用例，统计APP信息和采集次数
                        for (TestCase testCase : testCases) {
                            String appName = testCase.getApp();
                            if (appName != null && !appName.trim().isEmpty()) {
                                appName = appName.trim();
                                // 获取该用例在该任务下的执行例次数（该地域执行机执行的）
                                int instanceCount = testCaseInstanceCountMap.getOrDefault(testCase.getId(), 0);
                                
                                if (instanceCount > 0) {
                                    AppStatInfo appStat = appStatsMap.getOrDefault(appName, new AppStatInfo(appName));
                                    // 累加该用例的执行例次数
                                    appStat.addCollectCount(instanceCount);
                                    appStatsMap.put(appName, appStat);
                                    totalCollectCount += instanceCount;
                                    
                                    log.debug("APP {} in task {} has {} execution instances", 
                                            appName, taskId, instanceCount);
                                }
                            }
                        }
                    } else {
                        if (task == null) {
                            log.debug("Collect task not found - task ID: {}", taskId);
                        } else {
                            log.debug("Collect task {} has no test case set ID", taskId);
                        }
                    }
                }
            }
            
            log.debug("Statistics: {} unique apps found with total {} execution instances", 
                    appStatsMap.size(), totalCollectCount);
        } else {
            log.debug("No executors found for region {}", regionId);
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
        
        log.info("Region statistics calculated - region ID: {}, app count: {}, collect count: {}, executor count: {}", 
                regionId, appStatsMap.size(), totalCollectCount, executorCount);
        
        return stats;
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




