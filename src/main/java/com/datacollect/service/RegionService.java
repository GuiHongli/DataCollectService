package com.datacollect.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.Region;
import com.datacollect.entity.dto.BatchRegionRequest;
import com.datacollect.entity.dto.RegionHierarchyDTO;

import java.util.List;

public interface RegionService extends IService<Region> {
    
    /**
     * 根据层级获取地域列表
     */
    List<Region> getRegionsByLevel(Integer level);
    
    /**
     * 根据父级ID获取子地域列表
     */
    List<Region> getRegionsByParentId(Long parentId);
    
    /**
     * 获取地域树形结构
     */
    List<Region> getRegionTree();
    
    /**
     * 批量创建地域（地域、国家、省份、城市）
     */
    List<Region> createBatchRegions(BatchRegionRequest request);
    
    /**
     * 搜索地域
     */
    List<Region> searchRegions(String name, Integer level);
    
    /**
     * 获取层级化的地域数据
     */
    List<RegionHierarchyDTO> getRegionHierarchy();
    
    /**
     * 获取层级化的地域数据（分页）
     */
    Page<RegionHierarchyDTO> getRegionHierarchyPage(Integer current, Integer size);
    
    /**
     * 根据地域、国家、省份、城市信息查找或创建地域，返回最终的地域ID
     * 优先级：城市ID > 省份ID > 国家ID > 地域ID
     * 
     * @param regionName 地域名称（片区，level=1）
     * @param countryName 国家名称（level=2）
     * @param provinceName 省份名称（level=3）
     * @param cityName 城市名称（level=4）
     * @param description 描述信息
     * @return 最终的地域ID（如果提供了城市则返回城市ID，否则返回省份ID，以此类推）
     */
    Long findOrCreateRegionHierarchy(String regionName, String countryName, String provinceName, String cityName, String description);
}
