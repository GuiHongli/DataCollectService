package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.SpeedData;
import com.datacollect.mapper.SpeedDataMapper;
import com.datacollect.service.SpeedDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 速率数据服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class SpeedDataServiceImpl extends ServiceImpl<SpeedDataMapper, SpeedData> implements SpeedDataService {

    @Override
    public boolean batchSaveSpeedData(List<SpeedData> speedDataList, String taskId) {
        if (speedDataList == null || speedDataList.isEmpty()) {
            log.warn("SpeedData list is empty, cannot save");
            return false;
        }

        if (taskId == null || taskId.trim().isEmpty()) {
            log.warn("TaskId is null or empty, cannot save");
            return false;
        }

        try {
            // 检查该taskId是否已有数据
            QueryWrapper<SpeedData> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("task_id", taskId);
            queryWrapper.last("LIMIT 1");
            SpeedData existingData = getOne(queryWrapper);
            
            if (existingData != null) {
                log.info("SpeedData already exists for taskId, skip saving - taskId: {}", taskId);
                return true;
            }

            // 设置任务ID和创建时间
            LocalDateTime now = LocalDateTime.now();
            for (SpeedData speedData : speedDataList) {
                speedData.setTaskId(taskId);
                speedData.setCreateTime(now);
                speedData.setUpdateTime(now);
            }

            // 批量保存
            boolean success = saveBatch(speedDataList);
            if (success) {
                log.info("SpeedData batch saved successfully - taskId: {}, count: {}", taskId, speedDataList.size());
            } else {
                log.error("Failed to batch save SpeedData - taskId: {}, count: {}", taskId, speedDataList.size());
            }
            return success;
        } catch (Exception e) {
            log.error("Error batch saving SpeedData - taskId: {}, error: {}", taskId, e.getMessage(), e);
            return false;
        }
    }
}








