package com.datacollect.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.LogicEnvironmentNetwork;
import com.datacollect.mapper.LogicEnvironmentNetworkMapper;
import com.datacollect.service.LogicEnvironmentNetworkService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class LogicEnvironmentNetworkServiceImpl extends ServiceImpl<LogicEnvironmentNetworkMapper, LogicEnvironmentNetwork> implements LogicEnvironmentNetworkService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogicEnvironmentNetworkServiceImpl.class);
    
    @Override
    public List<LogicEnvironmentNetwork> getByLogicEnvironmentId(Long logicEnvironmentId) {
        LOGGER.debug("Getting network groups associated with logic environment - Logic Environment ID: {}", logicEnvironmentId);
        QueryWrapper<LogicEnvironmentNetwork> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("logic_environment_id", logicEnvironmentId);
        List<LogicEnvironmentNetwork> networks = list(queryWrapper);
        LOGGER.debug("Number of network groups associated with logic environment {}: {}", logicEnvironmentId, networks.size());
        for (LogicEnvironmentNetwork network : networks) {
            LOGGER.debug("Logic environment {} associated network group ID: {}", logicEnvironmentId, network.getLogicNetworkId());
        }
        return networks;
    }
}
