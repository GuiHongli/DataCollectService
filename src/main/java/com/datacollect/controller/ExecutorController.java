package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.Executor;
import com.datacollect.entity.ExecutorMacAddress;
import com.datacollect.entity.dto.ExecutorDTO;
import com.datacollect.dto.ExecutorRequest;
import com.datacollect.service.ExecutorService;
import com.datacollect.service.ExecutorMacAddressService;
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
    
    @Autowired
    private ExecutorMacAddressService executorMacAddressService;

    @PostMapping
    public Result<Executor> create(@Valid @RequestBody ExecutorRequest request) {
        try {
            log.info("Create executor - name: {}, IP address: {}, MAC address ID: {}", 
                    request.getName(), request.getIpAddress(), request.getMacAddressId());
            
            // 根据IP地址查找已存在的执行机（未删除的）
            QueryWrapper<Executor> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("ip_address", request.getIpAddress());
            queryWrapper.eq("deleted", 0); // 只查找未删除的记录
            Executor existingExecutor = executorService.getOne(queryWrapper);
            
            Executor executor = new Executor();
            executor.setName(request.getName());
            executor.setIpAddress(request.getIpAddress());
            executor.setMacAddress(request.getMacAddress());
            executor.setRegionId(request.getRegionId());
            executor.setDescription(request.getDescription());
            // 如果请求中没有提供status，默认设置为1（在线）
            executor.setStatus(request.getStatus() != null ? request.getStatus() : 1);
            
            if (existingExecutor != null) {
                // 如果存在，则更新（replace）
                log.info("Executor with IP {} already exists (ID: {}), performing update instead of insert", 
                        request.getIpAddress(), existingExecutor.getId());
                executor.setId(existingExecutor.getId());
                boolean success = executorService.updateById(executor);
                if (success) {
                    // 关联MAC地址
                    associateMacAddress(executor.getId(), request.getMacAddressId());
                    log.info("Executor updated successfully via IP replace - ID: {}, IP: {}", 
                            executor.getId(), request.getIpAddress());
                    return Result.success(executor);
                } else {
                    log.error("Executor update failed via IP replace - IP: {}", request.getIpAddress());
                    return Result.error("Update failed via IP replace");
                }
            } else {
                // 如果不存在，则插入
                boolean success = executorService.save(executor);
                if (success) {
                    // 关联MAC地址
                    associateMacAddress(executor.getId(), request.getMacAddressId());
                    log.info("Executor created successfully - ID: {}, IP: {}", 
                            executor.getId(), request.getIpAddress());
                    return Result.success(executor);
                } else {
                    log.error("Executor creation failed - IP: {}", request.getIpAddress());
                    return Result.error("Creation failed");
                }
            }
        } catch (Exception e) {
            log.error("Exception creating/updating executor - IP: {}, error: {}", 
                    request.getIpAddress(), e.getMessage(), e);
            return Result.error("Create/Update failed: " + e.getMessage());
        }
    }
    
    /**
     * 关联MAC地址到执行机
     */
    private void associateMacAddress(Long executorId, Long macAddressId) {
        if (macAddressId != null) {
            ExecutorMacAddress macAddress = executorMacAddressService.getById(macAddressId);
            if (macAddress != null) {
                macAddress.setExecutorId(executorId);
                executorMacAddressService.updateById(macAddress);
                log.info("MAC地址已关联到执行机 - MAC地址ID: {}, 执行机ID: {}", macAddressId, executorId);
            }
        }
    }

    @PutMapping("/{id}")
    public Result<Executor> update(@PathVariable @NotNull Long id, @Valid @RequestBody ExecutorRequest request) {
        try {
            log.info("Update executor - ID: {}, name: {}, IP address: {}, MAC address ID: {}", 
                    id, request.getName(), request.getIpAddress(), request.getMacAddressId());
            
            // 检查IP地址是否已被其他执行机使用
            if (isIpAddressInUse(request.getIpAddress(), id)) {
                return Result.error("IP address " + request.getIpAddress() + " is already used by another executor");
            }
            
            // 执行更新
            Executor executor = new Executor();
            executor.setId(id);
            executor.setName(request.getName());
            executor.setIpAddress(request.getIpAddress());
            executor.setMacAddress(request.getMacAddress());
            executor.setRegionId(request.getRegionId());
            executor.setDescription(request.getDescription());
            // 如果请求中提供了status，则更新；否则保持原值
            if (request.getStatus() != null) {
                executor.setStatus(request.getStatus());
            }
            
            Result<Executor> result = performUpdate(id, executor);
            
            // 关联MAC地址
            if (result.getCode() == 200) {
                associateMacAddress(id, request.getMacAddressId());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Exception updating executor - ID: {}, error: {}", id, e.getMessage(), e);
            return Result.error("Update failed: " + e.getMessage());
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
            log.warn("IP address {} is already used by executor {} (ID: {})", 
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
            log.info("Executor updated successfully - ID: {}", id);
            return Result.success(executor);
        } else {
            log.error("Executor update failed - ID: {}", id);
            return Result.error("Update failed");
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
