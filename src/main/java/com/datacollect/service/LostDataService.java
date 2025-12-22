package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.LostData;

import java.util.List;

/**
 * 丢包率数据服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface LostDataService extends IService<LostData> {
    
    /**
     * 批量保存丢包率数据
     * 
     * @param lostDataList 丢包率数据列表
     * @param taskId 任务ID
     * @return 是否保存成功
     */
    boolean batchSaveLostData(List<LostData> lostDataList, String taskId);
}




