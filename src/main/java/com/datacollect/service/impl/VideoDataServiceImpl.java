package com.datacollect.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.VideoData;
import com.datacollect.mapper.VideoDataMapper;
import com.datacollect.service.VideoDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频卡顿数据服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class VideoDataServiceImpl extends ServiceImpl<VideoDataMapper, VideoData> implements VideoDataService {

    @Override
    public boolean batchSaveVideoData(List<VideoData> videoDataList, String taskId) {
        if (videoDataList == null || videoDataList.isEmpty()) {
            log.warn("VideoData list is empty, cannot save");
            return false;
        }

        if (taskId == null || taskId.trim().isEmpty()) {
            log.warn("TaskId is null or empty, cannot save");
            return false;
        }

        try {
            // 设置任务ID和创建时间
            LocalDateTime now = LocalDateTime.now();
            for (VideoData videoData : videoDataList) {
                videoData.setTaskId(taskId);
                videoData.setCreateTime(now);
                videoData.setUpdateTime(now);
            }

            // 批量保存
            boolean success = saveBatch(videoDataList);
            if (success) {
                log.info("VideoData batch saved successfully - taskId: {}, count: {}", taskId, videoDataList.size());
            } else {
                log.error("Failed to batch save VideoData - taskId: {}, count: {}", taskId, videoDataList.size());
            }
            return success;
        } catch (Exception e) {
            log.error("Error batch saving VideoData - taskId: {}, error: {}", taskId, e.getMessage(), e);
            return false;
        }
    }
}




