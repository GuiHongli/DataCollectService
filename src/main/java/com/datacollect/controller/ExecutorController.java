package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.Executor;
import com.datacollect.entity.dto.ExecutorDTO;
import com.datacollect.service.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/executor")
@Validated
public class ExecutorController {

    @Autowired
    private ExecutorService executorService;

    @PostMapping
    public Result<Executor> create(@Valid @RequestBody Executor executor) {
        executorService.save(executor);
        return Result.success(executor);
    }

    @PutMapping("/{id}")
    public Result<Executor> update(@PathVariable @NotNull Long id, @Valid @RequestBody Executor executor) {
        try {
            log.info("更新执行机 - ID: {}, 名称: {}, IP地址: {}", id, executor.getName(), executor.getIpAddress());
            
            // 检查IP地址是否已被其他执行机使用
            if (isIpAddressInUse(executor.getIpAddress(), id)) {
                return Result.error("IP地址 " + executor.getIpAddress() + " 已被其他执行机使用");
            }
            
            // 执行更新
            return performUpdate(id, executor);
            
        } catch (Exception e) {
            log.error("更新执行机异常 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 检查IP地址是否已被其他执行机使用
     */
    private boolean isIpAddressInUse(String ipAddress, Long excludeId) {
        QueryWrapper<Executor> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ip_address", ipAddress);
        queryWrapper.ne("id", excludeId); // 排除当前执行机
        queryWrapper.eq("deleted", 0); // 只检查未删除的记录
        Executor existingExecutor = executorService.getOne(queryWrapper);
        
        if (existingExecutor != null) {
            log.warn("IP地址 {} 已被执行机 {} (ID: {}) 使用", 
                    ipAddress, existingExecutor.getName(), existingExecutor.getId());
            return true;
        }
        
        return false;
    }

    /**
     * 执行更新操作
     */
    private Result<Executor> performUpdate(Long id, Executor executor) {
        executor.setId(id);
        boolean success = executorService.updateById(executor);
        
        if (success) {
            log.info("执行机更新成功 - ID: {}", id);
            return Result.success(executor);
        } else {
            log.error("执行机更新失败 - ID: {}", id);
            return Result.error("更新失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable @NotNull Long id) {
        boolean result = executorService.removeById(id);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<Executor> getById(@PathVariable @NotNull Long id) {
        Executor executor = executorService.getById(id);
        return Result.success(executor);
    }

    @GetMapping("/page")
    public Result<Page<ExecutorDTO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) Long regionId) {
        
        Page<ExecutorDTO> result = executorService.getExecutorPageWithRegion(current, size, name, ipAddress, regionId);
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result<List<Executor>> list() {
        QueryWrapper<Executor> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("create_time");
        List<Executor> list = executorService.list(queryWrapper);
        return Result.success(list);
    }

    @GetMapping("/region/{regionId}")
    public Result<List<Executor>> getByRegionId(@PathVariable @NotNull Long regionId) {
        QueryWrapper<Executor> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("region_id", regionId);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("name");
        List<Executor> list = executorService.list(queryWrapper);
        return Result.success(list);
    }

    @GetMapping("/region-options")
    public Result<List<Map<String, Object>>> getRegionOptions() {
        List<Map<String, Object>> options = executorService.getRegionOptionsForSelect();
        return Result.success(options);
    }

    @GetMapping("/options")
    public Result<List<Map<String, Object>>> getExecutorOptions() {
        List<Map<String, Object>> options = executorService.getExecutorOptionsForSelect();
        return Result.success(options);
    }
}
