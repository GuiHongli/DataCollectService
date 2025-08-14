package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.Region;
import com.datacollect.entity.dto.BatchRegionRequest;
import com.datacollect.entity.dto.RegionHierarchyDTO;
import com.datacollect.mapper.RegionMapper;
import com.datacollect.service.RegionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RegionServiceImpl extends ServiceImpl<RegionMapper, Region> implements RegionService {

    @Override
    public List<Region> getRegionsByLevel(Integer level) {
        QueryWrapper<Region> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("level", level);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("name");
        return list(queryWrapper);
    }

    @Override
    public List<Region> getRegionsByParentId(Long parentId) {
        QueryWrapper<Region> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("name");
        return list(queryWrapper);
    }

    @Override
    public List<Region> getRegionTree() {
        QueryWrapper<Region> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("level", "name");
        return list(queryWrapper);
    }

    @Override
    @Transactional
    public List<Region> createBatchRegions(BatchRegionRequest request) {
        List<Region> regions = new ArrayList<>();
        
        // 1. 创建或获取地域（片区）
        Region region = findOrCreateRegion(request.getRegionName(), 1, null, request.getDescription());
        regions.add(region);
        
        // 2. 创建或获取国家/省份（level=2）
        Region country = findOrCreateRegion(request.getCountryName(), 2, region.getId(), request.getDescription());
        regions.add(country);
        
        // 3. 创建或获取省份/城市（level=3）
        if (request.getProvinceName() != null && !request.getProvinceName().trim().isEmpty()) {
            Region province = findOrCreateRegion(request.getProvinceName(), 3, country.getId(), request.getDescription());
            regions.add(province);
            
            // 4. 创建或获取城市（level=4）
            if (request.getCityName() != null && !request.getCityName().trim().isEmpty()) {
                Region city = findOrCreateRegion(request.getCityName(), 4, province.getId(), request.getDescription());
                regions.add(city);
            }
        } else if (request.getCityName() != null && !request.getCityName().trim().isEmpty()) {
            // 如果只有城市，直接作为level=4创建，父级是国家
            Region city = findOrCreateRegion(request.getCityName(), 4, country.getId(), request.getDescription());
            regions.add(city);
        }
        
        return regions;
    }

    @Override
    public List<Region> searchRegions(String name, Integer level) {
        QueryWrapper<Region> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        
        if (level != null) {
            queryWrapper.eq("level", level);
        }
        
        queryWrapper.orderByAsc("level", "name");
        return list(queryWrapper);
    }

    @Override
    public List<RegionHierarchyDTO> getRegionHierarchy() {
        // 获取所有地域数据
        QueryWrapper<Region> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("level", "name");
        List<Region> allRegions = list(queryWrapper);
        
        // 按层级分组
        Map<Integer, List<Region>> regionsByLevel = new HashMap<>();
        for (Region region : allRegions) {
            regionsByLevel.computeIfAbsent(region.getLevel(), k -> new ArrayList<>()).add(region);
        }
        
        List<RegionHierarchyDTO> result = new ArrayList<>();
        
        // 处理level=1的地域（片区）
        List<Region> level1Regions = regionsByLevel.get(1);
        if (level1Regions != null) {
            for (Region region1 : level1Regions) {
                // 处理level=2的地域（国家/省份）
                List<Region> level2Regions = regionsByLevel.get(2);
                if (level2Regions != null) {
                    for (Region region2 : level2Regions) {
                        if (region2.getParentId().equals(region1.getId())) {
                            // 处理level=3的地域（省份/城市）
                            List<Region> level3Regions = regionsByLevel.get(3);
                            if (level3Regions != null) {
                                for (Region region3 : level3Regions) {
                                    if (region3.getParentId().equals(region2.getId())) {
                                        // 处理level=4的地域（城市）
                                        List<Region> level4Regions = regionsByLevel.get(4);
                                        if (level4Regions != null) {
                                            for (Region region4 : level4Regions) {
                                                if (region4.getParentId().equals(region3.getId())) {
                                                    // 完整的4级层级
                                                    RegionHierarchyDTO dto = new RegionHierarchyDTO();
                                                    dto.setId(region4.getId());
                                                    dto.setRegionName(region1.getName());
                                                    dto.setCountryName(region2.getName());
                                                    dto.setProvinceName(region3.getName());
                                                    dto.setCityName(region4.getName());
                                                    dto.setStatus(region4.getStatus());
                                                    dto.setCreateTime(region4.getCreateTime().toString());
                                                    result.add(dto);
                                                }
                                            }
                                        }
                                        
                                        // 只有3级层级（没有城市）
                                        if (level4Regions == null || level4Regions.stream().noneMatch(r -> r.getParentId().equals(region3.getId()))) {
                                            RegionHierarchyDTO dto = new RegionHierarchyDTO();
                                            dto.setId(region3.getId());
                                            dto.setRegionName(region1.getName());
                                            dto.setCountryName(region2.getName());
                                            dto.setProvinceName(region3.getName());
                                            dto.setCityName("");
                                            dto.setStatus(region3.getStatus());
                                            dto.setCreateTime(region3.getCreateTime().toString());
                                            result.add(dto);
                                        }
                                    }
                                }
                            }
                            
                            // 只有2级层级（没有省份）
                            if (level3Regions == null || level3Regions.stream().noneMatch(r -> r.getParentId().equals(region2.getId()))) {
                                // 检查是否有直接的城市
                                List<Region> level4Regions = regionsByLevel.get(4);
                                if (level4Regions != null) {
                                    for (Region region4 : level4Regions) {
                                        if (region4.getParentId().equals(region2.getId())) {
                                            RegionHierarchyDTO dto = new RegionHierarchyDTO();
                                            dto.setId(region4.getId());
                                            dto.setRegionName(region1.getName());
                                            dto.setCountryName(region2.getName());
                                            dto.setProvinceName("");
                                            dto.setCityName(region4.getName());
                                            dto.setStatus(region4.getStatus());
                                            dto.setCreateTime(region4.getCreateTime().toString());
                                            result.add(dto);
                                        }
                                    }
                                }
                                
                                // 只有2级层级（没有省份和城市）
                                if (level4Regions == null || level4Regions.stream().noneMatch(r -> r.getParentId().equals(region2.getId()))) {
                                    RegionHierarchyDTO dto = new RegionHierarchyDTO();
                                    dto.setId(region2.getId());
                                    dto.setRegionName(region1.getName());
                                    dto.setCountryName(region2.getName());
                                    dto.setProvinceName("");
                                    dto.setCityName("");
                                    dto.setStatus(region2.getStatus());
                                    dto.setCreateTime(region2.getCreateTime().toString());
                                    result.add(dto);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * 查找或创建地域
     */
    private Region findOrCreateRegion(String name, Integer level, Long parentId, String description) {
        QueryWrapper<Region> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name);
        queryWrapper.eq("level", level);
        if (parentId != null) {
            queryWrapper.eq("parent_id", parentId);
        } else {
            queryWrapper.isNull("parent_id");
        }
        
        Region existingRegion = getOne(queryWrapper);
        if (existingRegion != null) {
            return existingRegion;
        }
        
        // 创建新地域
        Region newRegion = new Region();
        newRegion.setName(name);
        newRegion.setLevel(level);
        newRegion.setParentId(parentId);
        newRegion.setDescription(description);
        newRegion.setStatus(1);
        
        save(newRegion);
        return newRegion;
    }

    @Override
    public Page<RegionHierarchyDTO> getRegionHierarchyPage(Integer current, Integer size) {
        // 获取所有层级化数据
        List<RegionHierarchyDTO> allData = getRegionHierarchy();
        
        // 手动分页
        int total = allData.size();
        int startIndex = (current - 1) * size;
        int endIndex = Math.min(startIndex + size, total);
        
        List<RegionHierarchyDTO> pageData = new ArrayList<>();
        if (startIndex < total) {
            pageData = allData.subList(startIndex, endIndex);
        }
        
        // 创建分页对象
        Page<RegionHierarchyDTO> page = new Page<>(current, size);
        page.setRecords(pageData);
        page.setTotal(total);
        
        return page;
    }
}
