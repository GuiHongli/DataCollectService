package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.VideoData;

import java.util.List;

/**
 * 视频卡顿数据服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface VideoDataService extends IService<VideoData> {
    
    /**
     * 批量保存视频卡顿数据
     * 
     * @param videoDataList 视频卡顿数据列表
     * @param taskId 任务ID
     * @return 是否保存成功
     */
    boolean batchSaveVideoData(List<VideoData> videoDataList, String taskId);
}

