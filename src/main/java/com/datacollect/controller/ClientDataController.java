package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.*;
import com.datacollect.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 端侧数据管理控制器
 *
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/client-data")
public class ClientDataController {

    @Autowired
    private TaskInfoService taskInfoService;

    @Autowired
    private SpeedDataService speedDataService;

    @Autowired
    private VmosDataService vmosDataService;

    @Autowired
    private RttDataService rttDataService;

    @Autowired
    private LostDataService lostDataService;

    @Autowired
    private VideoDataService videoDataService;

    /**
     * 分页查询端侧任务信息列表（倒序）
     *
     * @param current 当前页
     * @param size 每页大小
     * @param taskId 任务ID（可选，用于搜索）
     * @param service 业务大类（可选，用于搜索）
     * @param app 应用名称（可选，用于搜索）
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<Page<ClientTaskInfo>> getTaskInfoPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String app) {
        try {
            Page<ClientTaskInfo> page = new Page<>(current, size);
            QueryWrapper<ClientTaskInfo> queryWrapper = new QueryWrapper<>();
            
            // 搜索条件
            if (taskId != null && !taskId.trim().isEmpty()) {
                queryWrapper.like("task_id", taskId);
            }
            if (service != null && !service.trim().isEmpty()) {
                queryWrapper.like("service", service);
            }
            if (app != null && !app.trim().isEmpty()) {
                queryWrapper.like("app", app);
            }
            
            // 倒序排列（按创建时间倒序）
            queryWrapper.orderByDesc("create_time");
            
            Page<ClientTaskInfo> result = taskInfoService.page(page, queryWrapper);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to get task info page: {}", e.getMessage(), e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据任务ID获取任务详情
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    @GetMapping("/detail/{taskId}")
    public Result<Map<String, Object>> getTaskDetail(@PathVariable String taskId) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 查询基础信息
            QueryWrapper<ClientTaskInfo> taskInfoWrapper = new QueryWrapper<>();
            taskInfoWrapper.eq("task_id", taskId);
            ClientTaskInfo taskInfo = taskInfoService.getOne(taskInfoWrapper);
            result.put("taskInfo", taskInfo);
            
            // 查询vmos数据
            QueryWrapper<VmosData> vmosWrapper = new QueryWrapper<>();
            vmosWrapper.eq("task_id", taskId);
            vmosWrapper.orderByAsc("id");
            List<VmosData> vmosDataList = vmosDataService.list(vmosWrapper);
            result.put("vmosDataList", vmosDataList);
            
            // 查询speed数据
            QueryWrapper<SpeedData> speedWrapper = new QueryWrapper<>();
            speedWrapper.eq("task_id", taskId);
            speedWrapper.orderByAsc("id");
            List<SpeedData> speedDataList = speedDataService.list(speedWrapper);
            result.put("speedDataList", speedDataList);
            
            // 查询rtt数据
            QueryWrapper<RttData> rttWrapper = new QueryWrapper<>();
            rttWrapper.eq("task_id", taskId);
            rttWrapper.orderByAsc("id");
            List<RttData> rttDataList = rttDataService.list(rttWrapper);
            result.put("rttDataList", rttDataList);
            
            // 查询lost数据
            QueryWrapper<LostData> lostWrapper = new QueryWrapper<>();
            lostWrapper.eq("task_id", taskId);
            lostWrapper.orderByAsc("id");
            List<LostData> lostDataList = lostDataService.list(lostWrapper);
            result.put("lostDataList", lostDataList);
            
            // 查询video数据
            QueryWrapper<VideoData> videoWrapper = new QueryWrapper<>();
            videoWrapper.eq("task_id", taskId);
            videoWrapper.orderByAsc("id");
            List<VideoData> videoDataList = videoDataService.list(videoWrapper);
            result.put("videoDataList", videoDataList);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to get task detail: {}", e.getMessage(), e);
            return Result.error("查询详情失败: " + e.getMessage());
        }
    }
}

