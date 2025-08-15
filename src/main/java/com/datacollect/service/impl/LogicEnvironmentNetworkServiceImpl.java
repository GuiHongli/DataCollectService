package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.LogicEnvironmentNetwork;
import com.datacollect.mapper.LogicEnvironmentNetworkMapper;
import com.datacollect.service.LogicEnvironmentNetworkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
public class LogicEnvironmentNetworkServiceImpl extends ServiceImpl<LogicEnvironmentNetworkMapper, LogicEnvironmentNetwork> implements LogicEnvironmentNetworkService {
    
    @Override
    public List<LogicEnvironmentNetwork> getByLogicEnvironmentId(Long logicEnvironmentId) {
        log.debug("获取逻辑环境关联的网络组网 - 逻辑环境ID: {}", logicEnvironmentId);
        QueryWrapper<LogicEnvironmentNetwork> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("logic_environment_id", logicEnvironmentId);
        List<LogicEnvironmentNetwork> networks = list(queryWrapper);
        log.debug("逻辑环境 {} 关联的网络组网数量: {}", logicEnvironmentId, networks.size());
        for (LogicEnvironmentNetwork network : networks) {
            log.debug("逻辑环境 {} 关联的网络组网ID: {}", logicEnvironmentId, network.getLogicNetworkId());
        }
        return networks;
    }
}
