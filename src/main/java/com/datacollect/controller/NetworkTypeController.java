package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.NetworkType;
import com.datacollect.service.NetworkTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/network-type")
@Validated
public class NetworkTypeController {

    @Autowired
    private NetworkTypeService networkTypeService;

    @PostMapping
    public Result<NetworkType> create(@Valid @RequestBody NetworkType networkType) {
        networkTypeService.save(networkType);
        return Result.success(networkType);
    }

    @PutMapping("/{id}")
    public Result<NetworkType> update(@PathVariable @NotNull Long id, @Valid @RequestBody NetworkType networkType) {
        networkType.setId(id);
        networkTypeService.updateById(networkType);
        return Result.success(networkType);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable @NotNull Long id) {
        boolean result = networkTypeService.removeById(id);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<NetworkType> getById(@PathVariable @NotNull Long id) {
        NetworkType networkType = networkTypeService.getById(id);
        return Result.success(networkType);
    }

    @GetMapping("/page")
    public Result<Page<NetworkType>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {
        
        Page<NetworkType> page = new Page<>(current, size);
        QueryWrapper<NetworkType> queryWrapper = new QueryWrapper<>();
        
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        
        queryWrapper.orderByDesc("create_time");
        Page<NetworkType> result = networkTypeService.page(page, queryWrapper);
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result<List<NetworkType>> list() {
        QueryWrapper<NetworkType> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("name");
        List<NetworkType> list = networkTypeService.list(queryWrapper);
        return Result.success(list);
    }
}
