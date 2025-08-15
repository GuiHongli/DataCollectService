package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.LogicEnvironment;
import com.datacollect.entity.LogicEnvironmentUe;
import com.datacollect.entity.LogicEnvironmentNetwork;
import com.datacollect.entity.Ue;
import com.datacollect.entity.Executor;
import com.datacollect.entity.LogicNetwork;
import com.datacollect.entity.dto.LogicEnvironmentDTO;
import com.datacollect.mapper.LogicEnvironmentMapper;
import com.datacollect.service.LogicEnvironmentService;
import com.datacollect.service.LogicEnvironmentUeService;
import com.datacollect.service.LogicEnvironmentNetworkService;
import com.datacollect.service.UeService;
import com.datacollect.service.ExecutorService;
import com.datacollect.service.LogicNetworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
        
        // 获取执行机信息
        try {
            Executor executor = executorService.getById(logicEnvironment.getExecutorId());
            if (executor != null) {
                dto.setExecutorName(executor.getName());
                dto.setExecutorIpAddress(executor.getIpAddress());
                // 这里可以进一步获取地域信息，暂时使用简单的方式
                dto.setExecutorRegionName("中国/北京");
            } else {
                dto.setExecutorName("未知执行机");
                dto.setExecutorIpAddress("未知IP");
                dto.setExecutorRegionName("未知地域");
            }
        } catch (Exception e) {
            dto.setExecutorName("未知执行机");
            dto.setExecutorIpAddress("未知IP");
            dto.setExecutorRegionName("未知地域");
        }
        
        // 获取UE信息
        List<LogicEnvironmentDTO.UeInfo> ueInfoList = new ArrayList<>();
        try {
            QueryWrapper<LogicEnvironmentUe> ueQueryWrapper = new QueryWrapper<>();
            ueQueryWrapper.eq("logic_environment_id", logicEnvironment.getId());
            List<LogicEnvironmentUe> logicEnvironmentUes = logicEnvironmentUeService.list(ueQueryWrapper);
            
            if (!logicEnvironmentUes.isEmpty()) {
                List<Long> ueIds = logicEnvironmentUes.stream()
                    .map(LogicEnvironmentUe::getUeId)
                    .collect(Collectors.toList());
                
                QueryWrapper<Ue> ueQuery = new QueryWrapper<>();
                ueQuery.in("id", ueIds);
                List<Ue> ues = ueService.list(ueQuery);
                
                for (Ue ue : ues) {
                    LogicEnvironmentDTO.UeInfo ueInfo = new LogicEnvironmentDTO.UeInfo();
                    ueInfo.setId(ue.getId());
                    ueInfo.setUeId(ue.getUeId());
                    ueInfo.setName(ue.getName());
                    ueInfo.setPurpose(ue.getPurpose());
                    ueInfo.setNetworkTypeName("正常网络"); // 这里应该从网络类型表获取
                    ueInfoList.add(ueInfo);
                }
            }
        } catch (Exception e) {
            // 如果获取UE信息失败，记录日志但不影响主流程
            System.err.println("获取UE信息失败: " + e.getMessage());
        }
        dto.setUeList(ueInfoList);
        
        // 获取逻辑组网信息
        List<LogicEnvironmentDTO.NetworkInfo> networkInfoList = new ArrayList<>();
        try {
            QueryWrapper<LogicEnvironmentNetwork> networkQueryWrapper = new QueryWrapper<>();
            networkQueryWrapper.eq("logic_environment_id", logicEnvironment.getId());
            List<LogicEnvironmentNetwork> logicEnvironmentNetworks = logicEnvironmentNetworkService.list(networkQueryWrapper);
            
            if (!logicEnvironmentNetworks.isEmpty()) {
                List<Long> networkIds = logicEnvironmentNetworks.stream()
                    .map(LogicEnvironmentNetwork::getLogicNetworkId)
                    .collect(Collectors.toList());
                
                QueryWrapper<LogicNetwork> networkQuery = new QueryWrapper<>();
                networkQuery.in("id", networkIds);
                List<LogicNetwork> networks = logicNetworkService.list(networkQuery);
                
                for (LogicNetwork network : networks) {
                    LogicEnvironmentDTO.NetworkInfo networkInfo = new LogicEnvironmentDTO.NetworkInfo();
                    networkInfo.setId(network.getId());
                    networkInfo.setName(network.getName());
                    networkInfo.setDescription(network.getDescription());
                    networkInfoList.add(networkInfo);
                }
            }
        } catch (Exception e) {
            // 如果获取逻辑组网信息失败，记录日志但不影响主流程
            System.err.println("获取逻辑组网信息失败: " + e.getMessage());
        }
        dto.setNetworkList(networkInfoList);
        
        return dto;
    }
    
    @Override
    public List<LogicEnvironment> getByExecutorId(Long executorId) {
        log.debug("获取执行机关联的逻辑环境 - 执行机ID: {}", executorId);
        QueryWrapper<LogicEnvironment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("executor_id", executorId);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("name");
        List<LogicEnvironment> environments = list(queryWrapper);
        log.debug("执行机 {} 关联的逻辑环境数量: {}", executorId, environments.size());
        for (LogicEnvironment env : environments) {
            log.debug("执行机 {} 关联的逻辑环境: {} (ID: {})", executorId, env.getName(), env.getId());
        }
        return environments;
    }
    
    @Override
    public LogicEnvironmentDTO getLogicEnvironmentDTO(Long logicEnvironmentId) {
        log.debug("获取逻辑环境详细信息 - 逻辑环境ID: {}", logicEnvironmentId);
        LogicEnvironment logicEnvironment = getById(logicEnvironmentId);
        if (logicEnvironment == null) {
            log.warn("逻辑环境不存在 - 逻辑环境ID: {}", logicEnvironmentId);
            return null;
        }
        LogicEnvironmentDTO dto = convertToDTO(logicEnvironment);
        log.debug("获取到逻辑环境详细信息: {} (ID: {})", dto.getName(), dto.getId());
        return dto;
    }
}
