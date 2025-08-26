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
        Page<Ue> page = new Page<>(current, size);
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
        Page<Ue> uePage = page(page, queryWrapper);
        
        // 2. 获取所有网络类型数据
        List<NetworkType> networkTypes = networkTypeService.list();
        Map<Long, String> networkTypeMap = networkTypes.stream()
                .collect(Collectors.toMap(NetworkType::getId, NetworkType::getName));
        
        // 3. 转换为DTO
        List<UeDTO> dtoList = new ArrayList<>();
        for (Ue ue : uePage.getRecords()) {
            UeDTO dto = new UeDTO();
            dto.setId(ue.getId());
            dto.setUeId(ue.getUeId());
            dto.setName(ue.getName());
            dto.setPurpose(ue.getPurpose());
            dto.setNetworkTypeId(ue.getNetworkTypeId());
            // 处理网络类型名称，如果不存在则显示"未知网络类型"
            String networkTypeName = networkTypeMap.get(ue.getNetworkTypeId());
            dto.setNetworkTypeName(networkTypeName != null ? networkTypeName : "未知网络类型");
            dto.setBrand(ue.getBrand());
            // 处理品牌名称
            String brandName = ue.getBrand() != null ? UeBrandEnum.getNameByCode(ue.getBrand()) : null;
            dto.setBrandName(brandName);
            dto.setPort(ue.getPort());
            dto.setDescription(ue.getDescription());
            dto.setStatus(ue.getStatus());
            dto.setCreateBy(ue.getCreateBy());
            dto.setUpdateBy(ue.getUpdateBy());
            dto.setCreateTime(ue.getCreateTime());
            dto.setUpdateTime(ue.getUpdateTime());
            dtoList.add(dto);
        }
        
        // 4. 创建返回的分页对象
        Page<UeDTO> resultPage = new Page<>(current, size);
        resultPage.setRecords(dtoList);
        resultPage.setTotal(uePage.getTotal());
        
        return resultPage;
    }
    
    @Override
    public List<Map<String, Object>> getUeOptionsForSelect() {
        List<Map<String, Object>> options = new ArrayList<>();
        
        // 获取所有UE数据
        QueryWrapper<Ue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1); // 只获取启用的UE
        queryWrapper.orderByAsc("name");
        List<Ue> ues = list(queryWrapper);
        
        // 获取所有网络类型数据
        List<NetworkType> networkTypes = networkTypeService.list();
        Map<Long, String> networkTypeMap = networkTypes.stream()
                .collect(Collectors.toMap(NetworkType::getId, NetworkType::getName));
        
        // 为每个UE构建选项
        for (Ue ue : ues) {
            Map<String, Object> option = new HashMap<>();
            option.put("id", ue.getId());
            option.put("name", ue.getName());
            option.put("ueId", ue.getUeId());
            option.put("purpose", ue.getPurpose());
            option.put("networkTypeId", ue.getNetworkTypeId());
            
            // 获取网络类型名称
            String networkTypeName = networkTypeMap.get(ue.getNetworkTypeId());
            option.put("networkTypeName", networkTypeName != null ? networkTypeName : "未知网络类型");
            
            // 添加品牌和port信息
            option.put("brand", ue.getBrand());
            option.put("brandName", ue.getBrand() != null ? UeBrandEnum.getNameByCode(ue.getBrand()) : null);
            option.put("port", ue.getPort());
            
            // 构建显示名称：UE名称 + UE ID + 用途
            String displayName = String.format("%s (%s) - %s", 
                ue.getName(), 
                ue.getUeId(), 
                ue.getPurpose());
            option.put("displayName", displayName);
            
            options.add(option);
        }
        
        return options;
    }
}
