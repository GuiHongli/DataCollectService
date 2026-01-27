package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.GameDelayData;
import com.datacollect.mapper.GameDelayDataMapper;
import com.datacollect.service.GameDelayDataService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 游戏时延数据服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Service
public class GameDelayDataServiceImpl extends ServiceImpl<GameDelayDataMapper, GameDelayData> implements GameDelayDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameDelayDataServiceImpl.class);

    @Override
    public boolean batchSaveGameDelayData(List<GameDelayData> gameDelayDataList, String taskId) {
        if (gameDelayDataList == null || gameDelayDataList.isEmpty()) {
            LOGGER.warn("GameDelayData list is empty, cannot save");
            return false;
        }

        if (taskId == null || taskId.trim().isEmpty()) {
            LOGGER.warn("TaskId is null or empty, cannot save");
            return false;
        }

        try {
            // 检查该taskId是否已有数据
            QueryWrapper<GameDelayData> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("task_id", taskId);
            queryWrapper.last("LIMIT 1");
            GameDelayData existingData = getOne(queryWrapper);
            
            if (existingData != null) {
                LOGGER.info("GameDelayData already exists for taskId, skip saving - taskId: {}", taskId);
                return true;
            }

            // 设置任务ID和创建时间
            LocalDateTime now = LocalDateTime.now();
            for (GameDelayData gameDelayData : gameDelayDataList) {
                gameDelayData.setTaskId(taskId);
                gameDelayData.setCreateTime(now);
                gameDelayData.setUpdateTime(now);
            }

            // 批量保存
            boolean success = saveBatch(gameDelayDataList);
            if (success) {
                LOGGER.info("GameDelayData batch saved successfully - taskId: {}, count: {}", taskId, gameDelayDataList.size());
            } else {
                LOGGER.error("Failed to batch save GameDelayData - taskId: {}, count: {}", taskId, gameDelayDataList.size());
            }
            return success;
        } catch (Exception e) {
            LOGGER.error("Error batch saving GameDelayData - taskId: {}, error: {}", taskId, e.getMessage(), e);
            return false;
        }
    }
}

