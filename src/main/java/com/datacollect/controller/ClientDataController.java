package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.*;
import com.datacollect.service.*;
import com.datacollect.dto.SpeedComparisonDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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

    @Autowired
    private NetworkDataService networkDataService;

    @Autowired
    private TestSettingsDeviceImsiMappingService deviceImsiMappingService;

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

    /**
     * 更新vMOS数据
     *
     * @param id vMOS数据ID
     * @param vmosData vMOS数据对象
     * @return 更新结果
     */
    @PutMapping("/vmos/{id}")
    public Result<VmosData> updateVmosData(@PathVariable Long id, @RequestBody VmosData vmosData) {
        try {
            vmosData.setId(id);
            boolean success = vmosDataService.updateById(vmosData);
            if (success) {
                log.info("VmosData updated successfully - ID: {}", id);
                return Result.success(vmosData);
            } else {
                log.error("Failed to update VmosData - ID: {}", id);
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            log.error("Failed to update VmosData - ID: {}, error: {}", id, e.getMessage(), e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 获取速率对比数据
     * 
     * @param taskId 任务ID
     * @return 速率对比数据
     */
    @GetMapping("/speed-comparison/{taskId}")
    public Result<SpeedComparisonDTO> getSpeedComparison(@PathVariable String taskId) {
        try {
            SpeedComparisonDTO result = new SpeedComparisonDTO();
            
            // 1. 通过taskId在client_task_info表获取device_id、service、app、start_time、end_time
            QueryWrapper<ClientTaskInfo> taskInfoWrapper = new QueryWrapper<>();
            taskInfoWrapper.eq("task_id", taskId);
            ClientTaskInfo taskInfo = taskInfoService.getOne(taskInfoWrapper);
            
            if (taskInfo == null) {
                return Result.error("任务信息不存在");
            }
            
            String deviceId = taskInfo.getDeviceId();
            String service = taskInfo.getService();
            String app = taskInfo.getApp();
            String startTime = taskInfo.getStartTime();
            String endTime = taskInfo.getEndTime();
            
            if (deviceId == null || deviceId.trim().isEmpty()) {
                return Result.error("设备ID为空");
            }
            
            // 2. 通过device_id去test_settings_device_imsi_mapping表获取gpsi
            QueryWrapper<TestSettingsDeviceImsiMapping> mappingWrapper = new QueryWrapper<>();
            mappingWrapper.eq("device_id", deviceId);
            TestSettingsDeviceImsiMapping mapping = deviceImsiMappingService.getOne(mappingWrapper);
            
            if (mapping == null) {
                return Result.error("未找到设备ID对应的GPSI映射");
            }
            
            String gpsi = mapping.getGpsi();
            
            // 3. 通过taskId在vmos表获取speed字段（端侧速率数据）
            QueryWrapper<VmosData> vmosWrapper = new QueryWrapper<>();
            vmosWrapper.eq("task_id", taskId);
            vmosWrapper.orderByAsc("id");
            List<VmosData> vmosDataList = vmosDataService.list(vmosWrapper);
            
            List<SpeedComparisonDTO.ClientSpeedData> clientSpeedList = new ArrayList<>();
            if (vmosDataList != null && !vmosDataList.isEmpty()) {
                for (VmosData vmosData : vmosDataList) {
                    SpeedComparisonDTO.ClientSpeedData clientSpeed = new SpeedComparisonDTO.ClientSpeedData();
                    clientSpeed.setSequenceNumber(vmosData.getSequenceNumber());
                    
                    // 解析speed字段，转换为BigDecimal（单位：Kbps）
                    String speedStr = vmosData.getSpeed();
                    if (speedStr != null && !speedStr.trim().isEmpty()) {
                        try {
                            BigDecimal speed = new BigDecimal(speedStr);
                            clientSpeed.setSpeed(speed);
                        } catch (NumberFormatException e) {
                            log.warn("无法解析speed字段: {}", speedStr);
                            clientSpeed.setSpeed(BigDecimal.ZERO);
                        }
                    } else {
                        clientSpeed.setSpeed(BigDecimal.ZERO);
                    }
                    
                    // 使用序号作为时间戳（可以根据实际需求调整）
                    clientSpeed.setTimeStamp(vmosData.getSequenceNumber());
                    
                    clientSpeedList.add(clientSpeed);
                }
            }
            result.setClientSpeedList(clientSpeedList);
            
            // 4. 通过gpsi、app_service（service+app的组合）、start_time、end_time去network_data表筛选数据
            // 注意：sub_app_id应该对应app_service（service+app的组合）
            String appService = (service != null ? service : "") + (app != null ? app : "");
            
            QueryWrapper<NetworkData> networkWrapper = new QueryWrapper<>();
            networkWrapper.eq("gpsi", gpsi);
            if (appService != null && !appService.trim().isEmpty()) {
                networkWrapper.eq("sub_app_id", appService);
            }
            if (startTime != null && !startTime.trim().isEmpty()) {
                networkWrapper.ge("start_time", startTime);
            }
            if (endTime != null && !endTime.trim().isEmpty()) {
                networkWrapper.le("time_stamp", endTime);
            }
            networkWrapper.orderByAsc("time_stamp");
            List<NetworkData> networkDataList = networkDataService.list(networkWrapper);
            
            List<SpeedComparisonDTO.NetworkSpeedData> networkSpeedList = new ArrayList<>();
            if (networkDataList != null && !networkDataList.isEmpty()) {
                for (NetworkData networkData : networkDataList) {
                    SpeedComparisonDTO.NetworkSpeedData networkSpeed = new SpeedComparisonDTO.NetworkSpeedData();
                    networkSpeed.setTimeStamp(networkData.getTimeStamp());
                    
                    // 解析uplink_bandwidth和downlink_bandwidth，除以1024转换为Kbps
                    String uplinkStr = networkData.getUplinkBandwidth();
                    String downlinkStr = networkData.getDownlinkBandwidth();
                    
                    if (uplinkStr != null && !uplinkStr.trim().isEmpty()) {
                        try {
                            BigDecimal uplink = new BigDecimal(uplinkStr);
                            // 除以1024转换为Kbps
                            networkSpeed.setUplinkBandwidth(uplink.divide(new BigDecimal("1024"), 2, RoundingMode.HALF_UP));
                        } catch (NumberFormatException e) {
                            log.warn("无法解析uplink_bandwidth字段: {}", uplinkStr);
                            networkSpeed.setUplinkBandwidth(BigDecimal.ZERO);
                        }
                    } else {
                        networkSpeed.setUplinkBandwidth(BigDecimal.ZERO);
                    }
                    
                    if (downlinkStr != null && !downlinkStr.trim().isEmpty()) {
                        try {
                            BigDecimal downlink = new BigDecimal(downlinkStr);
                            // 除以1024转换为Kbps
                            networkSpeed.setDownlinkBandwidth(downlink.divide(new BigDecimal("1024"), 2, RoundingMode.HALF_UP));
                        } catch (NumberFormatException e) {
                            log.warn("无法解析downlink_bandwidth字段: {}", downlinkStr);
                            networkSpeed.setDownlinkBandwidth(BigDecimal.ZERO);
                        }
                    } else {
                        networkSpeed.setDownlinkBandwidth(BigDecimal.ZERO);
                    }
                    
                    networkSpeedList.add(networkSpeed);
                }
            }
            result.setNetworkSpeedList(networkSpeedList);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to get speed comparison: {}", e.getMessage(), e);
            return Result.error("获取速率对比数据失败: " + e.getMessage());
        }
    }
}

