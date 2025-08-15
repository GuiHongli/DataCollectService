package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.Executor;
import com.datacollect.entity.Region;
import com.datacollect.entity.dto.ExecutorDTO;
import com.datacollect.mapper.ExecutorMapper;
import com.datacollect.service.ExecutorService;
import com.datacollect.service.RegionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExecutorServiceImpl extends ServiceImpl<ExecutorMapper, Executor> implements ExecutorService {

    @Autowired
    private RegionService regionService;

    @Override
    public Page<ExecutorDTO> getExecutorPageWithRegion(Integer current, Integer size, String name, String ipAddress, Long regionId) {
        // 1. 获取执行机分页数据
        Page<Executor> page = new Page<>(current, size);
        QueryWrapper<Executor> queryWrapper = new QueryWrapper<>();
        
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        if (ipAddress != null && !ipAddress.isEmpty()) {
            queryWrapper.like("ip_address", ipAddress);
        }
        if (regionId != null) {
            queryWrapper.eq("region_id", regionId);
        }
        
        queryWrapper.orderByDesc("create_time");
        Page<Executor> executorPage = page(page, queryWrapper);
        
        // 2. 获取所有地域数据并建立层级映射
        List<Region> allRegions = regionService.list();
        Map<Long, Region> regionMap = new HashMap<>();
        for (Region region : allRegions) {
            regionMap.put(region.getId(), region);
        }
        
        // 3. 转换为DTO，构建完整的地域路径
        List<ExecutorDTO> dtoList = new ArrayList<>();
        for (Executor executor : executorPage.getRecords()) {
            ExecutorDTO dto = new ExecutorDTO();
            dto.setId(executor.getId());
            dto.setIpAddress(executor.getIpAddress());
            dto.setName(executor.getName());
            dto.setRegionId(executor.getRegionId());
            dto.setDescription(executor.getDescription());
            dto.setStatus(executor.getStatus());
            dto.setCreateBy(executor.getCreateBy());
            dto.setUpdateBy(executor.getUpdateBy());
            dto.setCreateTime(executor.getCreateTime());
            dto.setUpdateTime(executor.getUpdateTime());
            
            // 构建完整的地域路径
            String regionPath = buildRegionPath(executor.getRegionId(), regionMap);
            dto.setRegionName(regionPath);
            
            dtoList.add(dto);
        }
        
        // 4. 创建返回的分页对象
        Page<ExecutorDTO> resultPage = new Page<>(current, size);
        resultPage.setRecords(dtoList);
        resultPage.setTotal(executorPage.getTotal());
        
        return resultPage;
    }
    
    /**
     * 构建完整的地域路径（地域+国家+省份+城市）
     */
    private String buildRegionPath(Long regionId, Map<Long, Region> regionMap) {
        if (regionId == null) {
            return "未知地域";
        }
        
        Region currentRegion = regionMap.get(regionId);
        if (currentRegion == null) {
            return "未知地域";
        }
        
        List<String> pathParts = new ArrayList<>();
        pathParts.add(currentRegion.getName());
        
        // 向上查找父级地域，构建完整路径
        Long parentId = currentRegion.getParentId();
        while (parentId != null) {
            Region parentRegion = regionMap.get(parentId);
            if (parentRegion != null) {
                pathParts.add(0, parentRegion.getName()); // 在开头插入父级名称
                parentId = parentRegion.getParentId();
            } else {
                break;
            }
        }
        
        // 根据层级添加标签
        String path = String.join(" / ", pathParts);
        if (currentRegion.getLevel() == 1) {
            return path + " (片区)";
        } else if (currentRegion.getLevel() == 2) {
            return path + " (国家/直辖市)";
        } else if (currentRegion.getLevel() == 3) {
            return path + " (省份)";
        } else if (currentRegion.getLevel() == 4) {
            return path + " (城市)";
        }
        
        return path;
    }
    
    @Override
    public List<Map<String, Object>> getRegionOptionsForSelect() {
        List<Map<String, Object>> options = new ArrayList<>();
        
        // 获取所有地域数据
        List<Region> allRegions = regionService.list();
        Map<Long, Region> regionMap = new HashMap<>();
        for (Region region : allRegions) {
            regionMap.put(region.getId(), region);
        }
        
        // 只显示城市级别（level=4）的地域
        for (Region region : allRegions) {
            if (region.getLevel() == 4) {
                Map<String, Object> option = new HashMap<>();
                option.put("id", region.getId());
                option.put("name", region.getName());
                option.put("level", region.getLevel());
                
                // 构建完整的地域路径
                String regionPath = buildRegionPath(region.getId(), regionMap);
                option.put("fullPath", regionPath);
                
                options.add(option);
            }
        }
        
        // 按名称排序
        options.sort((a, b) -> {
            String nameA = (String) a.get("name");
            String nameB = (String) b.get("name");
            return nameA.compareTo(nameB);
        });
        
        return options;
    }
    
    @Override
    public List<Map<String, Object>> getExecutorOptionsForSelect() {
        List<Map<String, Object>> options = new ArrayList<>();
        
        // 获取所有执行机数据
        QueryWrapper<Executor> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1); // 只获取启用的执行机
        queryWrapper.orderByAsc("name");
        List<Executor> executors = list(queryWrapper);
        
        // 获取所有地域数据并建立层级映射
        List<Region> allRegions = regionService.list();
        Map<Long, Region> regionMap = new HashMap<>();
        for (Region region : allRegions) {
            regionMap.put(region.getId(), region);
        }
        
        // 为每个执行机构建选项
        for (Executor executor : executors) {
            Map<String, Object> option = new HashMap<>();
            option.put("id", executor.getId());
            option.put("name", executor.getName());
            option.put("ipAddress", executor.getIpAddress());
            option.put("regionId", executor.getRegionId());
            
            // 构建完整的地域路径
            String regionPath = buildRegionPath(executor.getRegionId(), regionMap);
            option.put("regionName", regionPath);
            
            // 构建显示名称：执行机名称 + IP + 所属地域
            String displayName = String.format("%s (%s) - %s", 
                executor.getName(), 
                executor.getIpAddress(), 
                regionPath);
            option.put("displayName", displayName);
            
            options.add(option);
        }
        
        return options;
    }
    
    @Override
    public List<Executor> getExecutorsByRegion(Long regionId, Long countryId, Long provinceId, Long cityId) {
        log.info("开始根据地域条件获取执行机 - regionId={}, countryId={}, provinceId={}, cityId={}", 
                regionId, countryId, provinceId, cityId);
        
        QueryWrapper<Executor> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1); // 只获取在线的执行机
        
        // 根据筛选条件构建查询
        if (cityId != null) {
            // 如果指定了城市，直接查询该城市的执行机
            log.info("按城市筛选执行机 - cityId: {}", cityId);
            queryWrapper.eq("region_id", cityId);
        } else if (provinceId != null) {
            // 如果指定了省份，查询该省份下所有城市的执行机
            log.info("按省份筛选执行机 - provinceId: {}", provinceId);
            List<Region> cities = regionService.getRegionsByParentId(provinceId);
            List<Long> cityIds = cities.stream().map(Region::getId).collect(Collectors.toList());
            log.debug("省份 {} 下的城市数量: {}, 城市ID列表: {}", provinceId, cityIds.size(), cityIds);
            if (!cityIds.isEmpty()) {
                queryWrapper.in("region_id", cityIds);
            }
        } else if (countryId != null) {
            // 如果指定了国家，查询该国家下所有省份的执行机
            log.info("按国家筛选执行机 - countryId: {}", countryId);
            List<Region> provinces = regionService.getRegionsByParentId(countryId);
            List<Long> provinceIds = provinces.stream().map(Region::getId).collect(Collectors.toList());
            log.debug("国家 {} 下的省份数量: {}, 省份ID列表: {}", countryId, provinceIds.size(), provinceIds);
            if (!provinceIds.isEmpty()) {
                List<Long> allCityIds = new ArrayList<>();
                for (Long pid : provinceIds) {
                    List<Region> cities = regionService.getRegionsByParentId(pid);
                    List<Long> cityIds = cities.stream().map(Region::getId).collect(Collectors.toList());
                    log.debug("省份 {} 下的城市数量: {}, 城市ID列表: {}", pid, cityIds.size(), cityIds);
                    allCityIds.addAll(cityIds);
                }
                log.debug("国家 {} 下所有城市数量: {}, 城市ID列表: {}", countryId, allCityIds.size(), allCityIds);
                if (!allCityIds.isEmpty()) {
                    queryWrapper.in("region_id", allCityIds);
                }
            }
        } else if (regionId != null) {
            // 如果指定了地域，查询该地域下所有国家的执行机
            log.info("按地域筛选执行机 - regionId: {}", regionId);
            List<Region> countries = regionService.getRegionsByParentId(regionId);
            List<Long> countryIds = countries.stream().map(Region::getId).collect(Collectors.toList());
            log.debug("地域 {} 下的国家数量: {}, 国家ID列表: {}", regionId, countryIds.size(), countryIds);
            if (!countryIds.isEmpty()) {
                List<Long> allCityIds = new ArrayList<>();
                for (Long cid : countryIds) {
                    List<Region> provinces = regionService.getRegionsByParentId(cid);
                    log.debug("国家 {} 下的省份数量: {}", cid, provinces.size());
                    for (Region province : provinces) {
                        List<Region> cities = regionService.getRegionsByParentId(province.getId());
                        List<Long> cityIds = cities.stream().map(Region::getId).collect(Collectors.toList());
                        log.debug("省份 {} 下的城市数量: {}, 城市ID列表: {}", province.getId(), cityIds.size(), cityIds);
                        allCityIds.addAll(cityIds);
                    }
                }
                log.debug("地域 {} 下所有城市数量: {}, 城市ID列表: {}", regionId, allCityIds.size(), allCityIds);
                if (!allCityIds.isEmpty()) {
                    queryWrapper.in("region_id", allCityIds);
                }
            }
        } else {
            log.info("未指定地域筛选条件，将查询所有执行机");
        }
        
        queryWrapper.orderByAsc("name");
        List<Executor> executors = list(queryWrapper);
        log.info("根据地域条件获取到执行机数量: {}", executors.size());
        for (Executor executor : executors) {
            log.debug("匹配的执行机: {} (ID: {}, IP: {}, 地域ID: {})", 
                    executor.getName(), executor.getId(), executor.getIpAddress(), executor.getRegionId());
        }
        
        return executors;
    }
}
