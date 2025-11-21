package com.datacollect.controller;

import com.datacollect.common.Result;
import com.datacollect.entity.ExecutorMacAddress;
import com.datacollect.service.ExecutorMacAddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/executor-mac-address")
public class ExecutorMacAddressController {

    @Autowired
    private ExecutorMacAddressService executorMacAddressService;

    /**
     * 获取所有可用的MAC地址列表（未分配给执行机的）
     */
    @GetMapping("/available")
    public Result<List<ExecutorMacAddress>> getAvailableMacAddresses() {
        List<ExecutorMacAddress> macAddresses = executorMacAddressService.getAvailableMacAddresses();
        return Result.success(macAddresses);
    }

    /**
     * 获取所有MAC地址列表
     */
    @GetMapping("/list")
    public Result<List<ExecutorMacAddress>> list() {
        List<ExecutorMacAddress> macAddresses = executorMacAddressService.list();
        return Result.success(macAddresses);
    }

    /**
     * 根据执行机ID获取MAC地址列表
     */
    @GetMapping("/executor/{executorId}")
    public Result<List<ExecutorMacAddress>> getByExecutorId(@PathVariable Long executorId) {
        List<ExecutorMacAddress> macAddresses = executorMacAddressService.getByExecutorId(executorId);
        return Result.success(macAddresses);
    }
}

