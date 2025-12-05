package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.TestSettingsClientFtp;
import com.datacollect.entity.TestSettingsDeviceImsiMapping;
import com.datacollect.entity.TestSettingsNetworkFtp;
import com.datacollect.service.TestSettingsClientFtpService;
import com.datacollect.service.TestSettingsDeviceImsiMappingService;
import com.datacollect.service.TestSettingsNetworkFtpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/test-settings")
@Validated
public class TestSettingsController {

    @Autowired
    private TestSettingsClientFtpService clientFtpService;

    @Autowired
    private TestSettingsNetworkFtpService networkFtpService;

    @Autowired
    private TestSettingsDeviceImsiMappingService deviceImsiMappingService;

    // ========== 端侧FTP服务器配置 ==========
    
    @GetMapping("/client-ftp")
    public Result<TestSettingsClientFtp> getClientFtpConfig() {
        TestSettingsClientFtp config = clientFtpService.getClientFtpConfig();
        return Result.success(config);
    }

    @PostMapping("/client-ftp")
    public Result<TestSettingsClientFtp> saveOrUpdateClientFtpConfig(@Valid @RequestBody TestSettingsClientFtp config) {
        boolean success = clientFtpService.saveOrUpdateClientFtpConfig(config);
        if (success) {
            TestSettingsClientFtp saved = clientFtpService.getClientFtpConfig();
            return Result.success(saved);
        } else {
            return Result.error("保存失败");
        }
    }

    // ========== 网络侧FTP服务器配置 ==========
    
    @GetMapping("/network-ftp")
    public Result<TestSettingsNetworkFtp> getNetworkFtpConfig() {
        TestSettingsNetworkFtp config = networkFtpService.getNetworkFtpConfig();
        return Result.success(config);
    }

    @PostMapping("/network-ftp")
    public Result<TestSettingsNetworkFtp> saveOrUpdateNetworkFtpConfig(@Valid @RequestBody TestSettingsNetworkFtp config) {
        boolean success = networkFtpService.saveOrUpdateNetworkFtpConfig(config);
        if (success) {
            TestSettingsNetworkFtp saved = networkFtpService.getNetworkFtpConfig();
            return Result.success(saved);
        } else {
            return Result.error("保存失败");
        }
    }

    // ========== deviceid和IMSI对应关系 ==========
    
    @GetMapping("/device-imsi-mapping")
    public Result<List<TestSettingsDeviceImsiMapping>> getDeviceImsiMappings() {
        QueryWrapper<TestSettingsDeviceImsiMapping> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        List<TestSettingsDeviceImsiMapping> list = deviceImsiMappingService.list(queryWrapper);
        return Result.success(list);
    }

    @GetMapping("/device-imsi-mapping/page")
    public Result<Page<TestSettingsDeviceImsiMapping>> getDeviceImsiMappingPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String imsi) {
        
        Page<TestSettingsDeviceImsiMapping> page = new Page<>(current, size);
        QueryWrapper<TestSettingsDeviceImsiMapping> queryWrapper = new QueryWrapper<>();
        
        if (deviceId != null && !deviceId.isEmpty()) {
            queryWrapper.like("device_id", deviceId);
        }
        if (imsi != null && !imsi.isEmpty()) {
            queryWrapper.like("imsi", imsi);
        }
        
        queryWrapper.orderByDesc("create_time");
        Page<TestSettingsDeviceImsiMapping> result = deviceImsiMappingService.page(page, queryWrapper);
        return Result.success(result);
    }

    @PostMapping("/device-imsi-mapping")
    public Result<TestSettingsDeviceImsiMapping> createDeviceImsiMapping(@Valid @RequestBody TestSettingsDeviceImsiMapping mapping) {
        deviceImsiMappingService.save(mapping);
        return Result.success(mapping);
    }

    @PutMapping("/device-imsi-mapping/{id}")
    public Result<TestSettingsDeviceImsiMapping> updateDeviceImsiMapping(
            @PathVariable @NotNull Long id,
            @Valid @RequestBody TestSettingsDeviceImsiMapping mapping) {
        mapping.setId(id);
        deviceImsiMappingService.updateById(mapping);
        return Result.success(mapping);
    }

    @DeleteMapping("/device-imsi-mapping/{id}")
    public Result<Boolean> deleteDeviceImsiMapping(@PathVariable @NotNull Long id) {
        boolean result = deviceImsiMappingService.removeById(id);
        return Result.success(result);
    }
}




