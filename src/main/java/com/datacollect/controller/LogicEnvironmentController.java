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
import com.datacollect.entity.dto.LogicEnvironmentDTO;
import com.datacollect.entity.Ue;
import com.datacollect.service.UeService;

import java.util.ArrayList;
import java.util.stream.Collectors;
import com.datacollect.entity.LogicEnvironmentNetwork;
import com.datacollect.entity.LogicNetwork;
import com.datacollect.service.LogicEnvironmentNetworkService;
import com.datacollect.service.LogicNetworkService;

@Slf4j
@RestController
@RequestMapping("/logic-environment")
@Validated
public class LogicEnvironmentController {

    @Autowired
    private LogicEnvironmentService logicEnvironmentService;

    @Autowired
    private LogicEnvironmentUeService logicEnvironmentUeService;

    @Autowired
    private UeService ueService;

    @Autowired
    private LogicEnvironmentNetworkService logicEnvironmentNetworkService;

    @Autowired
    private LogicNetworkService logicNetworkService;


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

    @PostMapping("/with-ue-and-network")
    public Result<LogicEnvironment> createWithUeAndNetwork(@Valid @RequestBody CreateLogicEnvironmentRequest request) {
        // 保存逻辑环境
        LogicEnvironment logicEnvironment = request.getLogicEnvironment();
        logicEnvironmentService.save(logicEnvironment);
        
        // 关联UE
        associateUes(logicEnvironment.getId(), request.getUeIds());
        
        // 关联逻辑组网
        associateNetworks(logicEnvironment.getId(), request.getNetworkIds());
        
        return Result.success(logicEnvironment);
    }

    @PutMapping("/{id}")
    public Result<LogicEnvironment> update(@PathVariable @NotNull Long id, @Valid @RequestBody LogicEnvironment logicEnvironment) {
        logicEnvironment.setId(id);
        logicEnvironmentService.updateById(logicEnvironment);
        return Result.success(logicEnvironment);
    }

    @PutMapping("/{id}/with-ue-and-network")
    public Result<LogicEnvironment> updateWithUeAndNetwork(@PathVariable @NotNull Long id, @Valid @RequestBody CreateLogicEnvironmentRequest request) {
        // 更新逻辑环境基本信息
        LogicEnvironment logicEnvironment = request.getLogicEnvironment();
        logicEnvironment.setId(id);
        logicEnvironmentService.updateById(logicEnvironment);
        
        // 重新关联UE
        updateUeAssociations(id, request.getUeIds());
        
        // 重新关联逻辑组网
        updateNetworkAssociations(id, request.getNetworkIds());
        
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
    public Result<Page<LogicEnvironmentDTO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long executorId) {
        
        Page<LogicEnvironmentDTO> result = logicEnvironmentService.getLogicEnvironmentPageWithDetails(current, size, name, executorId);
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
    public Result<List<LogicEnvironmentDTO.UeInfo>> getUes(@PathVariable @NotNull Long logicEnvironmentId) {
        try {
            // 获取逻辑环境关联的UE ID列表
            QueryWrapper<LogicEnvironmentUe> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("logic_environment_id", logicEnvironmentId);
            List<LogicEnvironmentUe> logicEnvironmentUes = logicEnvironmentUeService.list(queryWrapper);
            
            List<LogicEnvironmentDTO.UeInfo> ueInfoList = new ArrayList<>();
            
            if (!logicEnvironmentUes.isEmpty()) {
                List<Long> ueIds = logicEnvironmentUes.stream()
                    .map(LogicEnvironmentUe::getUeId)
                    .collect(Collectors.toList());
                
                // 获取UE详细信息
                QueryWrapper<Ue> ueQuery = new QueryWrapper<>();
                ueQuery.in("id", ueIds);
                List<Ue> ues = ueService.list(ueQuery);
                
                for (Ue ue : ues) {
                    LogicEnvironmentDTO.UeInfo ueInfo = new LogicEnvironmentDTO.UeInfo();
                    ueInfo.setId(ue.getId());
                    ueInfo.setUeId(ue.getUeId());
                    ueInfo.setName(ue.getName());
                    ueInfo.setPurpose(ue.getPurpose());
                    ueInfo.setNetworkTypeName("正常网络"); // 这里应该从网络类型表获取
                    ueInfoList.add(ueInfo);
                }
            }
            
            return Result.success(ueInfoList);
        } catch (Exception e) {
            return Result.error("获取UE信息失败: " + e.getMessage());
        }
    }

    // 逻辑环境组网关联管理
    @PostMapping("/{logicEnvironmentId}/network")
    public Result<Boolean> addNetwork(@PathVariable @NotNull Long logicEnvironmentId, @RequestBody List<Long> networkIds) {
        for (Long networkId : networkIds) {
            LogicEnvironmentNetwork logicEnvironmentNetwork = new LogicEnvironmentNetwork();
            logicEnvironmentNetwork.setLogicEnvironmentId(logicEnvironmentId);
            logicEnvironmentNetwork.setLogicNetworkId(networkId);
            logicEnvironmentNetworkService.save(logicEnvironmentNetwork);
        }
        return Result.success(true);
    }

    @DeleteMapping("/{logicEnvironmentId}/network/{networkId}")
    public Result<Boolean> removeNetwork(@PathVariable @NotNull Long logicEnvironmentId, @PathVariable @NotNull Long networkId) {
        QueryWrapper<LogicEnvironmentNetwork> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("logic_environment_id", logicEnvironmentId);
        queryWrapper.eq("logic_network_id", networkId);
        boolean result = logicEnvironmentNetworkService.remove(queryWrapper);
        return Result.success(result);
    }

    @GetMapping("/{logicEnvironmentId}/network")
    public Result<List<LogicEnvironmentDTO.NetworkInfo>> getNetworks(@PathVariable @NotNull Long logicEnvironmentId) {
        try {
            // 获取逻辑环境关联的组网ID列表
            QueryWrapper<LogicEnvironmentNetwork> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("logic_environment_id", logicEnvironmentId);
            List<LogicEnvironmentNetwork> logicEnvironmentNetworks = logicEnvironmentNetworkService.list(queryWrapper);
            
            List<LogicEnvironmentDTO.NetworkInfo> networkInfoList = new ArrayList<>();
            
            if (!logicEnvironmentNetworks.isEmpty()) {
                List<Long> networkIds = logicEnvironmentNetworks.stream()
                    .map(LogicEnvironmentNetwork::getLogicNetworkId)
                    .collect(Collectors.toList());
                
                // 获取组网详细信息
                QueryWrapper<LogicNetwork> networkQuery = new QueryWrapper<>();
                networkQuery.in("id", networkIds);
                List<LogicNetwork> networks = logicNetworkService.list(networkQuery);
                
                for (LogicNetwork network : networks) {
                    LogicEnvironmentDTO.NetworkInfo networkInfo = new LogicEnvironmentDTO.NetworkInfo();
                    networkInfo.setId(network.getId());
                    networkInfo.setName(network.getName());
                    networkInfo.setDescription(network.getDescription());
                    networkInfoList.add(networkInfo);
                }
            }
            
            return Result.success(networkInfoList);
        } catch (Exception e) {
            return Result.error("获取组网信息失败: " + e.getMessage());
        }
    }

    /**
     * 关联UE到逻辑环境
     */
    private void associateUes(Long logicEnvironmentId, List<Long> ueIds) {
        if (ueIds != null && !ueIds.isEmpty()) {
            for (Long ueId : ueIds) {
                LogicEnvironmentUe logicEnvironmentUe = new LogicEnvironmentUe();
                logicEnvironmentUe.setLogicEnvironmentId(logicEnvironmentId);
                logicEnvironmentUe.setUeId(ueId);
                logicEnvironmentUeService.save(logicEnvironmentUe);
            }
        }
    }

    /**
     * 关联网络到逻辑环境
     */
    private void associateNetworks(Long logicEnvironmentId, List<Long> networkIds) {
        if (networkIds != null && !networkIds.isEmpty()) {
            for (Long networkId : networkIds) {
                LogicEnvironmentNetwork logicEnvironmentNetwork = new LogicEnvironmentNetwork();
                logicEnvironmentNetwork.setLogicEnvironmentId(logicEnvironmentId);
                logicEnvironmentNetwork.setLogicNetworkId(networkId);
                logicEnvironmentNetworkService.save(logicEnvironmentNetwork);
            }
        }
    }

    /**
     * 更新UE关联
     */
    private void updateUeAssociations(Long logicEnvironmentId, List<Long> ueIds) {
        // 删除原有的UE关联
        QueryWrapper<LogicEnvironmentUe> ueQueryWrapper = new QueryWrapper<>();
        ueQueryWrapper.eq("logic_environment_id", logicEnvironmentId);
        logicEnvironmentUeService.remove(ueQueryWrapper);
        
        // 重新关联UE
        associateUes(logicEnvironmentId, ueIds);
    }

    /**
     * 更新网络关联
     */
    private void updateNetworkAssociations(Long logicEnvironmentId, List<Long> networkIds) {
        // 删除原有的逻辑组网关联
        QueryWrapper<LogicEnvironmentNetwork> networkQueryWrapper = new QueryWrapper<>();
        networkQueryWrapper.eq("logic_environment_id", logicEnvironmentId);
        logicEnvironmentNetworkService.remove(networkQueryWrapper);
        
        // 重新关联逻辑组网
        associateNetworks(logicEnvironmentId, networkIds);
    }
}
