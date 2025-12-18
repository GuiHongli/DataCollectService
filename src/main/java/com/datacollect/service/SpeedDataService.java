package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.SpeedData;

import java.util.List;

/**
 * 速率数据服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface SpeedDataService extends IService<SpeedData> {
    
    /**
     * 批量保存速率数据
     * 
     * @param speedDataList 速率数据列表
     * @param taskId 任务ID
     * @return 是否保存成功
     */
    boolean batchSaveSpeedData(List<SpeedData> speedDataList, String taskId);
}







