package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.RttData;

import java.util.List;

/**
 * RTT数据服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface RttDataService extends IService<RttData> {
    
    /**
     * 批量保存RTT数据
     * 
     * @param rttDataList RTT数据列表
     * @param taskId 任务ID
     * @return 是否保存成功
     */
    boolean batchSaveRttData(List<RttData> rttDataList, String taskId);
}

