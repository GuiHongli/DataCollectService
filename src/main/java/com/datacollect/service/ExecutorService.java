package com.datacollect.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.Executor;
import com.datacollect.entity.dto.ExecutorDTO;

import java.util.List;
import java.util.Map;

public interface ExecutorService extends IService<Executor> {
    
    /**
     * 获取执行机分页数据（包含完整地域信息）
     */
    Page<ExecutorDTO> getExecutorPageWithRegion(Integer current, Integer size, String name, String ipAddress, Long regionId);
    
    /**
     * 获取用于下拉选择的地域选项（仅城市级别）
     */
    List<Map<String, Object>> getRegionOptionsForSelect();
    
    /**
     * 获取用于下拉选择的执行机选项（包含地域信息）
     */
    List<Map<String, Object>> getExecutorOptionsForSelect();
}
