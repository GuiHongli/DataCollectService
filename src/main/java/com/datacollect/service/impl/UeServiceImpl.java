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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UeServiceImpl extends ServiceImpl<UeMapper, Ue> implements UeService {

    @Autowired
    private NetworkTypeService networkTypeService;

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
}
