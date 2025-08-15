package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.LogicEnvironmentNetwork;
import java.util.List;

public interface LogicEnvironmentNetworkService extends IService<LogicEnvironmentNetwork> {
    
    /**
     * 根据逻辑环境ID获取关联的网络列表
     */
    List<LogicEnvironmentNetwork> getByLogicEnvironmentId(Long logicEnvironmentId);
}
