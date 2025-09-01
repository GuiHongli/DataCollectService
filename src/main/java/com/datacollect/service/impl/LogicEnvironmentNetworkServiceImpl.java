package com.datacollect.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.LogicEnvironmentNetwork;
import com.datacollect.mapper.LogicEnvironmentNetworkMapper;
import com.datacollect.service.LogicEnvironmentNetworkService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LogicEnvironmentNetworkServiceImpl extends ServiceImpl<LogicEnvironmentNetworkMapper, LogicEnvironmentNetwork> implements LogicEnvironmentNetworkService {
    
    @Override
    public List<LogicEnvironmentNetwork> getByLogicEnvironmentId(Long logicEnvironmentId) {
        log.debug("Getting network groups associated with logic environment - Logic Environment ID: {}", logicEnvironmentId);
        QueryWrapper<LogicEnvironmentNetwork> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("logic_environment_id", logicEnvironmentId);
        List<LogicEnvironmentNetwork> networks = list(queryWrapper);
        log.debug("Number of network groups associated with logic environment {}: {}", logicEnvironmentId, networks.size());
        for (LogicEnvironmentNetwork network : networks) {
            log.debug("Logic environment {} associated network group ID: {}", logicEnvironmentId, network.getLogicNetworkId());
        }
        return networks;
    }
}
