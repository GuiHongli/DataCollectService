package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.NetworkData;
import com.datacollect.service.NetworkDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 网络侧数据管理控制器
 *
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/network-data")
public class NetworkDataController {

    @Autowired
    private NetworkDataService networkDataService;

    /**
     * 分页查询网络侧数据
     * 支持按 gpsi、time_stamp、start_time、sub_app_id 查询
     *
     * @param current 当前页
     * @param size 每页大小
     * @param gpsi GPSI（可选，用于搜索）
     * @param timeStamp 时间戳（可选，用于搜索）
     * @param startTime 开始时间（可选，用于搜索）
     * @param subAppId 子应用ID（可选，用于搜索）
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<Page<NetworkData>> getNetworkDataPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String gpsi,
            @RequestParam(required = false) String timeStamp,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String subAppId) {
        try {
            Page<NetworkData> page = new Page<>(current, size);
            QueryWrapper<NetworkData> queryWrapper = new QueryWrapper<>();
            
            // 搜索条件
            if (gpsi != null && !gpsi.trim().isEmpty()) {
                queryWrapper.eq("gpsi", gpsi);
            }
            if (timeStamp != null && !timeStamp.trim().isEmpty()) {
                queryWrapper.eq("time_stamp", timeStamp);
            }
            if (startTime != null && !startTime.trim().isEmpty()) {
                queryWrapper.eq("start_time", startTime);
            }
            if (subAppId != null && !subAppId.trim().isEmpty()) {
                queryWrapper.eq("sub_app_id", subAppId);
            }
            
            // 倒序排列（按创建时间倒序）
            queryWrapper.orderByDesc("create_time");
            
            Page<NetworkData> result = networkDataService.page(page, queryWrapper);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to get network data page: {}", e.getMessage(), e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}

