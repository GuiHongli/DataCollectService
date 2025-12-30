package com.datacollect.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.dto.NetworkDataGroupDTO;
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
    
    /**
     * 分页查询网络侧数据聚合（按GPSI+日期+子应用ID分组）
     * 
     * @param current 当前页
     * @param size 每页大小
     * @param gpsi GPSI筛选条件（可选）
     * @param date 日期筛选条件（可选，格式：YYYY-MM-DD）
     * @param subAppId 子应用ID筛选条件（可选）
     * @return 分页结果
     */
    Page<NetworkDataGroupDTO> getGroupedNetworkDataPage(Integer current, Integer size, String gpsi, String date, String subAppId);
}





