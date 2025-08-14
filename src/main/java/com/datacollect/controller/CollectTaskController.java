package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.CollectTask;
import com.datacollect.service.CollectTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/collect-task")
@Validated
public class CollectTaskController {

    @Autowired
    private CollectTaskService collectTaskService;

    @PostMapping
    public Result<CollectTask> create(@Valid @RequestBody CollectTask collectTask) {
        collectTaskService.save(collectTask);
        return Result.success(collectTask);
    }

    @PutMapping("/{id}")
    public Result<CollectTask> update(@PathVariable @NotNull Long id, @Valid @RequestBody CollectTask collectTask) {
        collectTask.setId(id);
        collectTaskService.updateById(collectTask);
        return Result.success(collectTask);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable @NotNull Long id) {
        boolean result = collectTaskService.removeById(id);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<CollectTask> getById(@PathVariable @NotNull Long id) {
        CollectTask collectTask = collectTaskService.getById(id);
        return Result.success(collectTask);
    }

    @GetMapping("/page")
    public Result<Page<CollectTask>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long strategyId,
            @RequestParam(required = false) Integer status) {
        
        Page<CollectTask> page = new Page<>(current, size);
        QueryWrapper<CollectTask> queryWrapper = new QueryWrapper<>();
        
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        if (strategyId != null) {
            queryWrapper.eq("strategy_id", strategyId);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByDesc("create_time");
        Page<CollectTask> result = collectTaskService.page(page, queryWrapper);
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result<List<CollectTask>> list() {
        QueryWrapper<CollectTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        List<CollectTask> list = collectTaskService.list(queryWrapper);
        return Result.success(list);
    }

    @GetMapping("/strategy/{strategyId}")
    public Result<List<CollectTask>> getByStrategyId(@PathVariable @NotNull Long strategyId) {
        QueryWrapper<CollectTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("strategy_id", strategyId);
        queryWrapper.orderByDesc("create_time");
        List<CollectTask> list = collectTaskService.list(queryWrapper);
        return Result.success(list);
    }

    @PostMapping("/{id}/start")
    public Result<Boolean> startTask(@PathVariable @NotNull Long id) {
        CollectTask collectTask = collectTaskService.getById(id);
        if (collectTask != null) {
            collectTask.setStatus(1); // 运行中
            collectTask.setLastRunTime(LocalDateTime.now());
            collectTaskService.updateById(collectTask);
            return Result.success(true);
        }
        return Result.error("任务不存在");
    }

    @PostMapping("/{id}/stop")
    public Result<Boolean> stopTask(@PathVariable @NotNull Long id) {
        CollectTask collectTask = collectTaskService.getById(id);
        if (collectTask != null) {
            collectTask.setStatus(0); // 停止
            collectTaskService.updateById(collectTask);
            return Result.success(true);
        }
        return Result.error("任务不存在");
    }

    @PostMapping("/{id}/pause")
    public Result<Boolean> pauseTask(@PathVariable @NotNull Long id) {
        CollectTask collectTask = collectTaskService.getById(id);
        if (collectTask != null) {
            collectTask.setStatus(2); // 暂停
            collectTaskService.updateById(collectTask);
            return Result.success(true);
        }
        return Result.error("任务不存在");
    }

    @GetMapping("/status/{status}")
    public Result<List<CollectTask>> getByStatus(@PathVariable @NotNull Integer status) {
        QueryWrapper<CollectTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status);
        queryWrapper.orderByDesc("create_time");
        List<CollectTask> list = collectTaskService.list(queryWrapper);
        return Result.success(list);
    }
}
