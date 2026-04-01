package com.datacollect.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.Ue;
import com.datacollect.entity.dto.UeDTO;

import java.util.List;
import java.util.Map;

public interface UeService extends IService<Ue> {
    
    /**
     * 获取UE分页数据（包含网络类型名称）
     */
    Page<UeDTO> getUePageWithNetworkType(Integer current, Integer size, String name, String ueId, String purpose, Long networkTypeId);
    
    /**
     * 获取用于下拉选择的UE选项（包含详细信息）
     */
    List<Map<String, Object>> getUeOptionsForSelect();
    
    /**
     * 标记UE为使用中
     * 
     * @param ueIds UE ID列表
     * @return 是否成功
     */
    boolean markUesInUse(List<Integer> ueIds);
    
    /**
     * 标记UE为可用（未使用）
     * 
     * @param ueIds UE ID列表
     * @return 是否成功
     */
    boolean markUesAvailable(List<Integer> ueIds);
    
    /**
     * 检查UE是否可用（未使用中且状态为可用）
     * 
     * @param ueIds UE ID列表
     * @return 不可用的UE ID列表
     */
    List<Integer> checkUesAvailability(List<Integer> ueIds);
}
