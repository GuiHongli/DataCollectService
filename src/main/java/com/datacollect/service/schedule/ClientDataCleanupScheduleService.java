package com.datacollect.service.schedule;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datacollect.entity.ClientTaskInfo;
import com.datacollect.entity.LostData;
import com.datacollect.entity.RttData;
import com.datacollect.entity.SpeedData;
import com.datacollect.entity.VideoData;
import com.datacollect.entity.VmosData;
import com.datacollect.mapper.LostDataMapper;
import com.datacollect.mapper.RttDataMapper;
import com.datacollect.mapper.SpeedDataMapper;
import com.datacollect.mapper.TaskInfoMapper;
import com.datacollect.mapper.VideoDataMapper;
import com.datacollect.mapper.VmosDataMapper;
import com.datacollect.service.LostDataService;
import com.datacollect.service.RttDataService;
import com.datacollect.service.SpeedDataService;
import com.datacollect.service.TaskInfoService;
import com.datacollect.service.VideoDataService;
import com.datacollect.service.VmosDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 端侧数据清理定时任务服务
 * 每三个月执行一次，物理删除三个月之前的端侧数据
 * 
 * @author system
 * @since 2024-01-01
 */
@Service
public class ClientDataCleanupScheduleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientDataCleanupScheduleService.class);

    @Autowired
    private TaskInfoService taskInfoService;

    @Autowired
    private TaskInfoMapper taskInfoMapper;

    @Autowired
    private SpeedDataService speedDataService;

    @Autowired
    private SpeedDataMapper speedDataMapper;

    @Autowired
    private VmosDataService vmosDataService;

    @Autowired
    private VmosDataMapper vmosDataMapper;

    @Autowired
    private RttDataService rttDataService;

    @Autowired
    private RttDataMapper rttDataMapper;

    @Autowired
    private LostDataService lostDataService;

    @Autowired
    private LostDataMapper lostDataMapper;

    @Autowired
    private VideoDataService videoDataService;

    @Autowired
    private VideoDataMapper videoDataMapper;

    /**
     * 每三个月执行一次数据清理
     * cron表达式: 0 0 0 1 1,4,7,10 * 表示每年1月、4月、7月、10月的第一天凌晨0点执行
     * 这样每3个月执行一次
     */
    @Scheduled(cron = "0 0 0 1 1,4,7,10 *")
    public void scheduledCleanupOldData() {
        LOGGER.info("Scheduled task: Starting client data cleanup (physical delete)...");
        
        try {
            // 计算3个月前的日期时间
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            LOGGER.info("Cleaning up client data before: {}", threeMonthsAgo);
            
            // 查询3个月前的任务信息（只处理未逻辑删除的数据）
            // 使用Mapper直接查询，避免逻辑删除过滤
            QueryWrapper<ClientTaskInfo> taskInfoWrapper = new QueryWrapper<>();
            taskInfoWrapper.lt("create_time", threeMonthsAgo);
            taskInfoWrapper.eq("deleted", 0); // 只处理未逻辑删除的数据
            List<ClientTaskInfo> oldTaskInfos = taskInfoMapper.selectList(taskInfoWrapper);
            
            if (oldTaskInfos == null || oldTaskInfos.isEmpty()) {
                LOGGER.info("No old client data found to cleanup");
                return;
            }
            
            LOGGER.info("Found {} old task info records to cleanup", oldTaskInfos.size());
            
            int deletedTaskInfoCount = 0;
            int deletedSpeedDataCount = 0;
            int deletedVmosDataCount = 0;
            int deletedRttDataCount = 0;
            int deletedLostDataCount = 0;
            int deletedVideoDataCount = 0;
            
            // 遍历每个任务，删除相关数据
            for (ClientTaskInfo taskInfo : oldTaskInfos) {
                String taskId = taskInfo.getTaskId();
                if (taskId == null || taskId.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    // 删除speed数据（物理删除）
                    QueryWrapper<SpeedData> speedWrapper = new QueryWrapper<>();
                    speedWrapper.eq("task_id", taskId);
                    List<SpeedData> speedDataList = speedDataService.list(speedWrapper);
                    if (speedDataList != null && !speedDataList.isEmpty()) {
                        for (SpeedData speedData : speedDataList) {
                            speedDataMapper.deleteById(speedData.getId());
                            deletedSpeedDataCount++;
                        }
                    }
                    
                    // 删除vmos数据（物理删除）
                    QueryWrapper<VmosData> vmosWrapper = new QueryWrapper<>();
                    vmosWrapper.eq("task_id", taskId);
                    List<VmosData> vmosDataList = vmosDataService.list(vmosWrapper);
                    if (vmosDataList != null && !vmosDataList.isEmpty()) {
                        for (VmosData vmosData : vmosDataList) {
                            vmosDataMapper.deleteById(vmosData.getId());
                            deletedVmosDataCount++;
                        }
                    }
                    
                    // 删除rtt数据（物理删除）
                    QueryWrapper<RttData> rttWrapper = new QueryWrapper<>();
                    rttWrapper.eq("task_id", taskId);
                    List<RttData> rttDataList = rttDataService.list(rttWrapper);
                    if (rttDataList != null && !rttDataList.isEmpty()) {
                        for (RttData rttData : rttDataList) {
                            rttDataMapper.deleteById(rttData.getId());
                            deletedRttDataCount++;
                        }
                    }
                    
                    // 删除lost数据（物理删除）
                    QueryWrapper<LostData> lostWrapper = new QueryWrapper<>();
                    lostWrapper.eq("task_id", taskId);
                    List<LostData> lostDataList = lostDataService.list(lostWrapper);
                    if (lostDataList != null && !lostDataList.isEmpty()) {
                        for (LostData lostData : lostDataList) {
                            lostDataMapper.deleteById(lostData.getId());
                            deletedLostDataCount++;
                        }
                    }
                    
                    // 删除video数据（物理删除）
                    QueryWrapper<VideoData> videoWrapper = new QueryWrapper<>();
                    videoWrapper.eq("task_id", taskId);
                    List<VideoData> videoDataList = videoDataService.list(videoWrapper);
                    if (videoDataList != null && !videoDataList.isEmpty()) {
                        for (VideoData videoData : videoDataList) {
                            videoDataMapper.deleteById(videoData.getId());
                            deletedVideoDataCount++;
                        }
                    }
                    
                    // 最后删除任务信息（物理删除）
                    taskInfoMapper.deleteById(taskInfo.getId());
                    deletedTaskInfoCount++;
                    
                } catch (Exception e) {
                    LOGGER.error("Failed to cleanup data for taskId: {}, error: {}", taskId, e.getMessage(), e);
                    // 继续处理下一个任务
                }
            }
            
            LOGGER.info("Scheduled task: Client data cleanup completed. " +
                    "Deleted - TaskInfo: {}, SpeedData: {}, VmosData: {}, RttData: {}, LostData: {}, VideoData: {}",
                    deletedTaskInfoCount, deletedSpeedDataCount, deletedVmosDataCount,
                    deletedRttDataCount, deletedLostDataCount, deletedVideoDataCount);
            
        } catch (Exception e) {
            LOGGER.error("Scheduled task: Failed to cleanup old client data - error: {}", e.getMessage(), e);
            // 不抛出异常，避免影响定时任务继续执行
        }
    }
}

