package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.VmosData;

import java.util.List;

/**
 * vMOS数据服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface VmosDataService extends IService<VmosData> {
    
    /**
     * 批量保存vMOS数据
     * 
     * @param vmosDataList vMOS数据列表
     * @param taskId 任务ID
     * @return 是否保存成功
     */
    boolean batchSaveVmosData(List<VmosData> vmosDataList, String taskId);
}







