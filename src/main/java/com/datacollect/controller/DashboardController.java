package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datacollect.common.Result;
import com.datacollect.entity.Executor;
import com.datacollect.entity.Region;
import com.datacollect.entity.Ue;
import com.datacollect.service.CollectTaskService;
import com.datacollect.service.ExecutorService;
import com.datacollect.service.RegionService;
import com.datacollect.service.UeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
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
    private RegionService regionService;

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private UeService ueService;

    @Autowired
    private CollectTaskService collectTaskService;

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
}

