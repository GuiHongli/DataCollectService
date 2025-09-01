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
        List<Region> allRegions = getAllRegions();
        
        // 按层级分组
        Map<Integer, List<Region>> regionsByLevel = groupRegionsByLevel(allRegions);
        
        // 构建层级结构
        return buildHierarchyStructure(regionsByLevel);
    }

    /**
     * 获取所有地域数据
     */
    private List<Region> getAllRegions() {
        QueryWrapper<Region> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("level", "name");
        return list(queryWrapper);
    }

    /**
     * 按层级分组地域
     */
    private Map<Integer, List<Region>> groupRegionsByLevel(List<Region> allRegions) {
        Map<Integer, List<Region>> regionsByLevel = new HashMap<>();
        for (Region region : allRegions) {
            regionsByLevel.computeIfAbsent(region.getLevel(), k -> new ArrayList<>()).add(region);
        }
        return regionsByLevel;
    }

    /**
     * 构建层级结构
     */
    private List<RegionHierarchyDTO> buildHierarchyStructure(Map<Integer, List<Region>> regionsByLevel) {
        List<RegionHierarchyDTO> result = new ArrayList<>();
        
        // 处理level=1的地域（片区）
        List<Region> level1Regions = regionsByLevel.get(1);
        if (level1Regions != null) {
            for (Region region1 : level1Regions) {
                processLevel1Region(region1, regionsByLevel, result);
            }
        }
        
        return result;
    }

    /**
     * 处理level=1的地域
     */
    private void processLevel1Region(Region region1, Map<Integer, List<Region>> regionsByLevel, List<RegionHierarchyDTO> result) {
        // 处理level=2的地域（国家/省份）
        List<Region> level2Regions = regionsByLevel.get(2);
        if (level2Regions != null) {
            for (Region region2 : level2Regions) {
                if (region2.getParentId().equals(region1.getId())) {
                    processLevel2Region(region1, region2, regionsByLevel, result);
                }
            }
        }
    }

    /**
     * 处理level=2的地域
     */
    private void processLevel2Region(Region region1, Region region2, Map<Integer, List<Region>> regionsByLevel, List<RegionHierarchyDTO> result) {
        // 处理level=3的地域（省份/城市）
        List<Region> level3Regions = regionsByLevel.get(3);
        if (level3Regions != null) {
            for (Region region3 : level3Regions) {
                if (region3.getParentId().equals(region2.getId())) {
                    processLevel3Region(region1, region2, region3, regionsByLevel, result);
                }
            }
        }
        
        // 只有2级层级（没有省份）
        if (level3Regions == null || level3Regions.stream().noneMatch(r -> r.getParentId().equals(region2.getId()))) {
            processLevel2OnlyRegion(region1, region2, regionsByLevel, result);
        }
    }

    /**
     * 处理level=3的地域
     */
    private void processLevel3Region(Region region1, Region region2, Region region3, Map<Integer, List<Region>> regionsByLevel, List<RegionHierarchyDTO> result) {
        // 处理level=4的地域（城市）
        List<Region> level4Regions = regionsByLevel.get(4);
        if (level4Regions != null) {
            for (Region region4 : level4Regions) {
                if (region4.getParentId().equals(region3.getId())) {
                    // 完整的4级层级
                    RegionHierarchyDTO dto = createHierarchyDTO(region4.getId(), region1.getName(), region2.getName(), region3.getName(), region4.getName(), region4.getStatus(), region4.getCreateTime().toString());
                    result.add(dto);
                }
            }
        }
        
        // 只有3级层级（没有城市）
        if (level4Regions == null || level4Regions.stream().noneMatch(r -> r.getParentId().equals(region3.getId()))) {
            RegionHierarchyDTO dto = createHierarchyDTO(region3.getId(), region1.getName(), region2.getName(), region3.getName(), "", region3.getStatus(), region3.getCreateTime().toString());
            result.add(dto);
        }
    }

    /**
     * 处理只有2级的地域
     */
    private void processLevel2OnlyRegion(Region region1, Region region2, Map<Integer, List<Region>> regionsByLevel, List<RegionHierarchyDTO> result) {
        // 检查是否有直接的城市
        List<Region> level4Regions = regionsByLevel.get(4);
        if (level4Regions != null) {
            for (Region region4 : level4Regions) {
                if (region4.getParentId().equals(region2.getId())) {
                    RegionHierarchyDTO dto = createHierarchyDTO(region4.getId(), region1.getName(), region2.getName(), "", region4.getName(), region4.getStatus(), region4.getCreateTime().toString());
                    result.add(dto);
                }
            }
        }
        
        // 只有2级层级（没有省份和城市）
        if (level4Regions == null || level4Regions.stream().noneMatch(r -> r.getParentId().equals(region2.getId()))) {
            RegionHierarchyDTO dto = createHierarchyDTO(region2.getId(), region1.getName(), region2.getName(), "", "", region2.getStatus(), region2.getCreateTime().toString());
            result.add(dto);
        }
    }

    /**
     * 创建层级DTO
     */
    private RegionHierarchyDTO createHierarchyDTO(Long id, String regionName, String countryName, String provinceName, String cityName, Integer status, String createTime) {
        RegionHierarchyDTO dto = new RegionHierarchyDTO();
        dto.setId(id);
        dto.setRegionName(regionName);
        dto.setCountryName(countryName);
        dto.setProvinceName(provinceName);
        dto.setCityName(cityName);
        dto.setStatus(status);
        dto.setCreateTime(createTime);
        return dto;
    }

    /**
     * 查找或创建地域
     */
    private Region findOrCreateRegion(String name, Integer level, Long parentId, String description) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        
        // 查找是否已存在
        QueryWrapper<Region> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name.trim());
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
        
        // 创建新的地域
        Region newRegion = new Region();
        newRegion.setName(name.trim());
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
