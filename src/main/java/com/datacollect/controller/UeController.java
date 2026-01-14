package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.Ue;
import com.datacollect.entity.dto.UeDTO;
import com.datacollect.enums.UeBrandEnum;
import com.datacollect.service.UeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ue")
@Validated
public class UeController {

    @Autowired
    private UeService ueService;

    @PostMapping
    public Result<Ue> create(@Valid @RequestBody Ue ue) {
        ueService.save(ue);
        return Result.success(ue);
    }

    @PutMapping("/{id}")
    public Result<Ue> update(@PathVariable @NotNull Long id, @Valid @RequestBody Ue ue) {
        ue.setId(id);
        ueService.updateById(ue);
        return Result.success(ue);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable @NotNull Long id) {
        boolean result = ueService.removeById(id);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<Ue> getById(@PathVariable @NotNull Long id) {
        Ue ue = ueService.getById(id);
        return Result.success(ue);
    }

    @GetMapping("/page")
    public Result<Page<UeDTO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String ueId,
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) Long networkTypeId) {
        
        Page<UeDTO> result = ueService.getUePageWithNetworkType(current, size, name, ueId, purpose, networkTypeId);
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result<List<Ue>> list() {
        QueryWrapper<Ue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("create_time");
        List<Ue> list = ueService.list(queryWrapper);
        return Result.success(list);
    }

    @GetMapping("/network-type/{networkTypeId}")
    public Result<List<Ue>> getByNetworkTypeId(@PathVariable @NotNull Long networkTypeId) {
        QueryWrapper<Ue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("network_type_id", networkTypeId);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("name");
        List<Ue> list = ueService.list(queryWrapper);
        return Result.success(list);
    }

    @GetMapping("/options")
    public Result<List<Map<String, Object>>> getUeOptions() {
        List<Map<String, Object>> options = ueService.getUeOptionsForSelect();
        return Result.success(options);
    }

    @GetMapping("/vendors")
    public Result<List<Map<String, String>>> getVendors() {
        List<Map<String, String>> vendors = new ArrayList<>();
        for (UeBrandEnum vendor : UeBrandEnum.values()) {
            Map<String, String> vendorMap = new HashMap<>();
            vendorMap.put("code", vendor.getCode());
            vendorMap.put("name", vendor.getName());
            vendors.add(vendorMap);
        }
        return Result.success(vendors);
    }

    /**
     * 释放UE（标记为可用）
     * 
     * @param id UE ID
     * @return 操作结果
     */
    @PostMapping("/{id}/release")
    public Result<Boolean> releaseUe(@PathVariable @NotNull Long id) {
        try {
            List<Integer> ueIds = new ArrayList<>();
            ueIds.add(id.intValue());
            boolean success = ueService.markUesAvailable(ueIds);
            if (success) {
                log.info("UE已释放 - UE ID: {}", id);
                return Result.success(true);
            } else {
                return Result.error("释放UE失败");
            }
        } catch (Exception e) {
            log.error("释放UE失败 - UE ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error("释放UE失败: " + e.getMessage());
        }
    }
}
