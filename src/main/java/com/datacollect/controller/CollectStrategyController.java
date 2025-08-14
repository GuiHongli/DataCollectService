package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.CollectStrategy;
import com.datacollect.service.CollectStrategyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/collect-strategy")
@Validated
public class CollectStrategyController {

    @Autowired
    private CollectStrategyService collectStrategyService;

    @PostMapping
    public Result<CollectStrategy> create(@Valid @RequestBody CollectStrategy collectStrategy) {
        collectStrategyService.save(collectStrategy);
        return Result.success(collectStrategy);
    }

    @PutMapping("/{id}")
    public Result<CollectStrategy> update(@PathVariable @NotNull Long id, @Valid @RequestBody CollectStrategy collectStrategy) {
        collectStrategy.setId(id);
        collectStrategyService.updateById(collectStrategy);
        return Result.success(collectStrategy);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable @NotNull Long id) {
        boolean result = collectStrategyService.removeById(id);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<CollectStrategy> getById(@PathVariable @NotNull Long id) {
        CollectStrategy collectStrategy = collectStrategyService.getById(id);
        return Result.success(collectStrategy);
    }

    @GetMapping("/page")
    public Result<Page<CollectStrategy>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long logicEnvironmentId) {
        
        Page<CollectStrategy> page = new Page<>(current, size);
        QueryWrapper<CollectStrategy> queryWrapper = new QueryWrapper<>();
        
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        if (logicEnvironmentId != null) {
            queryWrapper.eq("logic_environment_id", logicEnvironmentId);
        }
        
        queryWrapper.orderByDesc("create_time");
        Page<CollectStrategy> result = collectStrategyService.page(page, queryWrapper);
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result<List<CollectStrategy>> list() {
        QueryWrapper<CollectStrategy> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("create_time");
        List<CollectStrategy> list = collectStrategyService.list(queryWrapper);
        return Result.success(list);
    }

    @GetMapping("/logic-environment/{logicEnvironmentId}")
    public Result<List<CollectStrategy>> getByLogicEnvironmentId(@PathVariable @NotNull Long logicEnvironmentId) {
        QueryWrapper<CollectStrategy> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("logic_environment_id", logicEnvironmentId);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("name");
        List<CollectStrategy> list = collectStrategyService.list(queryWrapper);
        return Result.success(list);
    }
}
