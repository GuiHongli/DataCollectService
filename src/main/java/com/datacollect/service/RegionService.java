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
}
