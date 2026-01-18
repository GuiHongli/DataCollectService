package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.LostData;
import com.datacollect.mapper.LostDataMapper;
import com.datacollect.service.LostDataService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 丢包率数据服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Service
public class LostDataServiceImpl extends ServiceImpl<LostDataMapper, LostData> implements LostDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LostDataServiceImpl.class);

    @Override
    public boolean batchSaveLostData(List<LostData> lostDataList, String taskId) {
        if (lostDataList == null || lostDataList.isEmpty()) {
            LOGGER.warn("LostData list is empty, cannot save");
            return false;
        }

        if (taskId == null || taskId.trim().isEmpty()) {
            LOGGER.warn("TaskId is null or empty, cannot save");
            return false;
        }

        try {
            // check该taskId是否已有数据
            QueryWrapper<LostData> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("task_id", taskId);
            queryWrapper.last("LIMIT 1");
            LostData existingData = getOne(queryWrapper);
            
            if (existingData != null) {
                LOGGER.info("LostData already exists for taskId, skip saving - taskId: {}", taskId);
                return true;
            }

            // set任务ID和create时间
            LocalDateTime now = LocalDateTime.now();
            for (LostData lostData : lostDataList) {
                lostData.setTaskId(taskId);
                lostData.setCreateTime(now);
                lostData.setUpdateTime(now);
            }

            // 批量save
            boolean success = saveBatch(lostDataList);
            if (success) {
                LOGGER.info("LostData batch saved successfully - taskId: {}, count: {}", taskId, lostDataList.size());
            } else {
                LOGGER.error("Failed to batch save LostData - taskId: {}, count: {}", taskId, lostDataList.size());
            }
            return success;
        } catch (Exception e) {
            LOGGER.error("Error batch saving LostData - taskId: {}, error: {}", taskId, e.getMessage(), e);
            return false;
        }
    }
}




