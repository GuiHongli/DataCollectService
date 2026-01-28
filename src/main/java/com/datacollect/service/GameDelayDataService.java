package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.GameDelayData;

import java.util.List;

/**
 * 游戏时延数据服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface GameDelayDataService extends IService<GameDelayData> {
    
    /**
     * 批量保存游戏时延数据
     * 
     * @param gameDelayDataList 游戏时延数据列表
     * @param taskId 任务ID
     * @return 是否保存成功
     */
    boolean batchSaveGameDelayData(List<GameDelayData> gameDelayDataList, String taskId);
}


