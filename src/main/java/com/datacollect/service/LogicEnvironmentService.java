package com.datacollect.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.LogicEnvironment;
import com.datacollect.entity.dto.LogicEnvironmentDTO;
import java.util.List;

public interface LogicEnvironmentService extends IService<LogicEnvironment> {
    
    /**
     * 分页查询逻辑环境详细信息
     */
    Page<LogicEnvironmentDTO> getLogicEnvironmentPageWithDetails(Integer current, Integer size, String name, Long executorId);
    
    /**
     * 根据执行机ID获取逻辑环境列表
     */
    List<LogicEnvironment> getByExecutorId(Long executorId);
    
    /**
     * 根据逻辑环境ID获取详细信息DTO
     */
    LogicEnvironmentDTO getLogicEnvironmentDTO(Long logicEnvironmentId);
}
