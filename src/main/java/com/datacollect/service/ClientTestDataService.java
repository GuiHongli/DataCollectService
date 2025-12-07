package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.ClientTestData;

import java.util.List;

/**
 * 端侧测试数据服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface ClientTestDataService extends IService<ClientTestData> {
    
    /**
     * 批量保存端侧测试数据
     * 
     * @param clientTestDataList 端侧测试数据列表
     * @param taskId 任务ID
     * @return 是否保存成功
     */
    boolean batchSaveClientTestData(List<ClientTestData> clientTestDataList, String taskId);
}

