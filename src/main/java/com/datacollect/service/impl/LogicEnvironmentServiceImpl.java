package com.datacollect.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.Executor;
import com.datacollect.entity.LogicEnvironment;
import com.datacollect.entity.LogicEnvironmentNetwork;
import com.datacollect.entity.LogicEnvironmentUe;
import com.datacollect.entity.LogicNetwork;
import com.datacollect.entity.Ue;
import com.datacollect.entity.dto.LogicEnvironmentDTO;
import com.datacollect.mapper.LogicEnvironmentMapper;
import com.datacollect.service.ExecutorService;
import com.datacollect.service.LogicEnvironmentNetworkService;
import com.datacollect.service.LogicEnvironmentService;
import com.datacollect.service.LogicEnvironmentUeService;
import com.datacollect.service.LogicNetworkService;
import com.datacollect.service.UeService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LogicEnvironmentServiceImpl extends ServiceImpl<LogicEnvironmentMapper, LogicEnvironment> implements LogicEnvironmentService {
    
    @Autowired
    private LogicEnvironmentUeService logicEnvironmentUeService;
    
    @Autowired
    private LogicEnvironmentNetworkService logicEnvironmentNetworkService;
    
    @Autowired
    private UeService ueService;
    
    @Autowired
    private ExecutorService executorService;
    
    @Autowired
    private LogicNetworkService logicNetworkService;
    
    @Override
    public Page<LogicEnvironmentDTO> getLogicEnvironmentPageWithDetails(Integer current, Integer size, String name, Long executorId) {
        // 查询逻辑环境基本信息
        Page<LogicEnvironment> page = new Page<>(current, size);
        QueryWrapper<LogicEnvironment> queryWrapper = new QueryWrapper<>();
        
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        if (executorId != null) {
            queryWrapper.eq("executor_id", executorId);
        }
        
        queryWrapper.orderByDesc("create_time");
        Page<LogicEnvironment> logicEnvironmentPage = this.page(page, queryWrapper);
        
        // 转换为DTO
        Page<LogicEnvironmentDTO> resultPage = new Page<>(current, size, logicEnvironmentPage.getTotal());
        List<LogicEnvironmentDTO> dtoList = new ArrayList<>();
        
        for (LogicEnvironment logicEnvironment : logicEnvironmentPage.getRecords()) {
            LogicEnvironmentDTO dto = convertToDTO(logicEnvironment);
            dtoList.add(dto);
        }
        
        resultPage.setRecords(dtoList);
        return resultPage;
    }
    
    private LogicEnvironmentDTO convertToDTO(LogicEnvironment logicEnvironment) {
        LogicEnvironmentDTO dto = new LogicEnvironmentDTO();
        dto.setId(logicEnvironment.getId());
        dto.setName(logicEnvironment.getName());
        dto.setExecutorId(logicEnvironment.getExecutorId());
        dto.setDescription(logicEnvironment.getDescription());
        dto.setStatus(logicEnvironment.getStatus());
        dto.setCreateBy(logicEnvironment.getCreateBy());
        dto.setUpdateBy(logicEnvironment.getUpdateBy());
        dto.setCreateTime(logicEnvironment.getCreateTime());
        dto.setUpdateTime(logicEnvironment.getUpdateTime());

        enrichExecutorInfo(dto, logicEnvironment);
        dto.setUeList(buildUeInfoList(logicEnvironment));
        dto.setNetworkList(buildNetworkInfoList(logicEnvironment));
        return dto;
    }

    private void enrichExecutorInfo(LogicEnvironmentDTO dto, LogicEnvironment logicEnvironment) {
        try {
            Executor executor = executorService.getById(logicEnvironment.getExecutorId());
            if (executor != null) {
                dto.setExecutorName(executor.getName());
                dto.setExecutorIpAddress(executor.getIpAddress());
                dto.setExecutorRegionName("中国/北京");
            } else {
                setUnknownExecutorInfo(dto);
            }
        } catch (Exception e) {
            setUnknownExecutorInfo(dto);
        }
    }

    private void setUnknownExecutorInfo(LogicEnvironmentDTO dto) {
        dto.setExecutorName("未知执行机");
        dto.setExecutorIpAddress("未知IP");
        dto.setExecutorRegionName("未知地域");
    }

    private List<LogicEnvironmentDTO.UeInfo> buildUeInfoList(LogicEnvironment logicEnvironment) {
        List<LogicEnvironmentDTO.UeInfo> ueInfoList = new ArrayList<>();
        try {
            List<Long> ueIds = getUeIdsByEnvironment(logicEnvironment.getId());
            if (!ueIds.isEmpty()) {
                List<Ue> ues = getUesByIds(ueIds);
                ueInfoList = createUeInfoList(ues);
            }
        } catch (Exception e) {
            System.err.println("获取UE信息失败: " + e.getMessage());
        }
        return ueInfoList;
    }

    private List<LogicEnvironmentDTO.NetworkInfo> buildNetworkInfoList(LogicEnvironment logicEnvironment) {
        List<LogicEnvironmentDTO.NetworkInfo> networkInfoList = new ArrayList<>();
        try {
            List<Long> networkIds = getNetworkIdsByEnvironment(logicEnvironment.getId());
            if (!networkIds.isEmpty()) {
                List<LogicNetwork> networks = getNetworksByIds(networkIds);
                networkInfoList = createNetworkInfoList(networks);
            }
        } catch (Exception e) {
            System.err.println("获取逻辑组网信息失败: " + e.getMessage());
        }
        return networkInfoList;
    }

    private List<Long> getUeIdsByEnvironment(Long environmentId) {
        QueryWrapper<LogicEnvironmentUe> ueQueryWrapper = new QueryWrapper<>();
        ueQueryWrapper.eq("logic_environment_id", environmentId);
        List<LogicEnvironmentUe> logicEnvironmentUes = logicEnvironmentUeService.list(ueQueryWrapper);
        return logicEnvironmentUes.stream().map(LogicEnvironmentUe::getUeId).collect(Collectors.toList());
    }

    private List<Ue> getUesByIds(List<Long> ueIds) {
        QueryWrapper<Ue> ueQuery = new QueryWrapper<>();
        ueQuery.in("id", ueIds);
        return ueService.list(ueQuery);
    }

    private List<LogicEnvironmentDTO.UeInfo> createUeInfoList(List<Ue> ues) {
        List<LogicEnvironmentDTO.UeInfo> ueInfoList = new ArrayList<>();
        for (Ue ue : ues) {
            LogicEnvironmentDTO.UeInfo ueInfo = new LogicEnvironmentDTO.UeInfo();
            ueInfo.setId(ue.getId());
            ueInfo.setUeId(ue.getUeId());
            ueInfo.setName(ue.getName());
            ueInfo.setPurpose(ue.getPurpose());
            ueInfo.setNetworkTypeName("正常网络");
            ueInfoList.add(ueInfo);
        }
        return ueInfoList;
    }

    private List<Long> getNetworkIdsByEnvironment(Long environmentId) {
        QueryWrapper<LogicEnvironmentNetwork> networkQueryWrapper = new QueryWrapper<>();
        networkQueryWrapper.eq("logic_environment_id", environmentId);
        List<LogicEnvironmentNetwork> logicEnvironmentNetworks = logicEnvironmentNetworkService.list(networkQueryWrapper);
        return logicEnvironmentNetworks.stream().map(LogicEnvironmentNetwork::getLogicNetworkId).collect(Collectors.toList());
    }

    private List<LogicNetwork> getNetworksByIds(List<Long> networkIds) {
        QueryWrapper<LogicNetwork> networkQuery = new QueryWrapper<>();
        networkQuery.in("id", networkIds);
        return logicNetworkService.list(networkQuery);
    }

    private List<LogicEnvironmentDTO.NetworkInfo> createNetworkInfoList(List<LogicNetwork> networks) {
        List<LogicEnvironmentDTO.NetworkInfo> networkInfoList = new ArrayList<>();
        for (LogicNetwork network : networks) {
            LogicEnvironmentDTO.NetworkInfo networkInfo = new LogicEnvironmentDTO.NetworkInfo();
            networkInfo.setId(network.getId());
            networkInfo.setName(network.getName());
            networkInfo.setDescription(network.getDescription());
            networkInfoList.add(networkInfo);
        }
        return networkInfoList;
    }
    
    @Override
    public List<LogicEnvironment> getByExecutorId(Long executorId) {
        log.debug("Getting logic environments associated with executor - Executor ID: {}", executorId);
        QueryWrapper<LogicEnvironment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("executor_id", executorId);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("name");
        List<LogicEnvironment> environments = list(queryWrapper);
        log.debug("Number of logic environments associated with executor {}: {}", executorId, environments.size());
        for (LogicEnvironment env : environments) {
            log.debug("Executor {} associated logic environment: {} (ID: {})", executorId, env.getName(), env.getId());
        }
        return environments;
    }
    
    @Override
    public LogicEnvironmentDTO getLogicEnvironmentDTO(Long logicEnvironmentId) {
        log.debug("Getting logic environment details - Logic Environment ID: {}", logicEnvironmentId);
        LogicEnvironment logicEnvironment = getById(logicEnvironmentId);
        if (logicEnvironment == null) {
            log.warn("Logic environment not found - Logic Environment ID: {}", logicEnvironmentId);
            return null;
        }
        LogicEnvironmentDTO dto = convertToDTO(logicEnvironment);
        log.debug("Retrieved logic environment details: {} (ID: {})", dto.getName(), dto.getId());
        return dto;
    }
}
