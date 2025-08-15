package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.LogicNetwork;
import com.datacollect.service.LogicNetworkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/logic-network")
@Validated
public class LogicNetworkController {

    @Autowired
    private LogicNetworkService logicNetworkService;

    @PostMapping
    public Result<LogicNetwork> create(@Valid @RequestBody LogicNetwork logicNetwork) {
        logicNetworkService.save(logicNetwork);
        return Result.success(logicNetwork);
    }

    @PutMapping("/{id}")
    public Result<LogicNetwork> update(@PathVariable @NotNull Long id, @Valid @RequestBody LogicNetwork logicNetwork) {
        logicNetwork.setId(id);
        logicNetworkService.updateById(logicNetwork);
        return Result.success(logicNetwork);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable @NotNull Long id) {
        boolean result = logicNetworkService.removeById(id);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<LogicNetwork> getById(@PathVariable @NotNull Long id) {
        LogicNetwork logicNetwork = logicNetworkService.getById(id);
        return Result.success(logicNetwork);
    }

    @GetMapping("/page")
    public Result<Page<LogicNetwork>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {
        
        Page<LogicNetwork> page = new Page<>(current, size);
        QueryWrapper<LogicNetwork> queryWrapper = new QueryWrapper<>();
        
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        
        queryWrapper.orderByDesc("create_time");
        Page<LogicNetwork> result = logicNetworkService.page(page, queryWrapper);
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result<List<LogicNetwork>> list() {
        QueryWrapper<LogicNetwork> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("name");
        List<LogicNetwork> list = logicNetworkService.list(queryWrapper);
        return Result.success(list);
    }
}
