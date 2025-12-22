package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.NetworkData;

import java.util.List;

/**
 * 网络侧数据服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface NetworkDataService extends IService<NetworkData> {
    
    /**
     * 批量保存网络侧数据
     * 
     * @param networkDataList 网络侧数据列表
     * @param fileName 文件名
     * @return 是否保存成功
     */
    boolean batchSaveNetworkData(List<NetworkData> networkDataList, String fileName);
}




