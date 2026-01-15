package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.NetworkType;
import com.datacollect.entity.Ue;
import com.datacollect.entity.dto.UeDTO;
import com.datacollect.enums.UeBrandEnum;
import com.datacollect.mapper.UeMapper;
import com.datacollect.service.NetworkTypeService;
import com.datacollect.service.UeService;
import com.datacollect.service.CollectTaskProcessService;
import com.datacollect.service.LogicEnvironmentService;
import com.datacollect.service.LogicEnvironmentUeService;
import com.datacollect.entity.LogicEnvironment;
import com.datacollect.entity.LogicEnvironmentUe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UeServiceImpl extends ServiceImpl<UeMapper, Ue> implements UeService {

    @Autowired
    private NetworkTypeService networkTypeService;
    
    @Lazy
    @Autowired(required = false)
    private CollectTaskProcessService collectTaskProcessService;
    
    @Autowired
    private LogicEnvironmentService logicEnvironmentService;
    
    @Autowired
    private LogicEnvironmentUeService logicEnvironmentUeService;
    
    @Autowired(required = false)
    private com.datacollect.service.ConfigService configService;

    @Override
    public Page<UeDTO> getUePageWithNetworkType(Integer current, Integer size, String name, String ueId, String purpose, Long networkTypeId) {
        // 1. 获取UE分页数据
        Page<Ue> uePage = getUePage(current, size, name, ueId, purpose, networkTypeId);
        
        // 2. 获取所有网络类型数据
        Map<Long, String> networkTypeMap = buildNetworkTypeMap();
        
        // 3. 转换为DTO
        List<UeDTO> dtoList = convertToUeDTOs(uePage.getRecords(), networkTypeMap);
        
        // 4. 创建返回的分页对象
        return createResultPage(current, size, dtoList, uePage.getTotal());
    }

    private Page<Ue> getUePage(Integer current, Integer size, String name, String ueId, String purpose, Long networkTypeId) {
        Page<Ue> page = new Page<>(current, size);
        QueryWrapper<Ue> queryWrapper = buildUeQueryWrapper(name, ueId, purpose, networkTypeId);
        return page(page, queryWrapper);
    }

    private QueryWrapper<Ue> buildUeQueryWrapper(String name, String ueId, String purpose, Long networkTypeId) {
        QueryWrapper<Ue> queryWrapper = new QueryWrapper<>();
        
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        if (ueId != null && !ueId.isEmpty()) {
            queryWrapper.like("ue_id", ueId);
        }
        if (purpose != null && !purpose.isEmpty()) {
            queryWrapper.like("purpose", purpose);
        }
        if (networkTypeId != null) {
            queryWrapper.eq("network_type_id", networkTypeId);
        }
        
        queryWrapper.orderByDesc("create_time");
        return queryWrapper;
    }

    private Map<Long, String> buildNetworkTypeMap() {
        List<NetworkType> networkTypes = networkTypeService.list();
        return networkTypes.stream()
                .collect(Collectors.toMap(NetworkType::getId, NetworkType::getName));
    }

    private List<UeDTO> convertToUeDTOs(List<Ue> ues, Map<Long, String> networkTypeMap) {
        List<UeDTO> dtoList = new ArrayList<>();
        for (Ue ue : ues) {
            UeDTO dto = createUeDTO(ue, networkTypeMap);
            dtoList.add(dto);
        }
        return dtoList;
    }

    private UeDTO createUeDTO(Ue ue, Map<Long, String> networkTypeMap) {
        UeDTO dto = new UeDTO();
        dto.setId(ue.getId());
        dto.setUeId(ue.getUeId());
        dto.setName(ue.getName());
        dto.setPurpose(ue.getPurpose());
        dto.setNetworkTypeId(ue.getNetworkTypeId());
        
        setNetworkTypeName(dto, ue, networkTypeMap);
        setVendorInfo(dto, ue);
        
        dto.setPort(ue.getPort());
        dto.setDescription(ue.getDescription());
        dto.setStatus(ue.getStatus());
        dto.setInUse(ue.getInUse());
        dto.setCreateBy(ue.getCreateBy());
        dto.setUpdateBy(ue.getUpdateBy());
        dto.setCreateTime(ue.getCreateTime());
        dto.setUpdateTime(ue.getUpdateTime());
        
        return dto;
    }

    private void setNetworkTypeName(UeDTO dto, Ue ue, Map<Long, String> networkTypeMap) {
        String networkTypeName = networkTypeMap.get(ue.getNetworkTypeId());
        dto.setNetworkTypeName(networkTypeName != null ? networkTypeName : "未知网络类型");
    }

    private void setVendorInfo(UeDTO dto, Ue ue) {
        dto.setVendor(ue.getVendor());
        String vendorName = ue.getVendor() != null ? UeBrandEnum.getNameByCode(ue.getVendor()) : null;
        dto.setVendorName(vendorName);
    }

    private Page<UeDTO> createResultPage(Integer current, Integer size, List<UeDTO> dtoList, Long total) {
        Page<UeDTO> resultPage = new Page<>(current, size);
        resultPage.setRecords(dtoList);
        resultPage.setTotal(total);
        return resultPage;
    }
    
    @Override
    public List<Map<String, Object>> getUeOptionsForSelect() {
        // 获取所有UE数据
        List<Ue> ues = getEnabledUes();
        
        // 获取所有网络类型数据
        Map<Long, String> networkTypeMap = buildNetworkTypeMap();
        
        // 为每个UE构建选项
        return buildUeOptions(ues, networkTypeMap);
    }

    private List<Ue> getEnabledUes() {
        QueryWrapper<Ue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1); // 只获取启用的UE
        queryWrapper.orderByAsc("name");
        return list(queryWrapper);
    }

    private List<Map<String, Object>> buildUeOptions(List<Ue> ues, Map<Long, String> networkTypeMap) {
        List<Map<String, Object>> options = new ArrayList<>();
        
        for (Ue ue : ues) {
            Map<String, Object> option = createUeOption(ue, networkTypeMap);
            options.add(option);
        }
        
        return options;
    }

    private Map<String, Object> createUeOption(Ue ue, Map<Long, String> networkTypeMap) {
        Map<String, Object> option = new HashMap<>();
        option.put("id", ue.getId());
        option.put("name", ue.getName());
        option.put("ueId", ue.getUeId());
        option.put("purpose", ue.getPurpose());
        option.put("networkTypeId", ue.getNetworkTypeId());
        
        setNetworkTypeInfo(option, ue, networkTypeMap);
        setVendorAndPortInfo(option, ue);
        setDisplayName(option, ue);
        
        return option;
    }

    private void setNetworkTypeInfo(Map<String, Object> option, Ue ue, Map<Long, String> networkTypeMap) {
        String networkTypeName = networkTypeMap.get(ue.getNetworkTypeId());
        option.put("networkTypeName", networkTypeName != null ? networkTypeName : "未知网络类型");
    }

    private void setVendorAndPortInfo(Map<String, Object> option, Ue ue) {
        option.put("vendor", ue.getVendor());
        option.put("vendorName", ue.getVendor() != null ? UeBrandEnum.getNameByCode(ue.getVendor()) : null);
        option.put("port", ue.getPort());
    }

    private void setDisplayName(Map<String, Object> option, Ue ue) {
        String displayName = String.format("%s (%s) - %s", 
            ue.getName(), 
            ue.getUeId(), 
            ue.getPurpose());
        option.put("displayName", displayName);
    }
    
    @Override
    public boolean markUesInUse(List<Integer> ueIds) {
        if (ueIds == null || ueIds.isEmpty()) {
            return true;
        }
        
        try {
            for (Integer ueId : ueIds) {
                Ue ue = getById(ueId);
                if (ue != null) {
                    ue.setInUse(1);
                    updateById(ue);
                }
            }
            
            // 更新相关逻辑环境状态为禁用
            updateLogicEnvironmentStatusAfterUeInUse(ueIds);
            
            return true;
        } catch (Exception e) {
            log.error("标记UE为使用中失败 - UE IDs: {}, 错误: {}", ueIds, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean markUesAvailable(List<Integer> ueIds) {
        if (ueIds == null || ueIds.isEmpty()) {
            return true;
        }
        
        try {
            for (Integer ueId : ueIds) {
                Ue ue = getById(ueId);
                if (ue != null) {
                    ue.setInUse(0);
                    updateById(ue);
                }
            }
            
            // 更新相关逻辑环境状态为可用
            updateLogicEnvironmentStatusAfterUeAvailable(ueIds);
            
            // 释放UE锁并处理排队任务
            if (collectTaskProcessService != null) {
                try {
                    // 先释放UE锁
                    collectTaskProcessService.releaseUeLocks(ueIds);
                    log.info("UE锁已释放 - UE IDs: {}", ueIds);
                    
                    // 然后处理排队任务
                    collectTaskProcessService.processQueuedTasksAfterUeAvailable(ueIds);
                } catch (Exception e) {
                    log.warn("释放UE锁或处理排队任务失败 - UE IDs: {}, 错误: {}", ueIds, e.getMessage());
                }
            }
            
            return true;
        } catch (Exception e) {
            log.error("标记UE为可用失败 - UE IDs: {}, 错误: {}", ueIds, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public List<Integer> checkUesAvailability(List<Integer> ueIds) {
        List<Integer> unavailableUeIds = new ArrayList<>();
        
        if (ueIds == null || ueIds.isEmpty()) {
            return unavailableUeIds;
        }
        
        try {
            // 将Integer转换为Long（因为实体ID是Long类型）
            List<Long> longUeIds = ueIds.stream().map(Integer::longValue).collect(java.util.stream.Collectors.toList());
            List<Ue> ues = listByIds(longUeIds);
            for (Ue ue : ues) {
                if (ue == null) {
                    continue;
                }
                // 检查UE是否可用：状态为可用(1)且未使用中(0)
                if (ue.getStatus() == null || ue.getStatus() != 1 || 
                    (ue.getInUse() != null && ue.getInUse() == 1)) {
                    unavailableUeIds.add(ue.getId().intValue());
                }
            }
        } catch (Exception e) {
            log.error("检查UE可用性失败 - UE IDs: {}, 错误: {}", ueIds, e.getMessage(), e);
            // 如果检查失败，将所有UE标记为不可用，以确保安全
            unavailableUeIds.addAll(ueIds);
        }
        
        return unavailableUeIds;
    }
    
    /**
     * 当UE使用中时，更新相关逻辑环境状态为禁用
     * 根据配置决定是否禁用环境
     * 
     * @param ueIds UE ID列表
     */
    private void updateLogicEnvironmentStatusAfterUeInUse(List<Integer> ueIds) {
        if (ueIds == null || ueIds.isEmpty()) {
            return;
        }
        
        // 检查配置：如果配置为false，则不禁用环境
        boolean shouldDisableEnvironment = true; // 默认值
        if (configService != null) {
            Boolean configValue = configService.getUeDisableEnvironmentWhenInUse();
            if (configValue != null) {
                shouldDisableEnvironment = configValue;
            }
        }
        
        if (!shouldDisableEnvironment) {
            log.debug("配置为不禁用环境，跳过逻辑环境状态更新 - UE IDs: {}", ueIds);
            return;
        }
        
        try {
            // 将Integer转换为Long（因为数据库字段是Long类型）
            List<Long> longUeIds = ueIds.stream().map(Integer::longValue).collect(java.util.stream.Collectors.toList());
            
            // 查找包含这些UE的逻辑环境
            QueryWrapper<LogicEnvironmentUe> ueQuery = new QueryWrapper<>();
            ueQuery.in("ue_id", longUeIds);
            List<LogicEnvironmentUe> logicEnvironmentUes = logicEnvironmentUeService.list(ueQuery);
            
            if (logicEnvironmentUes.isEmpty()) {
                return;
            }
            
            // 获取所有相关的逻辑环境ID
            java.util.Set<Long> logicEnvironmentIds = logicEnvironmentUes.stream()
                    .map(LogicEnvironmentUe::getLogicEnvironmentId)
                    .collect(java.util.stream.Collectors.toSet());
            
            // 更新所有相关逻辑环境状态为禁用
            for (Long logicEnvironmentId : logicEnvironmentIds) {
                try {
                    LogicEnvironment logicEnvironment = logicEnvironmentService.getById(logicEnvironmentId);
                    if (logicEnvironment != null && logicEnvironment.getStatus() != null && logicEnvironment.getStatus() == 1) {
                        logicEnvironment.setStatus(0); // 0: 禁用
                        logicEnvironmentService.updateById(logicEnvironment);
                        log.info("UE使用中，逻辑环境已更新为禁用 - 逻辑环境ID: {}, UE IDs: {}", logicEnvironmentId, ueIds);
                    }
                } catch (Exception e) {
                    log.warn("更新逻辑环境状态失败 - 逻辑环境ID: {}, 错误: {}", logicEnvironmentId, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("更新逻辑环境状态失败 - UE IDs: {}, 错误: {}", ueIds, e.getMessage(), e);
        }
    }
    
    /**
     * 当UE可用后，更新相关逻辑环境状态为可用
     * 
     * @param ueIds UE ID列表
     */
    private void updateLogicEnvironmentStatusAfterUeAvailable(List<Integer> ueIds) {
        if (ueIds == null || ueIds.isEmpty()) {
            return;
        }
        
        try {
            // 将Integer转换为Long（因为数据库字段是Long类型）
            List<Long> longUeIds = ueIds.stream().map(Integer::longValue).collect(java.util.stream.Collectors.toList());
            
            // 查找包含这些UE的逻辑环境
            QueryWrapper<LogicEnvironmentUe> ueQuery = new QueryWrapper<>();
            ueQuery.in("ue_id", longUeIds);
            List<LogicEnvironmentUe> logicEnvironmentUes = logicEnvironmentUeService.list(ueQuery);
            
            if (logicEnvironmentUes.isEmpty()) {
                return;
            }
            
            // 获取所有相关的逻辑环境ID
            java.util.Set<Long> logicEnvironmentIds = logicEnvironmentUes.stream()
                    .map(LogicEnvironmentUe::getLogicEnvironmentId)
                    .collect(java.util.stream.Collectors.toSet());
            
            // 检查每个逻辑环境的所有UE是否都可用
            for (Long logicEnvironmentId : logicEnvironmentIds) {
                try {
                    // 获取该逻辑环境关联的所有UE
                    QueryWrapper<LogicEnvironmentUe> envQuery = new QueryWrapper<>();
                    envQuery.eq("logic_environment_id", logicEnvironmentId);
                    List<LogicEnvironmentUe> envUes = logicEnvironmentUeService.list(envQuery);
                    
                    if (envUes.isEmpty()) {
                        continue;
                    }
                    
                    // 获取所有UE ID
                    List<Long> allUeIds = envUes.stream()
                            .map(LogicEnvironmentUe::getUeId)
                            .collect(java.util.stream.Collectors.toList());
                    
                    // 检查所有UE是否都可用（状态为可用且未使用中）
                    List<Integer> allUeIdsInteger = allUeIds.stream().map(Long::intValue).collect(java.util.stream.Collectors.toList());
                    List<Integer> unavailableUeIds = checkUesAvailability(allUeIdsInteger);
                    
                    // 如果所有UE都可用，则更新逻辑环境状态为可用
                    if (unavailableUeIds.isEmpty()) {
                        LogicEnvironment logicEnvironment = logicEnvironmentService.getById(logicEnvironmentId);
                        if (logicEnvironment != null && logicEnvironment.getStatus() != null && logicEnvironment.getStatus() == 0) {
                            logicEnvironment.setStatus(1); // 1: 可用
                            logicEnvironmentService.updateById(logicEnvironment);
                            log.info("UE可用后，逻辑环境已更新为可用 - 逻辑环境ID: {}, UE IDs: {}", logicEnvironmentId, ueIds);
                        }
                    }
                } catch (Exception e) {
                    log.warn("更新逻辑环境状态失败 - 逻辑环境ID: {}, 错误: {}", logicEnvironmentId, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("更新逻辑环境状态失败 - UE IDs: {}, 错误: {}", ueIds, e.getMessage(), e);
        }
    }
}
