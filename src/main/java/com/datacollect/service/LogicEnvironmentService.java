package com.datacollect.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.LogicEnvironment;
import com.datacollect.entity.dto.LogicEnvironmentDTO;

public interface LogicEnvironmentService extends IService<LogicEnvironment> {
    
    /**
     * 分页查询逻辑环境详细信息
     */
    Page<LogicEnvironmentDTO> getLogicEnvironmentPageWithDetails(Integer current, Integer size, String name, Long executorId);
}
