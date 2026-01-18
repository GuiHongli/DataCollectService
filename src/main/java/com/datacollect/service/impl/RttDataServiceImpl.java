package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.RttData;
import com.datacollect.mapper.RttDataMapper;
import com.datacollect.service.RttDataService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * RTT数据服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Service
public class RttDataServiceImpl extends ServiceImpl<RttDataMapper, RttData> implements RttDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RttDataServiceImpl.class);

    @Override
    public boolean batchSaveRttData(List<RttData> rttDataList, String taskId) {
        if (rttDataList == null || rttDataList.isEmpty()) {
            LOGGER.warn("RttData list is empty, cannot save");
            return false;
        }

        if (taskId == null || taskId.trim().isEmpty()) {
            LOGGER.warn("TaskId is null or empty, cannot save");
            return false;
        }

        try {
            // check该taskId是否已有数据
            QueryWrapper<RttData> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("task_id", taskId);
            queryWrapper.last("LIMIT 1");
            RttData existingData = getOne(queryWrapper);
            
            if (existingData != null) {
                LOGGER.info("RttData already exists for taskId, skip saving - taskId: {}", taskId);
                return true;
            }

            // set任务ID和create时间
            LocalDateTime now = LocalDateTime.now();
            for (RttData rttData : rttDataList) {
                rttData.setTaskId(taskId);
                rttData.setCreateTime(now);
                rttData.setUpdateTime(now);
            }

            // 批量save
            boolean success = saveBatch(rttDataList);
            if (success) {
                LOGGER.info("RttData batch saved successfully - taskId: {}, count: {}", taskId, rttDataList.size());
            } else {
                LOGGER.error("Failed to batch save RttData - taskId: {}, count: {}", taskId, rttDataList.size());
            }
            return success;
        } catch (Exception e) {
            LOGGER.error("Error batch saving RttData - taskId: {}, error: {}", taskId, e.getMessage(), e);
            return false;
        }
    }
}




