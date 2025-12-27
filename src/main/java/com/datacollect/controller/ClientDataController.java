package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.*;
import com.datacollect.service.*;
import com.datacollect.dto.SpeedComparisonDTO;
import com.datacollect.dto.RttComparisonDTO;
import com.datacollect.dto.StutterComparisonDTO;
import com.datacollect.dto.AvgQoeComparisonDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
            
            // 转换时间格式和时区：从 UTC+8 的 20251027150500 转换为 UTC+0 的 2025-10-27 07:05:00
            String convertedStartTime = convertTimeFormatAndTimezone(startTime);
            String convertedEndTime = convertTimeFormatAndTimezone(endTime);
            
            // 2. 通过device_id去test_settings_device_imsi_mapping表获取gpsi
            QueryWrapper<TestSettingsDeviceImsiMapping> mappingWrapper = new QueryWrapper<>();
            mappingWrapper.eq("device_id", deviceId);
            TestSettingsDeviceImsiMapping mapping = deviceImsiMappingService.getOne(mappingWrapper);
            
            if (mapping == null) {
                return Result.error("未找到设备ID对应的GPSI映射");
            }
            
            String gpsi = mapping.getGpsi();
            
            // 3. 通过taskId在vmos表获取speed字段（端侧速率数据），按sequence_number排序
            QueryWrapper<VmosData> vmosWrapper = new QueryWrapper<>();
            vmosWrapper.eq("task_id", taskId);
            vmosWrapper.orderByAsc("sequence_number");
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
            String appService = (app != null ? app : "") + "-" + (service != null ? service : "");
            
            QueryWrapper<NetworkData> networkWrapper = new QueryWrapper<>();
            networkWrapper.eq("gpsi", gpsi);
            if (appService != null && !appService.trim().isEmpty()) {
                networkWrapper.eq("sub_app_id", appService);
            }
            
            // 如果用户选择了网络侧开始时间，使用该时间；否则使用转换后的start_time
            String networkStartTime = taskInfo.getNetworkStartTime();
            if (networkStartTime != null && !networkStartTime.trim().isEmpty()) {
                // 使用用户选择的网络侧开始时间
                networkWrapper.ge("start_time", networkStartTime);
            } else {
                // 使用转换后的时间格式进行查询
                if (convertedStartTime != null && !convertedStartTime.trim().isEmpty()) {
                    networkWrapper.ge("start_time", convertedStartTime);
                }
            }
            
            if (convertedEndTime != null && !convertedEndTime.trim().isEmpty()) {
                networkWrapper.le("time_stamp", convertedEndTime);
            }
            // 按start_time排序
            networkWrapper.orderByAsc("start_time");
            List<NetworkData> networkDataList = networkDataService.list(networkWrapper);
            
            List<SpeedComparisonDTO.NetworkSpeedData> networkSpeedList = new ArrayList<>();
            if (networkDataList != null && !networkDataList.isEmpty()) {
                for (NetworkData networkData : networkDataList) {
                    SpeedComparisonDTO.NetworkSpeedData networkSpeed = new SpeedComparisonDTO.NetworkSpeedData();
                    networkSpeed.setTimeStamp(networkData.getTimeStamp());
                    networkSpeed.setStartTime(networkData.getStartTime());
                    
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
            
            // 设置当前保存的网络侧开始时间
            result.setNetworkStartTime(taskInfo.getNetworkStartTime());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to get speed comparison: {}", e.getMessage(), e);
            return Result.error("获取速率对比数据失败: " + e.getMessage());
        }
    }

    /**
     * 更新任务的网络侧开始时间
     *
     * @param taskId 任务ID
     * @param networkStartTime 网络侧开始时间
     * @return 更新结果
     */
    @PutMapping("/network-start-time/{taskId}")
    public Result<ClientTaskInfo> updateNetworkStartTime(
            @PathVariable String taskId,
            @RequestBody Map<String, String> request) {
        try {
            String networkStartTime = request.get("networkStartTime");
            
            QueryWrapper<ClientTaskInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("task_id", taskId);
            ClientTaskInfo taskInfo = taskInfoService.getOne(queryWrapper);
            
            if (taskInfo == null) {
                return Result.error("任务信息不存在");
            }
            
            taskInfo.setNetworkStartTime(networkStartTime);
            boolean success = taskInfoService.updateById(taskInfo);
            
            if (success) {
                log.info("Network start time updated successfully - taskId: {}, networkStartTime: {}", taskId, networkStartTime);
                return Result.success(taskInfo);
            } else {
                log.error("Failed to update network start time - taskId: {}", taskId);
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            log.error("Failed to update network start time - taskId: {}, error: {}", taskId, e.getMessage(), e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 转换时间格式和时区
     * 从 UTC+8 的 20251027150500 (yyyyMMddHHmmss) 转换为 UTC+0 的 2025-10-27 07:05:00 (yyyy-MM-dd HH:mm:ss)
     * 
     * @param timeStr 原始时间字符串（UTC+8时区）
     * @return 转换后的时间字符串（UTC+0时区），如果转换失败返回null
     */
    private String convertTimeFormatAndTimezone(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 如果已经是标准格式，需要先解析时区再转换
            if (timeStr.contains("-") && timeStr.contains(":")) {
                // 解析为UTC+8时区的ZonedDateTime
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime localDateTime = LocalDateTime.parse(timeStr, formatter);
                ZonedDateTime utc8Time = localDateTime.atZone(ZoneId.of("Asia/Shanghai"));
                // 转换为UTC+0
                ZonedDateTime utc0Time = utc8Time.withZoneSameInstant(ZoneId.of("UTC"));
                return utc0Time.format(formatter);
            }
            
            // 如果是14位数字格式 (yyyyMMddHHmmss)，假设是UTC+8时区
            if (timeStr.length() == 14 && timeStr.matches("\\d{14}")) {
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                
                // 解析为UTC+8时区的ZonedDateTime
                LocalDateTime localDateTime = LocalDateTime.parse(timeStr, inputFormatter);
                ZonedDateTime utc8Time = localDateTime.atZone(ZoneId.of("Asia/Shanghai"));
                
                // 转换为UTC+0时区
                ZonedDateTime utc0Time = utc8Time.withZoneSameInstant(ZoneId.of("UTC"));
                
                return utc0Time.format(outputFormatter);
            }
            
            // 如果无法识别格式，返回原值
            log.warn("无法识别的时间格式: {}", timeStr);
            return timeStr;
        } catch (DateTimeParseException e) {
            log.warn("时间格式转换失败: {}, 错误: {}", timeStr, e.getMessage());
            return timeStr;
        }
    }

    /**
     * 获取RTT对比数据
     * 
     * @param taskId 任务ID
     * @return RTT对比数据
     */
    @GetMapping("/rtt-comparison/{taskId}")
    public Result<RttComparisonDTO> getRttComparison(@PathVariable String taskId) {
        try {
            RttComparisonDTO result = new RttComparisonDTO();
            
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
            
            // 转换时间格式和时区
            String convertedStartTime = convertTimeFormatAndTimezone(startTime);
            String convertedEndTime = convertTimeFormatAndTimezone(endTime);
            
            // 2. 通过device_id去test_settings_device_imsi_mapping表获取gpsi
            QueryWrapper<TestSettingsDeviceImsiMapping> mappingWrapper = new QueryWrapper<>();
            mappingWrapper.eq("device_id", deviceId);
            TestSettingsDeviceImsiMapping mapping = deviceImsiMappingService.getOne(mappingWrapper);
            
            if (mapping == null) {
                return Result.error("未找到设备ID对应的GPSI映射");
            }
            
            String gpsi = mapping.getGpsi();
            
            // 3. 通过taskId在vmos表获取rtt字段（端侧RTT数据），按sequence_number排序
            QueryWrapper<VmosData> vmosWrapper = new QueryWrapper<>();
            vmosWrapper.eq("task_id", taskId);
            vmosWrapper.orderByAsc("sequence_number");
            List<VmosData> vmosDataList = vmosDataService.list(vmosWrapper);
            
            List<RttComparisonDTO.ClientRttData> clientRttList = new ArrayList<>();
            if (vmosDataList != null && !vmosDataList.isEmpty()) {
                for (VmosData vmosData : vmosDataList) {
                    RttComparisonDTO.ClientRttData clientRtt = new RttComparisonDTO.ClientRttData();
                    clientRtt.setSequenceNumber(vmosData.getSequenceNumber());
                    
                    // 解析rtt字段
                    String rttStr = vmosData.getRtt();
                    if (rttStr != null && !rttStr.trim().isEmpty()) {
                        try {
                            BigDecimal rtt = new BigDecimal(rttStr);
                            clientRtt.setRtt(rtt);
                        } catch (NumberFormatException e) {
                            log.warn("无法解析rtt字段: {}", rttStr);
                            clientRtt.setRtt(BigDecimal.ZERO);
                        }
                    } else {
                        clientRtt.setRtt(BigDecimal.ZERO);
                    }
                    
                    clientRtt.setTimeStamp(vmosData.getSequenceNumber());
                    clientRttList.add(clientRtt);
                }
            }
            result.setClientRttList(clientRttList);
            
            // 4. 查询network_data表获取service_delay字段
            String appService = (app != null ? app : "") + "-" + (service != null ? service : "");
            
            QueryWrapper<NetworkData> networkWrapper = new QueryWrapper<>();
            networkWrapper.eq("gpsi", gpsi);
            if (appService != null && !appService.trim().isEmpty()) {
                networkWrapper.eq("sub_app_id", appService);
            }
            
            // 如果用户选择了网络侧开始时间，使用该时间；否则使用转换后的start_time
            String networkStartTime = taskInfo.getNetworkStartTime();
            if (networkStartTime != null && !networkStartTime.trim().isEmpty()) {
                networkWrapper.ge("start_time", networkStartTime);
            } else {
                if (convertedStartTime != null && !convertedStartTime.trim().isEmpty()) {
                    networkWrapper.ge("start_time", convertedStartTime);
                }
            }
            
            if (convertedEndTime != null && !convertedEndTime.trim().isEmpty()) {
                networkWrapper.le("time_stamp", convertedEndTime);
            }
            networkWrapper.orderByAsc("start_time");
            List<NetworkData> networkDataList = networkDataService.list(networkWrapper);
            
            List<RttComparisonDTO.NetworkRttData> networkRttList = new ArrayList<>();
            if (networkDataList != null && !networkDataList.isEmpty()) {
                for (NetworkData networkData : networkDataList) {
                    RttComparisonDTO.NetworkRttData networkRtt = new RttComparisonDTO.NetworkRttData();
                    networkRtt.setTimeStamp(networkData.getTimeStamp());
                    networkRtt.setStartTime(networkData.getStartTime());
                    
                    // 解析service_delay字段
                    String serviceDelayStr = networkData.getServiceDelay();
                    if (serviceDelayStr != null && !serviceDelayStr.trim().isEmpty()) {
                        try {
                            BigDecimal serviceDelay = new BigDecimal(serviceDelayStr);
                            networkRtt.setServiceDelay(serviceDelay);
                        } catch (NumberFormatException e) {
                            log.warn("无法解析service_delay字段: {}", serviceDelayStr);
                            networkRtt.setServiceDelay(BigDecimal.ZERO);
                        }
                    } else {
                        networkRtt.setServiceDelay(BigDecimal.ZERO);
                    }
                    
                    networkRttList.add(networkRtt);
                }
            }
            result.setNetworkRttList(networkRttList);
            result.setNetworkStartTime(taskInfo.getNetworkStartTime());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to get RTT comparison: {}", e.getMessage(), e);
            return Result.error("获取RTT对比数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取卡顿对比数据
     * 
     * @param taskId 任务ID
     * @return 卡顿对比数据
     */
    @GetMapping("/stutter-comparison/{taskId}")
    public Result<StutterComparisonDTO> getStutterComparison(@PathVariable String taskId) {
        try {
            StutterComparisonDTO result = new StutterComparisonDTO();
            
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
            
            // 转换时间格式和时区
            String convertedStartTime = convertTimeFormatAndTimezone(startTime);
            String convertedEndTime = convertTimeFormatAndTimezone(endTime);
            
            // 2. 通过device_id去test_settings_device_imsi_mapping表获取gpsi
            QueryWrapper<TestSettingsDeviceImsiMapping> mappingWrapper = new QueryWrapper<>();
            mappingWrapper.eq("device_id", deviceId);
            TestSettingsDeviceImsiMapping mapping = deviceImsiMappingService.getOne(mappingWrapper);
            
            if (mapping == null) {
                return Result.error("未找到设备ID对应的GPSI映射");
            }
            
            String gpsi = mapping.getGpsi();
            
            // 3. 通过taskId在vmos表获取stutter_ratio字段（端侧卡顿数据），按sequence_number排序
            QueryWrapper<VmosData> vmosWrapper = new QueryWrapper<>();
            vmosWrapper.eq("task_id", taskId);
            vmosWrapper.orderByAsc("sequence_number");
            List<VmosData> vmosDataList = vmosDataService.list(vmosWrapper);
            
            List<StutterComparisonDTO.ClientStutterData> clientStutterList = new ArrayList<>();
            if (vmosDataList != null && !vmosDataList.isEmpty()) {
                for (VmosData vmosData : vmosDataList) {
                    StutterComparisonDTO.ClientStutterData clientStutter = new StutterComparisonDTO.ClientStutterData();
                    clientStutter.setSequenceNumber(vmosData.getSequenceNumber());
                    
                    // 解析stutter_ratio字段
                    String stutterRatioStr = vmosData.getStutterRatio();
                    if (stutterRatioStr != null && !stutterRatioStr.trim().isEmpty()) {
                        try {
                            BigDecimal stutterRatio = new BigDecimal(stutterRatioStr);
                            clientStutter.setStutterRatio(stutterRatio);
                        } catch (NumberFormatException e) {
                            log.warn("无法解析stutter_ratio字段: {}", stutterRatioStr);
                            clientStutter.setStutterRatio(BigDecimal.ZERO);
                        }
                    } else {
                        clientStutter.setStutterRatio(BigDecimal.ZERO);
                    }
                    
                    clientStutter.setTimeStamp(vmosData.getSequenceNumber());
                    clientStutterList.add(clientStutter);
                }
            }
            result.setClientStutterList(clientStutterList);
            
            // 4. 查询network_data表获取stalling_number字段，除以10
            String appService = (app != null ? app : "") + "-" + (service != null ? service : "");
            
            QueryWrapper<NetworkData> networkWrapper = new QueryWrapper<>();
            networkWrapper.eq("gpsi", gpsi);
            if (appService != null && !appService.trim().isEmpty()) {
                networkWrapper.eq("sub_app_id", appService);
            }
            
            // 如果用户选择了网络侧开始时间，使用该时间；否则使用转换后的start_time
            String networkStartTime = taskInfo.getNetworkStartTime();
            if (networkStartTime != null && !networkStartTime.trim().isEmpty()) {
                networkWrapper.ge("start_time", networkStartTime);
            } else {
                if (convertedStartTime != null && !convertedStartTime.trim().isEmpty()) {
                    networkWrapper.ge("start_time", convertedStartTime);
                }
            }
            
            if (convertedEndTime != null && !convertedEndTime.trim().isEmpty()) {
                networkWrapper.le("time_stamp", convertedEndTime);
            }
            networkWrapper.orderByAsc("start_time");
            List<NetworkData> networkDataList = networkDataService.list(networkWrapper);
            
            List<StutterComparisonDTO.NetworkStutterData> networkStutterList = new ArrayList<>();
            if (networkDataList != null && !networkDataList.isEmpty()) {
                for (NetworkData networkData : networkDataList) {
                    StutterComparisonDTO.NetworkStutterData networkStutter = new StutterComparisonDTO.NetworkStutterData();
                    networkStutter.setTimeStamp(networkData.getTimeStamp());
                    networkStutter.setStartTime(networkData.getStartTime());
                    
                    // 解析stalling_number字段，除以10
                    String stallingNumberStr = networkData.getStallingNumber();
                    if (stallingNumberStr != null && !stallingNumberStr.trim().isEmpty()) {
                        try {
                            BigDecimal stallingNumber = new BigDecimal(stallingNumberStr);
                            // 除以10
                            networkStutter.setStallingNumberDiv10(stallingNumber.divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP));
                        } catch (NumberFormatException e) {
                            log.warn("无法解析stalling_number字段: {}", stallingNumberStr);
                            networkStutter.setStallingNumberDiv10(BigDecimal.ZERO);
                        }
                    } else {
                        networkStutter.setStallingNumberDiv10(BigDecimal.ZERO);
                    }
                    
                    networkStutterList.add(networkStutter);
                }
            }
            result.setNetworkStutterList(networkStutterList);
            result.setNetworkStartTime(taskInfo.getNetworkStartTime());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to get stutter comparison: {}", e.getMessage(), e);
            return Result.error("获取卡顿对比数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取平均QOE对比数据
     * 
     * @param taskId 任务ID
     * @return 平均QOE对比数据
     */
    @GetMapping("/avg-qoe-comparison/{taskId}")
    public Result<AvgQoeComparisonDTO> getAvgQoeComparison(@PathVariable String taskId) {
        try {
            AvgQoeComparisonDTO result = new AvgQoeComparisonDTO();
            
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
            
            // 转换时间格式和时区
            String convertedStartTime = convertTimeFormatAndTimezone(startTime);
            String convertedEndTime = convertTimeFormatAndTimezone(endTime);
            
            // 2. 通过device_id去test_settings_device_imsi_mapping表获取gpsi
            QueryWrapper<TestSettingsDeviceImsiMapping> mappingWrapper = new QueryWrapper<>();
            mappingWrapper.eq("device_id", deviceId);
            TestSettingsDeviceImsiMapping mapping = deviceImsiMappingService.getOne(mappingWrapper);
            
            if (mapping == null) {
                return Result.error("未找到设备ID对应的GPSI映射");
            }
            
            String gpsi = mapping.getGpsi();
            
            // 3. 通过taskId在vmos表获取avg_qoe字段（端侧平均QOE数据），按sequence_number排序
            QueryWrapper<VmosData> vmosWrapper = new QueryWrapper<>();
            vmosWrapper.eq("task_id", taskId);
            vmosWrapper.orderByAsc("sequence_number");
            List<VmosData> vmosDataList = vmosDataService.list(vmosWrapper);
            
            List<AvgQoeComparisonDTO.ClientAvgQoeData> clientAvgQoeList = new ArrayList<>();
            if (vmosDataList != null && !vmosDataList.isEmpty()) {
                for (VmosData vmosData : vmosDataList) {
                    AvgQoeComparisonDTO.ClientAvgQoeData clientAvgQoe = new AvgQoeComparisonDTO.ClientAvgQoeData();
                    clientAvgQoe.setSequenceNumber(vmosData.getSequenceNumber());
                    
                    // 解析avg_qoe字段
                    String avgQoeStr = vmosData.getAvgQoe();
                    if (avgQoeStr != null && !avgQoeStr.trim().isEmpty()) {
                        try {
                            BigDecimal avgQoe = new BigDecimal(avgQoeStr);
                            clientAvgQoe.setAvgQoe(avgQoe);
                        } catch (NumberFormatException e) {
                            log.warn("无法解析avg_qoe字段: {}", avgQoeStr);
                            clientAvgQoe.setAvgQoe(BigDecimal.ZERO);
                        }
                    } else {
                        clientAvgQoe.setAvgQoe(BigDecimal.ZERO);
                    }
                    
                    clientAvgQoe.setTimeStamp(vmosData.getSequenceNumber());
                    clientAvgQoeList.add(clientAvgQoe);
                }
            }
            result.setClientAvgQoeList(clientAvgQoeList);
            
            // 4. 查询network_data表获取avg_qoe字段
            String appService = (app != null ? app : "") + "-" + (service != null ? service : "");
            
            QueryWrapper<NetworkData> networkWrapper = new QueryWrapper<>();
            networkWrapper.eq("gpsi", gpsi);
            if (appService != null && !appService.trim().isEmpty()) {
                networkWrapper.eq("sub_app_id", appService);
            }
            
            // 如果用户选择了网络侧开始时间，使用该时间；否则使用转换后的start_time
            String networkStartTime = taskInfo.getNetworkStartTime();
            if (networkStartTime != null && !networkStartTime.trim().isEmpty()) {
                networkWrapper.ge("start_time", networkStartTime);
            } else {
                if (convertedStartTime != null && !convertedStartTime.trim().isEmpty()) {
                    networkWrapper.ge("start_time", convertedStartTime);
                }
            }
            
            if (convertedEndTime != null && !convertedEndTime.trim().isEmpty()) {
                networkWrapper.le("time_stamp", convertedEndTime);
            }
            networkWrapper.orderByAsc("start_time");
            List<NetworkData> networkDataList = networkDataService.list(networkWrapper);
            
            List<AvgQoeComparisonDTO.NetworkAvgQoeData> networkAvgQoeList = new ArrayList<>();
            if (networkDataList != null && !networkDataList.isEmpty()) {
                for (NetworkData networkData : networkDataList) {
                    AvgQoeComparisonDTO.NetworkAvgQoeData networkAvgQoe = new AvgQoeComparisonDTO.NetworkAvgQoeData();
                    networkAvgQoe.setTimeStamp(networkData.getTimeStamp());
                    networkAvgQoe.setStartTime(networkData.getStartTime());
                    
                    // 解析avg_qoe字段
                    String avgQoeStr = networkData.getAvgQoe();
                    if (avgQoeStr != null && !avgQoeStr.trim().isEmpty()) {
                        try {
                            BigDecimal avgQoe = new BigDecimal(avgQoeStr);
                            networkAvgQoe.setAvgQoe(avgQoe);
                        } catch (NumberFormatException e) {
                            log.warn("无法解析avg_qoe字段: {}", avgQoeStr);
                            networkAvgQoe.setAvgQoe(BigDecimal.ZERO);
                        }
                    } else {
                        networkAvgQoe.setAvgQoe(BigDecimal.ZERO);
                    }
                    
                    networkAvgQoeList.add(networkAvgQoe);
                }
            }
            result.setNetworkAvgQoeList(networkAvgQoeList);
            result.setNetworkStartTime(taskInfo.getNetworkStartTime());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to get avg QOE comparison: {}", e.getMessage(), e);
            return Result.error("获取平均QOE对比数据失败: " + e.getMessage());
        }
    }
}

