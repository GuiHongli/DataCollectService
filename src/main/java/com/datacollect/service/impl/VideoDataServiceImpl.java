package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.VideoData;
import com.datacollect.mapper.VideoDataMapper;
import com.datacollect.service.VideoDataService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 视频卡顿数据服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Service
public class VideoDataServiceImpl extends ServiceImpl<VideoDataMapper, VideoData> implements VideoDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoDataServiceImpl.class);

    @Override
    public boolean batchSaveVideoData(List<VideoData> videoDataList, String taskId) {
        if (videoDataList == null || videoDataList.isEmpty()) {
            LOGGER.warn("VideoData list is empty, cannot save");
            return false;
        }

        if (taskId == null || taskId.trim().isEmpty()) {
            LOGGER.warn("TaskId is null or empty, cannot save");
            return false;
        }

        try {
            // check该taskId是否已有数据
            QueryWrapper<VideoData> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("task_id", taskId);
            queryWrapper.last("LIMIT 1");
            VideoData existingData = getOne(queryWrapper);
            
            if (existingData != null) {
                LOGGER.info("VideoData already exists for taskId, skip saving - taskId: {}", taskId);
                return true;
            }

            // set任务ID和create时间
            LocalDateTime now = LocalDateTime.now();
            for (VideoData videoData : videoDataList) {
                videoData.setTaskId(taskId);
                videoData.setCreateTime(now);
                videoData.setUpdateTime(now);
            }

            // 批量save
            boolean success = saveBatch(videoDataList);
            if (success) {
                LOGGER.info("VideoData batch saved successfully - taskId: {}, count: {}", taskId, videoDataList.size());
            } else {
                LOGGER.error("Failed to batch save VideoData - taskId: {}, count: {}", taskId, videoDataList.size());
            }
            return success;
        } catch (Exception e) {
            LOGGER.error("Error batch saving VideoData - taskId: {}, error: {}", taskId, e.getMessage(), e);
            return false;
        }
    }
}




