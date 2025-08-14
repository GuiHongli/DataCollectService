package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.Region;
import com.datacollect.entity.dto.BatchRegionRequest;
import com.datacollect.entity.dto.RegionHierarchyDTO;
import com.datacollect.service.RegionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/region")
@Validated
public class RegionController {

    @Autowired
    private RegionService regionService;

    @PostMapping
    public Result<Region> create(@Valid @RequestBody Region region) {
        regionService.save(region);
        return Result.success(region);
    }

    @PostMapping("/batch")
    public Result<List<Region>> createBatch(@Valid @RequestBody BatchRegionRequest request) {
        List<Region> regions = regionService.createBatchRegions(request);
        return Result.success(regions);
    }

    @PutMapping("/{id}")
    public Result<Region> update(@PathVariable @NotNull Long id, @Valid @RequestBody Region region) {
        region.setId(id);
        regionService.updateById(region);
        return Result.success(region);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable @NotNull Long id) {
        boolean result = regionService.removeById(id);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<Region> getById(@PathVariable @NotNull Long id) {
        Region region = regionService.getById(id);
        return Result.success(region);
    }

    @GetMapping("/page")
    public Result<Page<Region>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer level) {
        
        Page<Region> page = new Page<>(current, size);
        QueryWrapper<Region> queryWrapper = new QueryWrapper<>();
        
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        if (level != null) {
            queryWrapper.eq("level", level);
        }
        
        queryWrapper.orderByAsc("level", "name");
        Page<Region> result = regionService.page(page, queryWrapper);
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result<List<Region>> list() {
        QueryWrapper<Region> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("level", "name");
        List<Region> list = regionService.list(queryWrapper);
        return Result.success(list);
    }

    @GetMapping("/level/{level}")
    public Result<List<Region>> getByLevel(@PathVariable @NotNull Integer level) {
        List<Region> list = regionService.getRegionsByLevel(level);
        return Result.success(list);
    }

    @GetMapping("/parent/{parentId}")
    public Result<List<Region>> getByParentId(@PathVariable @NotNull Long parentId) {
        List<Region> list = regionService.getRegionsByParentId(parentId);
        return Result.success(list);
    }

    @GetMapping("/tree")
    public Result<List<Region>> getTree() {
        List<Region> list = regionService.getRegionTree();
        return Result.success(list);
    }

    @GetMapping("/search")
    public Result<List<Region>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer level) {
        List<Region> list = regionService.searchRegions(name, level);
        return Result.success(list);
    }

    @GetMapping("/hierarchy")
    public Result<Page<RegionHierarchyDTO>> getHierarchy(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<RegionHierarchyDTO> page = regionService.getRegionHierarchyPage(current, size);
        return Result.success(page);
    }
}
