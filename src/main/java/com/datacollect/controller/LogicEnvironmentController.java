package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.LogicEnvironment;
import com.datacollect.entity.LogicEnvironmentUe;
import com.datacollect.entity.dto.CreateLogicEnvironmentRequest;
import com.datacollect.service.LogicEnvironmentService;
import com.datacollect.service.LogicEnvironmentUeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/logic-environment")
@Validated
public class LogicEnvironmentController {

    @Autowired
    private LogicEnvironmentService logicEnvironmentService;

    @Autowired
    private LogicEnvironmentUeService logicEnvironmentUeService;



    @PostMapping
    public Result<LogicEnvironment> create(@Valid @RequestBody LogicEnvironment logicEnvironment) {
        logicEnvironmentService.save(logicEnvironment);
        return Result.success(logicEnvironment);
    }

    @PostMapping("/with-ue")
    public Result<LogicEnvironment> createWithUe(@Valid @RequestBody CreateLogicEnvironmentRequest request) {
        // 保存逻辑环境
        LogicEnvironment logicEnvironment = request.getLogicEnvironment();
        logicEnvironmentService.save(logicEnvironment);
        
        // 关联UE
        List<Long> ueIds = request.getUeIds();
        if (ueIds != null && !ueIds.isEmpty()) {
            for (Long ueId : ueIds) {
                LogicEnvironmentUe logicEnvironmentUe = new LogicEnvironmentUe();
                logicEnvironmentUe.setLogicEnvironmentId(logicEnvironment.getId());
                logicEnvironmentUe.setUeId(ueId);
                logicEnvironmentUeService.save(logicEnvironmentUe);
            }
        }
        
        return Result.success(logicEnvironment);
    }

    @PutMapping("/{id}")
    public Result<LogicEnvironment> update(@PathVariable @NotNull Long id, @Valid @RequestBody LogicEnvironment logicEnvironment) {
        logicEnvironment.setId(id);
        logicEnvironmentService.updateById(logicEnvironment);
        return Result.success(logicEnvironment);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable @NotNull Long id) {
        // 删除逻辑环境时，同时删除关联的UE
        QueryWrapper<LogicEnvironmentUe> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("logic_environment_id", id);
        logicEnvironmentUeService.remove(queryWrapper);
        
        boolean result = logicEnvironmentService.removeById(id);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<LogicEnvironment> getById(@PathVariable @NotNull Long id) {
        LogicEnvironment logicEnvironment = logicEnvironmentService.getById(id);
        return Result.success(logicEnvironment);
    }

    @GetMapping("/page")
    public Result<Page<LogicEnvironment>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long executorId) {
        
        Page<LogicEnvironment> page = new Page<>(current, size);
        QueryWrapper<LogicEnvironment> queryWrapper = new QueryWrapper<>();
        
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        if (executorId != null) {
            queryWrapper.eq("executor_id", executorId);
        }
        
        queryWrapper.orderByDesc("create_time");
        Page<LogicEnvironment> result = logicEnvironmentService.page(page, queryWrapper);
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result<List<LogicEnvironment>> list() {
        QueryWrapper<LogicEnvironment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("create_time");
        List<LogicEnvironment> list = logicEnvironmentService.list(queryWrapper);
        return Result.success(list);
    }

    @GetMapping("/executor/{executorId}")
    public Result<List<LogicEnvironment>> getByExecutorId(@PathVariable @NotNull Long executorId) {
        QueryWrapper<LogicEnvironment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("executor_id", executorId);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("name");
        List<LogicEnvironment> list = logicEnvironmentService.list(queryWrapper);
        return Result.success(list);
    }

    // 逻辑环境UE关联管理
    @PostMapping("/{logicEnvironmentId}/ue")
    public Result<Boolean> addUe(@PathVariable @NotNull Long logicEnvironmentId, @RequestBody List<Long> ueIds) {
        for (Long ueId : ueIds) {
            LogicEnvironmentUe logicEnvironmentUe = new LogicEnvironmentUe();
            logicEnvironmentUe.setLogicEnvironmentId(logicEnvironmentId);
            logicEnvironmentUe.setUeId(ueId);
            logicEnvironmentUeService.save(logicEnvironmentUe);
        }
        return Result.success(true);
    }

    @DeleteMapping("/{logicEnvironmentId}/ue/{ueId}")
    public Result<Boolean> removeUe(@PathVariable @NotNull Long logicEnvironmentId, @PathVariable @NotNull Long ueId) {
        QueryWrapper<LogicEnvironmentUe> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("logic_environment_id", logicEnvironmentId);
        queryWrapper.eq("ue_id", ueId);
        boolean result = logicEnvironmentUeService.remove(queryWrapper);
        return Result.success(result);
    }

    @GetMapping("/{logicEnvironmentId}/ue")
    public Result<List<LogicEnvironmentUe>> getUes(@PathVariable @NotNull Long logicEnvironmentId) {
        QueryWrapper<LogicEnvironmentUe> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("logic_environment_id", logicEnvironmentId);
        List<LogicEnvironmentUe> list = logicEnvironmentUeService.list(queryWrapper);
        return Result.success(list);
    }
}
