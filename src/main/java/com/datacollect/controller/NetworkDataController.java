package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.dto.NetworkDataGroupDTO;
import com.datacollect.entity.NetworkData;
import com.datacollect.service.NetworkDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 网络侧数据管理控制器
 *
 * @author system
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/network-data")
public class NetworkDataController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkDataController.class);

    @Autowired
    private NetworkDataService networkDataService;

    /**
     * 分页查询网络侧数据
     * 支持按 gpsi、start_time 范围、sub_app_id 查询
     *
     * @param current 当前页
     * @param size 每页大小
     * @param gpsi GPSI（可选，用于搜索）
     * @param startTimeBegin 开始时间范围-开始（可选，用于搜索，格式：YYYY-MM-DD HH:mm:ss）
     * @param startTimeEnd 开始时间范围-结束（可选，用于搜索，格式：YYYY-MM-DD HH:mm:ss）
     * @param subAppId 子应用ID（可选，用于搜索）
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<Page<NetworkData>> getNetworkDataPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String gpsi,
            @RequestParam(required = false) String startTimeBegin,
            @RequestParam(required = false) String startTimeEnd,
            @RequestParam(required = false) String subAppId) {
        try {
            Page<NetworkData> page = new Page<>(current, size);
            QueryWrapper<NetworkData> queryWrapper = new QueryWrapper<>();
            
            // 搜索条件
            if (gpsi != null && !gpsi.trim().isEmpty()) {
                queryWrapper.eq("gpsi", gpsi);
            }
            // 开始时间范围查询：start_time >= startTimeBegin AND start_time <= startTimeEnd
            if (startTimeBegin != null && !startTimeBegin.trim().isEmpty()) {
                queryWrapper.ge("start_time", startTimeBegin);
            }
            if (startTimeEnd != null && !startTimeEnd.trim().isEmpty()) {
                queryWrapper.le("start_time", startTimeEnd);
            }
            if (subAppId != null && !subAppId.trim().isEmpty()) {
                queryWrapper.eq("sub_app_id", subAppId);
            }
            
            // 倒序排列（按创建时间倒序）
            queryWrapper.orderByDesc("create_time");
            
            Page<NetworkData> result = networkDataService.page(page, queryWrapper);
            return Result.success(result);
        } catch (Exception e) {
            LOGGER.error("Failed to get network data page: {}", e.getMessage(), e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询网络侧数据聚合（按GPSI+日期+子应用ID分组）
     *
     * @param current 当前页
     * @param size 每页大小
     * @param gpsi GPSI筛选条件（可选）
     * @param date 日期筛选条件（可选，格式：YYYY-MM-DD）
     * @param subAppId 子应用ID筛选条件（可选）
     * @return 分页结果
     */
    @GetMapping("/group/page")
    public Result<Page<NetworkDataGroupDTO>> getGroupedNetworkDataPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String gpsi,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String subAppId) {
        try {
            Page<NetworkDataGroupDTO> result = networkDataService.getGroupedNetworkDataPage(current, size, gpsi, date, subAppId);
            return Result.success(result);
        } catch (Exception e) {
            LOGGER.error("Failed to get grouped network data page: {}", e.getMessage(), e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 删除指定日期下的网络侧数据（逻辑删除）
     *
     * @param gpsi GPSI
     * @param date 日期（格式：YYYY-MM-DD）
     * @param subAppId 子应用ID
     * @return 删除结果
     */
    @DeleteMapping("/by-date")
    public Result<Void> deleteNetworkDataByDate(
            @RequestParam String gpsi,
            @RequestParam String date,
            @RequestParam String subAppId) {
        try {
            boolean success = networkDataService.deleteByDate(gpsi, date, subAppId);
            if (success) {
                LOGGER.info("Network data deleted successfully by date - gpsi: {}, date: {}, subAppId: {}", gpsi, date, subAppId);
                return Result.success(null);
            } else {
                LOGGER.warn("No network data found to delete - gpsi: {}, date: {}, subAppId: {}", gpsi, date, subAppId);
                return Result.error("未找到要删除的数据");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to delete network data by date - gpsi: {}, date: {}, subAppId: {}, error: {}", 
                    gpsi, date, subAppId, e.getMessage(), e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除网络侧数据（逻辑删除）
     *
     * @param ids 数据ID列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteNetworkData(@RequestBody List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return Result.error("请选择要删除的数据");
            }
            
            boolean success = networkDataService.removeByIds(ids);
            if (success) {
                LOGGER.info("Network data batch deleted successfully - count: {}", ids.size());
                return Result.success(null);
            } else {
                LOGGER.warn("Failed to batch delete network data - count: {}", ids.size());
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to batch delete network data - error: {}", e.getMessage(), e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}

