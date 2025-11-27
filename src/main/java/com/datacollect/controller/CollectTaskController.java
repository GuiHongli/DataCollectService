package com.datacollect.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.dto.CollectTaskRequest;
import com.datacollect.entity.CollectStrategy;
import com.datacollect.entity.CollectTask;
import com.datacollect.entity.Executor;
import com.datacollect.entity.ExecutorMacAddress;
import com.datacollect.entity.LogicEnvironment;
import com.datacollect.entity.LogicEnvironmentNetwork;
import com.datacollect.entity.NetworkType;
import com.datacollect.entity.TestCase;
import com.datacollect.entity.TestCaseExecutionInstance;
import com.datacollect.entity.dto.LogicEnvironmentDTO;
import com.datacollect.service.CaseExecuteServiceClient;
import com.datacollect.service.CollectStrategyService;
import com.datacollect.service.CollectTaskProcessService;
import com.datacollect.service.CollectTaskService;
import com.datacollect.service.ExecutorService;
import com.datacollect.service.ExecutorMacAddressService;
import com.datacollect.service.LogicEnvironmentNetworkService;
import com.datacollect.service.LogicEnvironmentService;
import com.datacollect.service.NetworkTypeService;
import com.datacollect.service.TestCaseExecutionInstanceService;
import com.datacollect.service.TestCaseService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/collect-task")
@Validated
public class CollectTaskController {

    @Autowired
    private CollectTaskService collectTaskService;

    @Autowired
    private CollectStrategyService collectStrategyService;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private ExecutorMacAddressService executorMacAddressService;

    @Autowired
    private LogicEnvironmentService logicEnvironmentService;

    @Autowired
    private LogicEnvironmentNetworkService logicEnvironmentNetworkService;

    @Autowired
    private NetworkTypeService networkTypeService;

    @Autowired
    private CollectTaskProcessService collectTaskProcessService;

    @Autowired
    private TestCaseExecutionInstanceService testCaseExecutionInstanceService;
    
    @Autowired
    private CaseExecuteServiceClient caseExecuteServiceClient;
    
    @Autowired
    private com.datacollect.service.ExecutorWebSocketService executorWebSocketService;

    @PostMapping
    public Result<CollectTask> create(@Valid @RequestBody CollectTask collectTask) {
        collectTaskService.save(collectTask);
        return Result.success(collectTask);
    }

    /**
     * 创建采集任务（新接口）
     * 
     * @param request 采集任务请求
     * @param httpRequest HTTP请求对象，用于获取当前用户信息
     * @return 采集任务ID
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createCollectTask(@Valid @RequestBody CollectTaskRequest request, HttpServletRequest httpRequest) {
        log.info("Received create collect task request - task name: {}, collect strategy ID: {}", request.getName(), request.getCollectStrategyId());
        
        try {
            // 从请求中获取当前用户名（下发人）
            String createBy = (String) httpRequest.getAttribute("username");
            log.info("Create collect task - createBy: {}", createBy);
            
            // 调用处理服务创建采集任务
            Long collectTaskId = collectTaskProcessService.processCollectTaskCreation(request, createBy);
            
            Map<String, Object> result = createSuccessResult(collectTaskId);
            
            log.info("Collect task created successfully - task ID: {}, createBy: {}", collectTaskId, createBy);
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("Failed to create collect task - task name: {}, error: {}", request.getName(), e.getMessage(), e);
            return Result.error("Failed to create collect task: " + e.getMessage());
        }
    }

    /**
     * 创建成功结果
     */
    private Map<String, Object> createSuccessResult(Long collectTaskId) {
        Map<String, Object> result = new HashMap<>();
        result.put("collectTaskId", collectTaskId);
        result.put("message", "Collect task created successfully");
        result.put("timestamp", System.currentTimeMillis());
        return result;
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
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {
        
        Page<CollectTask> page = new Page<>(current, size);
        QueryWrapper<CollectTask> queryWrapper = buildPageQueryWrapper(name, strategyId, status, httpRequest);
        Page<CollectTask> result = collectTaskService.page(page, queryWrapper);
        return Result.success(result);
    }

    /**
     * 构建分页查询条件
     */
    private QueryWrapper<CollectTask> buildPageQueryWrapper(String name, Long strategyId, String status, HttpServletRequest httpRequest) {
        QueryWrapper<CollectTask> queryWrapper = new QueryWrapper<>();
        
        // 获取当前用户信息
        String role = (String) httpRequest.getAttribute("role");
        String username = (String) httpRequest.getAttribute("username");
        
        // 根据用户角色过滤数据
        // admin 可以查看全部任务，普通用户只能查看自己下发的任务
        if (role != null && !"admin".equals(role) && username != null) {
            queryWrapper.eq("create_by", username);
            log.debug("普通用户查询采集任务 - 用户名: {}, 只能查看自己下发的任务", username);
        } else {
            log.debug("管理员查询采集任务 - 角色: {}, 可以查看全部任务", role);
        }
        
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        if (strategyId != null) {
            queryWrapper.eq("collect_strategy_id", strategyId);
        }
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByDesc("create_time");
        return queryWrapper;
    }

    @GetMapping("/list")
    public Result<List<CollectTask>> list(HttpServletRequest httpRequest) {
        QueryWrapper<CollectTask> queryWrapper = new QueryWrapper<>();
        
        // 获取当前用户信息
        String role = (String) httpRequest.getAttribute("role");
        String username = (String) httpRequest.getAttribute("username");
        
        // 根据用户角色过滤数据
        // admin 可以查看全部任务，普通用户只能查看自己下发的任务
        if (role != null && !"admin".equals(role) && username != null) {
            queryWrapper.eq("create_by", username);
            log.debug("普通用户查询采集任务列表 - 用户名: {}, 只能查看自己下发的任务", username);
        } else {
            log.debug("管理员查询采集任务列表 - 角色: {}, 可以查看全部任务", role);
        }
        
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
        log.info("Start task - task ID: {}", id);
        
        try {
            CollectTask collectTask = validateAndGetTask(id);
            if (collectTask == null) {
                return Result.error("Task not found");
            }
            
            // 更新任务状态
            boolean taskUpdated = collectTaskService.updateTaskStatus(id, "RUNNING");
            if (!taskUpdated) {
                return Result.error("Task status update failed");
            }
            
            // 更新所有PENDING状态的用例执行例次为RUNNING
            updatePendingInstancesToRunning(id);
            
            log.info("Task started successfully - task ID: {}", id);
            return Result.success(true);
            
        } catch (Exception e) {
            log.error("Failed to start task - task ID: {}, error: {}", id, e.getMessage(), e);
            return Result.error("Failed to start task: " + e.getMessage());
        }
    }

    /**
     * 验证并获取任务
     */
    private CollectTask validateAndGetTask(Long id) {
        return collectTaskService.getById(id);
    }

    /**
     * 更新待执行实例为运行状态
     */
    private void updatePendingInstancesToRunning(Long taskId) {
        List<TestCaseExecutionInstance> pendingInstances = testCaseExecutionInstanceService.getByCollectTaskIdAndStatus(taskId, "PENDING");
        for (TestCaseExecutionInstance instance : pendingInstances) {
            testCaseExecutionInstanceService.updateExecutionStatus(instance.getId(), "RUNNING", null);
        }
        log.info("Task started successfully - task ID: {}, updated instance count: {}", taskId, pendingInstances.size());
    }

    @PostMapping("/{id}/stop")
    public Result<Boolean> stopTask(@PathVariable @NotNull Long id) {
        log.info("Stop task - task ID: {}", id);
        
        try {
            CollectTask collectTask = validateAndGetTask(id);
            if (collectTask == null) {
                return Result.error("Task not found");
            }
            
            // 更新任务状态
            boolean taskUpdated = collectTaskService.updateTaskStatus(id, "STOPPED");
            if (!taskUpdated) {
                return Result.error("Task status update failed");
            }
            
            // 获取所有RUNNING状态的用例执行例次
            List<TestCaseExecutionInstance> runningInstances = testCaseExecutionInstanceService.getByCollectTaskIdAndStatus(id, "RUNNING");
            
            // 按执行机IP分组，相同IP只发起一次停止调用
            Map<String, List<TestCaseExecutionInstance>> instancesByExecutor = groupInstancesByExecutor(runningInstances);
            
            // 调用CaseExecuteService取消正在执行的任务
            cancelExecutorTasks(id, instancesByExecutor);
            
            // 更新所有RUNNING状态的例次为STOPPED
            updateRunningInstancesToStopped(runningInstances);
            
            // 更新采集任务和用例执行例次的执行进度
            updateTaskExecutionProgress(id);
            
            log.info("Task stopped successfully - task ID: {}, updated instance count: {}", id, runningInstances.size());
            return Result.success(true);
            
        } catch (Exception e) {
            log.error("Failed to stop task - task ID: {}, error: {}", id, e.getMessage(), e);
            return Result.error("Failed to stop task: " + e.getMessage());
        }
    }

    /**
     * 按执行机分组实例
     */
    private Map<String, List<TestCaseExecutionInstance>> groupInstancesByExecutor(List<TestCaseExecutionInstance> runningInstances) {
        return runningInstances.stream()
                .filter(instance -> instance.getExecutionTaskId() != null && instance.getExecutorIp() != null)
                .collect(Collectors.groupingBy(TestCaseExecutionInstance::getExecutorIp));
    }
    
    /**
     * 通过执行机IP获取MAC地址
     */
    private String getExecutorMacAddress(String executorIp) {
        try {
            // 1. 通过IP地址查找执行机
            QueryWrapper<Executor> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("ip_address", executorIp);
            Executor executor = executorService.getOne(queryWrapper);
            
            if (executor == null) {
                log.warn("执行机不存在 - 执行机IP: {}", executorIp);
                return null;
            }
            
            // 2. 优先从executor表的mac_address字段获取
            if (executor.getMacAddress() != null && !executor.getMacAddress().trim().isEmpty()) {
                return executor.getMacAddress();
            }
            
            // 3. 如果executor表没有MAC地址，尝试通过macAddressId查找
            if (executor.getMacAddressId() != null) {
                ExecutorMacAddress macAddress = executorMacAddressService.getById(executor.getMacAddressId());
                if (macAddress != null && macAddress.getMacAddress() != null) {
                    return macAddress.getMacAddress();
                }
            }
            
            log.warn("执行机没有MAC地址 - 执行机IP: {}, 执行机ID: {}", executorIp, executor.getId());
            return null;
            
        } catch (Exception e) {
            log.error("获取执行机MAC地址失败 - 执行机IP: {}, 错误: {}", executorIp, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 取消执行机任务
     */
    private void cancelExecutorTasks(Long taskId, Map<String, List<TestCaseExecutionInstance>> instancesByExecutor) {
        for (Map.Entry<String, List<TestCaseExecutionInstance>> entry : instancesByExecutor.entrySet()) {
            String executorIp = entry.getKey();
            List<TestCaseExecutionInstance> instances = entry.getValue();
            
            // 取第一个实例的executionTaskId作为停止目标（因为同一执行机的任务通常使用相同的taskId）
            String executionTaskId = instances.get(0).getExecutionTaskId();
            
            try {
                boolean cancelled = false;
                
                // 通过IP地址查找执行机，获取MAC地址
                String executorMac = getExecutorMacAddress(executorIp);
                if (executorMac != null && !executorMac.trim().isEmpty()) {
                // 优先使用WebSocket发送停止命令
                    if (executorWebSocketService.isExecutorOnline(executorMac)) {
                        log.info("执行机在线，通过WebSocket发送停止命令 - 执行机MAC地址: {}, 执行机IP: {}, 任务ID: {}", executorMac, executorIp, executionTaskId);
                        cancelled = executorWebSocketService.sendCancelCommand(executorMac, executionTaskId);
                    if (cancelled) {
                        log.info("停止命令已通过WebSocket发送成功 - 任务ID: {}, 执行机IP: {}, 执行任务ID: {}, 受影响例次数: {}", 
                                taskId, executorIp, executionTaskId, instances.size());
                    } else {
                        log.warn("WebSocket发送停止命令失败，尝试使用HTTP发送 - 执行机MAC地址: {}, 执行机IP: {}, 任务ID: {}", executorMac, executorIp, executionTaskId);
                    }
                } else {
                    log.info("执行机不在线，使用HTTP发送停止命令 - 执行机MAC地址: {}, 执行机IP: {}, 任务ID: {}", executorMac, executorIp, executionTaskId);
                }
            } else {
                log.warn("无法获取执行机MAC地址，使用HTTP发送停止命令 - 执行机IP: {}, 任务ID: {}", executorIp, executionTaskId);
                }
                
                // 如果WebSocket不可用或发送失败，回退到HTTP请求
                if (!cancelled) {
                    cancelled = caseExecuteServiceClient.cancelTaskExecution(executorIp, executionTaskId);
                    if (cancelled) {
                        log.info("通过HTTP成功取消执行机任务 - 任务ID: {}, 执行机IP: {}, 执行任务ID: {}, 受影响例次数: {}", 
                                taskId, executorIp, executionTaskId, instances.size());
                    } else {
                        log.warn("通过HTTP取消执行机任务失败 - 任务ID: {}, 执行机IP: {}, 执行任务ID: {}", 
                                taskId, executorIp, executionTaskId);
                    }
                }
            } catch (Exception e) {
                log.error("取消执行机任务异常 - 任务ID: {}, 执行机IP: {}, 执行任务ID: {}, 错误: {}", 
                        taskId, executorIp, executionTaskId, e.getMessage(), e);
            }
        }
    }

    /**
     * 更新运行实例为停止状态
     */
    private void updateRunningInstancesToStopped(List<TestCaseExecutionInstance> runningInstances) {
        for (TestCaseExecutionInstance instance : runningInstances) {
            testCaseExecutionInstanceService.updateExecutionStatus(instance.getId(), "STOPPED", instance.getExecutionTaskId());
        }
    }

    @PostMapping("/{id}/pause")
    public Result<Boolean> pauseTask(@PathVariable @NotNull Long id) {
        log.info("Pause task - task ID: {}", id);
        
        try {
            CollectTask collectTask = validateAndGetTask(id);
            if (collectTask == null) {
                return Result.error("Task not found");
            }
            
            // 更新任务状态
            boolean taskUpdated = collectTaskService.updateTaskStatus(id, "PAUSED");
            if (!taskUpdated) {
                return Result.error("Task status update failed");
            }
            
            // 更新所有RUNNING状态的用例执行例次为PAUSED
            updateRunningInstancesToPaused(id);
            
            log.info("Task paused successfully - task ID: {}", id);
            return Result.success(true);
            
        } catch (Exception e) {
            log.error("Failed to pause task - task ID: {}, error: {}", id, e.getMessage(), e);
            return Result.error("Failed to pause task: " + e.getMessage());
        }
    }

    /**
     * 更新运行实例为暂停状态
     */
    private void updateRunningInstancesToPaused(Long taskId) {
        List<TestCaseExecutionInstance> runningInstances = testCaseExecutionInstanceService.getByCollectTaskIdAndStatus(taskId, "RUNNING");
        for (TestCaseExecutionInstance instance : runningInstances) {
            testCaseExecutionInstanceService.updateExecutionStatus(instance.getId(), "PAUSED", instance.getExecutionTaskId());
        }
        log.info("Task paused successfully - task ID: {}, updated instance count: {}", taskId, runningInstances.size());
    }

    @GetMapping("/status/{status}")
    public Result<List<CollectTask>> getByStatus(@PathVariable @NotNull String status) {
        QueryWrapper<CollectTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status);
        queryWrapper.orderByDesc("create_time");
        List<CollectTask> list = collectTaskService.list(queryWrapper);
        return Result.success(list);
    }

    /**
     * 更新任务状态（通用接口）
     * 
     * @param id 任务ID
     * @param status 新状态
     * @return 更新结果
     */
    @PutMapping("/{id}/status")
    public Result<Boolean> updateTaskStatus(@PathVariable @NotNull Long id, @RequestParam @NotNull String status) {
        log.info("Update task status - task ID: {}, new status: {}", id, status);
        
        try {
            CollectTask collectTask = validateAndGetTask(id);
            if (collectTask == null) {
                return Result.error("Task not found");
            }
            
            // 验证状态转换是否有效
            if (!isValidStatusTransition(collectTask.getStatus(), status)) {
                return Result.error("Invalid status transition: " + collectTask.getStatus() + " -> " + status);
            }
            
            // 更新任务状态
            boolean taskUpdated = collectTaskService.updateTaskStatus(id, status);
            if (!taskUpdated) {
                return Result.error("Task status update failed");
            }
            
            // 根据状态转换更新相关的用例执行例次状态
            updateExecutionInstancesStatus(id, collectTask.getStatus(), status);
            
            log.info("Task status updated successfully - task ID: {}, status: {} -> {}", id, collectTask.getStatus(), status);
            return Result.success(true);
            
        } catch (Exception e) {
            log.error("Failed to update task status - task ID: {}, new status: {}, error: {}", id, status, e.getMessage(), e);
            return Result.error("Failed to update task status: " + e.getMessage());
        }
    }
    
    /**
     * 验证状态转换是否有效
     * 
     * @param currentStatus 当前状态
     * @param newStatus 新状态
     * @return 是否有效
     */
    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        // 定义有效的状态转换规则
        if ("PENDING".equals(currentStatus)) {
            return "RUNNING".equals(newStatus) || "STOPPED".equals(newStatus);
        } else if ("RUNNING".equals(currentStatus)) {
            return "COMPLETED".equals(newStatus) || "STOPPED".equals(newStatus) || "PAUSED".equals(newStatus);
        } else if ("PAUSED".equals(currentStatus)) {
            return "RUNNING".equals(newStatus) || "STOPPED".equals(newStatus);
        } else if ("STOPPED".equals(currentStatus)) {
            return "PENDING".equals(newStatus) || "RUNNING".equals(newStatus);
        } else if ("COMPLETED".equals(currentStatus)) {
            return false; // 已完成状态不能转换到其他状态
        }
        
        return false;
    }
    
    /**
     * 根据状态转换更新用例执行例次状态
     * 
     * @param taskId 任务ID
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     */
    private void updateExecutionInstancesStatus(Long taskId, String oldStatus, String newStatus) {
        try {
            StatusTransitionInfo transitionInfo = getStatusTransitionInfo(oldStatus, newStatus);
            
            if (transitionInfo != null) {
                List<TestCaseExecutionInstance> instances = testCaseExecutionInstanceService.getByCollectTaskIdAndStatus(taskId, transitionInfo.getSourceStatus());
                for (TestCaseExecutionInstance instance : instances) {
                    testCaseExecutionInstanceService.updateExecutionStatus(instance.getId(), transitionInfo.getTargetStatus(), instance.getExecutionTaskId());
                }
                log.info("Updated test case execution instance status - task ID: {}, status transition: {} -> {}, affected instance count: {}", 
                        taskId, transitionInfo.getSourceStatus(), transitionInfo.getTargetStatus(), instances.size());
            }
        } catch (Exception e) {
            log.error("Failed to update test case execution instance status - task ID: {}, error: {}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 状态转换信息
     */
    private static class StatusTransitionInfo {
        private String sourceStatus;
        private String targetStatus;
        
        public StatusTransitionInfo(String sourceStatus, String targetStatus) {
            this.sourceStatus = sourceStatus;
            this.targetStatus = targetStatus;
        }
        
        public String getSourceStatus() { return sourceStatus; }
        public String getTargetStatus() { return targetStatus; }
    }

    /**
     * 获取状态转换信息
     */
    private StatusTransitionInfo getStatusTransitionInfo(String oldStatus, String newStatus) {
        // 根据状态转换确定需要更新的例次状态
        if ("STOPPED".equals(oldStatus) && "RUNNING".equals(newStatus)) {
             return new StatusTransitionInfo("STOPPED", "PENDING");
         }
    
        return new StatusTransitionInfo(oldStatus, newStatus);
    }

    /**
     * 获取任务执行进度
     */
    @GetMapping("/{id}/progress")
    public Result<Map<String, Object>> getTaskProgress(@PathVariable @NotNull Long id) {
        log.info("Get task execution progress - task ID: {}", id);
        
        try {
            CollectTask task = collectTaskService.getById(id);
            if (task == null) {
                return Result.error("Task not found");
            }
            
            // 获取执行例次列表，基于真实数据计算进度
            List<TestCaseExecutionInstance> instances = testCaseExecutionInstanceService.getByCollectTaskId(id);
            
            Map<String, Object> progress = calculateTaskProgress(instances);
            
            return Result.success(progress);
        } catch (Exception e) {
            log.error("Failed to get task execution progress - task ID: {}, error: {}", id, e.getMessage(), e);
            return Result.error("Failed to get task execution progress: " + e.getMessage());
        }
    }

    /**
     * 计算任务进度
     */
    private Map<String, Object> calculateTaskProgress(List<TestCaseExecutionInstance> instances) {
        int totalCount = instances.size();
        int successCount = 0;
        int failedCount = 0;
        int blockedCount = 0;
        int runningCount = 0;
        
        for (TestCaseExecutionInstance instance : instances) {
            String status = instance.getStatus();
            String result = instance.getResult();
            
            // 只要不是执行中，都算作已完成
            if (!"RUNNING".equals(status)) {
                // 根据执行结果统计
                if ("SUCCESS".equals(result)) {
                    successCount++;
                } else if ("FAILED".equals(result)) {
                    failedCount++;
                } else if ("BLOCKED".equals(result)) {
                    blockedCount++;
                }
            } else {
                runningCount++;
            }
        }
        
        Map<String, Object> progress = new HashMap<>();
        progress.put("totalCount", totalCount);
        progress.put("successCount", successCount);
        progress.put("failedCount", failedCount);
        progress.put("blockedCount", blockedCount);
        progress.put("runningCount", runningCount);
        
        return progress;
    }

    /**
     * 获取任务执行例次列表
     */
    @GetMapping("/{id}/execution-instances")
    public Result<List<Map<String, Object>>> getExecutionInstances(@PathVariable @NotNull Long id) {
        log.info("Get task execution instance list - task ID: {}", id);
        
        try {
            CollectTask task = collectTaskService.getById(id);
            if (task == null) {
                return Result.error("Task not found");
            }
            
            // 获取执行例次列表
            List<TestCaseExecutionInstance> instances = testCaseExecutionInstanceService.getByCollectTaskId(id);
            List<Map<String, Object>> result = buildExecutionInstancesResult(instances);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to get task execution instance list - task ID: {}, error: {}", id, e.getMessage(), e);
            return Result.error("Failed to get task execution instance list: " + e.getMessage());
        }
    }

    /**
     * 构建执行例次结果
     */
    private List<Map<String, Object>> buildExecutionInstancesResult(List<TestCaseExecutionInstance> instances) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (TestCaseExecutionInstance instance : instances) {
            Map<String, Object> instanceMap = buildInstanceMap(instance);
            result.add(instanceMap);
        }
        
        return result;
    }

    /**
     * 构建单个实例的Map
     */
    private Map<String, Object> buildInstanceMap(TestCaseExecutionInstance instance) {
        Map<String, Object> instanceMap = new HashMap<>();
        instanceMap.put("id", instance.getId());
        instanceMap.put("testCaseId", instance.getTestCaseId());
        instanceMap.put("round", instance.getRound());
        instanceMap.put("logicEnvironmentId", instance.getLogicEnvironmentId());
        instanceMap.put("executorIp", instance.getExecutorIp());
        instanceMap.put("status", instance.getStatus());
        instanceMap.put("result", instance.getResult());
        instanceMap.put("failureReason", instance.getFailureReason());
        instanceMap.put("logFilePath", instance.getLogFilePath());
        instanceMap.put("executionTaskId", instance.getExecutionTaskId());
        instanceMap.put("createTime", instance.getCreateTime());
        instanceMap.put("updateTime", instance.getUpdateTime());
        
        // 获取用例信息
        TestCase testCase = testCaseService.getById(instance.getTestCaseId());
        if (testCase != null) {
            instanceMap.put("testCaseNumber", testCase.getNumber());
            instanceMap.put("testCaseName", testCase.getName());
        }
        
        // 获取逻辑环境信息
        LogicEnvironment logicEnvironment = logicEnvironmentService.getById(instance.getLogicEnvironmentId());
        if (logicEnvironment != null) {
            instanceMap.put("logicEnvironmentName", logicEnvironment.getName());
        }
        
        return instanceMap;
    }
    
    /**
     * 更新任务执行进度
     * 
     * @param taskId 任务ID
     */
    private void updateTaskExecutionProgress(Long taskId) {
        try {
            // 获取任务的所有用例执行例次
            List<TestCaseExecutionInstance> allInstances = testCaseExecutionInstanceService.getByCollectTaskId(taskId);
            
            // 统计各种状态的例次数量
            long totalCount = allInstances.size();
            long completedCount = allInstances.stream().filter(instance -> "COMPLETED".equals(instance.getStatus())).count();
            long failedCount = allInstances.stream().filter(instance -> "FAILED".equals(instance.getStatus())).count();
            
            // 更新任务进度
            collectTaskService.updateTaskProgress(taskId, (int) totalCount, (int) completedCount, (int) failedCount);
            
            log.info("Updated task execution progress - task ID: {}, total: {}, completed: {}, failed: {}", taskId, totalCount, completedCount, failedCount);
            
        } catch (Exception e) {
            log.error("Failed to update task execution progress - task ID: {}, error: {}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 根据采集策略和环境筛选条件获取可用的逻辑环境列表
     */
    @GetMapping("/available-logic-environments")
    public Result<List<LogicEnvironmentDTO>> getAvailableLogicEnvironments(
            @RequestParam @NotNull Long strategyId,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) Long provinceId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) String network,
            @RequestParam(required = false) List<String> manufacturer) {
        
        log.info("Start getting available logic environment list - strategy ID: {}, region filter: regionId={}, countryId={}, provinceId={}, cityId={}, network={}, manufacturer={}", 
                strategyId, regionId, countryId, provinceId, cityId, network, manufacturer);
        
        try {
            // 1. 获取采集策略信息
            CollectStrategy strategy = validateAndGetStrategy(strategyId);
            if (strategy == null) {
                return Result.error("Collect strategy not found");
            }
            
            // 2. 获取策略关联的测试用例（基于筛选条件）
            List<TestCase> testCases = getFilteredTestCases(strategy);
            
            // 3. 根据用例的逻辑组网、网络和厂商，组成物理组网列表
            Set<String> requiredPhysicalNetworks = extractRequiredPhysicalNetworks(testCases, network, manufacturer);
            
            // 4. 根据环境筛选条件获取可用的执行机
            List<Executor> availableExecutors = getAvailableExecutors(regionId, countryId, provinceId, cityId);
            
            // 5. 获取执行机关联的逻辑环境列表B
            List<LogicEnvironment> allLogicEnvironments = getAllLogicEnvironments(availableExecutors);
            
            // 6. 筛选逻辑环境，检查其物理组网是否存在于物理组网列表A中
            List<LogicEnvironmentDTO> availableLogicEnvironments = filterMatchingLogicEnvironmentsByPhysicalNetwork(allLogicEnvironments, requiredPhysicalNetworks);
            
            log.info("Matching completed - available logic environment count: {}", availableLogicEnvironments.size());
            
            return Result.success(availableLogicEnvironments);
            
        } catch (Exception e) {
            log.error("Failed to get available logic environment list", e);
            return Result.error("Failed to get available logic environment list: " + e.getMessage());
        }
    }

    /**
     * 验证并获取采集策略
     */
    private CollectStrategy validateAndGetStrategy(Long strategyId) {
        log.info("Step 1: Get collect strategy information - strategy ID: {}", strategyId);
        CollectStrategy strategy = collectStrategyService.getById(strategyId);
        if (strategy == null) {
            log.error("Collect strategy not found - strategy ID: {}", strategyId);
            return null;
        }
        log.info("Got collect strategy: {} (test case set ID: {})", strategy.getName(), strategy.getTestCaseSetId());
        return strategy;
    }

    /**
     * 获取测试用例（根据策略中勾选的用例返回）
     */
    private List<TestCase> getFilteredTestCases(CollectStrategy strategy) {
        log.info("Step 2: Get test cases associated with strategy - test case set ID: {}", 
                strategy.getTestCaseSetId());
        List<TestCase> allTestCases = testCaseService.getByTestCaseSetId(strategy.getTestCaseSetId());
        log.info("Got all test case count: {}", allTestCases.size());
        
        // 获取策略中勾选的用例ID列表
        List<Long> selectedTestCaseIds = getSelectedTestCaseIds(strategy);
        
        if (selectedTestCaseIds == null || selectedTestCaseIds.isEmpty()) {
            log.info("No selected test cases found in strategy, returning all test cases");
            return allTestCases;
        }
        
        log.info("Strategy has {} selected test case IDs: {}", selectedTestCaseIds.size(), selectedTestCaseIds);
        
        // 根据勾选的用例ID筛选用例
        List<TestCase> filteredTestCases = allTestCases.stream()
                .filter(testCase -> selectedTestCaseIds.contains(testCase.getId()))
                .collect(Collectors.toList());
        
        log.info("Filtered test case count: {} (from {} total test cases)", 
                filteredTestCases.size(), allTestCases.size());
        
        return filteredTestCases;
    }
    
    /**
     * 获取策略中勾选的用例ID列表
     */
    private List<Long> getSelectedTestCaseIds(CollectStrategy strategy) {
        List<Long> selectedIds = new ArrayList<>();
        
        try {
            // 首先尝试从 selectedTestCaseIds 字段获取
            if (strategy.getSelectedTestCaseIds() != null && !strategy.getSelectedTestCaseIds().trim().isEmpty()) {
                List<Object> ids = JSON.parseArray(strategy.getSelectedTestCaseIds(), Object.class);
                if (ids != null) {
                    for (Object id : ids) {
                        if (id != null) {
                            Long testCaseId = null;
                            if (id instanceof Number) {
                                testCaseId = ((Number) id).longValue();
                            } else if (id instanceof String) {
                                try {
                                    testCaseId = Long.parseLong((String) id);
                                } catch (NumberFormatException e) {
                                    log.warn("Invalid test case ID format: {}", id);
                                }
                            }
                            if (testCaseId != null) {
                                selectedIds.add(testCaseId);
                            }
                        }
                    }
                }
            }
            
            // 如果没有 selectedTestCaseIds，尝试从 testCaseExecutionCounts 中获取（兼容旧数据）
            if (selectedIds.isEmpty() && strategy.getTestCaseExecutionCounts() != null 
                    && !strategy.getTestCaseExecutionCounts().trim().isEmpty()) {
                try {
                    Map<String, Object> executionCounts = JSON.parseObject(
                            strategy.getTestCaseExecutionCounts(), 
                            new TypeReference<Map<String, Object>>() {});
                    if (executionCounts != null) {
                        for (Map.Entry<String, Object> entry : executionCounts.entrySet()) {
                            try {
                                Long testCaseId = Long.parseLong(entry.getKey());
                                Object count = entry.getValue();
                                // 如果执行次数大于0，则认为该用例被勾选
                                if (count instanceof Number && ((Number) count).intValue() > 0) {
                                    selectedIds.add(testCaseId);
                                }
                            } catch (NumberFormatException e) {
                                log.warn("Invalid test case ID in execution counts: {}", entry.getKey());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse testCaseExecutionCounts: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse selectedTestCaseIds: {}", e.getMessage());
        }
        
        return selectedIds;
    }

    /**
     * 提取测试用例中的环境组网需求
     */
    private Set<String> extractRequiredNetworks(List<TestCase> testCases) {
        log.info("Step 3: Extract environment network requirements from test cases");
        Set<String> requiredNetworks = new HashSet<>();
        
        for (TestCase testCase : testCases) {
            log.debug("Processing test case: {} (number: {})", testCase.getName(), testCase.getNumber());
            if (testCase.getLogicNetwork() != null && !testCase.getLogicNetwork().trim().isEmpty()) {
                String[] networks = testCase.getLogicNetwork().split(";");
                log.debug("Test case {} environment network requirements: {}", testCase.getNumber(), testCase.getLogicNetwork());
                for (String network : networks) {
                    String trimmedNetwork = network.trim();
                    requiredNetworks.add(trimmedNetwork);
                    log.debug("Added environment network requirement: {}", trimmedNetwork);
                }
            } else {
                log.debug("Test case {} has no environment network requirements", testCase.getNumber());
            }
        }
        
        log.info("Extracted environment network requirements list A: {}", requiredNetworks);
        return requiredNetworks;
    }

    /**
     * 根据用例的逻辑组网、网络和厂商，组成物理组网列表
     * 物理组网格式：逻辑组网_网络_厂商
     */
    private Set<String> extractRequiredPhysicalNetworks(List<TestCase> testCases, String network, List<String> manufacturer) {
        log.info("Step 3: Extract physical network requirements from test cases - network: {}, manufacturer: {}", network, manufacturer);
        Set<String> requiredPhysicalNetworks = new HashSet<>();
        
        // 如果没有选择网络或厂商，返回空集合
        if (network == null || network.trim().isEmpty() || manufacturer == null || manufacturer.isEmpty()) {
            log.info("Network or manufacturer not selected, returning empty physical network list");
            return requiredPhysicalNetworks;
        }
        
        // 过滤掉空的厂商
        List<String> manufacturers = manufacturer.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        
        if (manufacturers.isEmpty()) {
            log.info("No valid manufacturers found, returning empty physical network list");
            return requiredPhysicalNetworks;
        }
        
        // 遍历所有用例，提取逻辑组网
        Set<String> logicNetworks = new HashSet<>();
        for (TestCase testCase : testCases) {
            if (testCase.getLogicNetwork() != null && !testCase.getLogicNetwork().trim().isEmpty()) {
                String[] networks = testCase.getLogicNetwork().split(";");
                for (String logicNetwork : networks) {
                    String trimmedNetwork = logicNetwork.trim();
                    if (!trimmedNetwork.isEmpty()) {
                        logicNetworks.add(trimmedNetwork);
                    }
                }
            }
        }
        
        // 生成所有可能的物理组网组合：逻辑组网_网络_厂商
        for (String logicNetwork : logicNetworks) {
            for (String manu : manufacturers) {
                String physicalNetwork = logicNetwork + "_" + network + "_" + manu;
                requiredPhysicalNetworks.add(physicalNetwork);
                log.debug("Generated physical network: {}", physicalNetwork);
            }
        }
        
        log.info("Extracted physical network requirements list: {}", requiredPhysicalNetworks);
        return requiredPhysicalNetworks;
    }

    /**
     * 获取可用的执行机
     */
    private List<Executor> getAvailableExecutors(Long regionId, Long countryId, Long provinceId, Long cityId) {
        log.info("Step 4: Get available executors based on region filter criteria - regionId={}, countryId={}, provinceId={}, cityId={}", 
                regionId, countryId, provinceId, cityId);
        List<Executor> availableExecutors = executorService.getExecutorsByRegion(
            regionId, countryId, provinceId, cityId);
        log.info("Got available executor count: {}", availableExecutors.size());
        
        for (Executor executor : availableExecutors) {
            log.debug("Matched executor: {} (ID: {}, IP: {})", executor.getName(), executor.getId(), executor.getIpAddress());
        }
        
        return availableExecutors;
    }

    /**
     * 获取所有逻辑环境
     */
    private List<LogicEnvironment> getAllLogicEnvironments(List<Executor> availableExecutors) {
        log.info("Step 5: Get logic environments associated with executors");
        List<LogicEnvironment> allLogicEnvironments = new ArrayList<>();
        
        for (Executor executor : availableExecutors) {
            log.debug("Get logic environments associated with executor {}", executor.getName());
            List<LogicEnvironment> environments = logicEnvironmentService.getByExecutorId(executor.getId());
            log.debug("Executor {} associated logic environment count: {}", executor.getName(), environments.size());
            for (LogicEnvironment env : environments) {
                log.debug("Executor {} associated logic environment: {} (ID: {})", executor.getName(), env.getName(), env.getId());
            }
            allLogicEnvironments.addAll(environments);
        }
        
        log.info("Got all logic environment count: {}", allLogicEnvironments.size());
        return allLogicEnvironments;
    }

    /**
     * 筛选匹配的逻辑环境
     */
    private List<LogicEnvironmentDTO> filterMatchingLogicEnvironments(List<LogicEnvironment> allLogicEnvironments, Set<String> requiredNetworks) {
        log.info("Step 6: Filter matching logic environments");
        List<LogicEnvironmentDTO> availableLogicEnvironments = new ArrayList<>();
        
        for (LogicEnvironment logicEnvironment : allLogicEnvironments) {
            log.debug("Checking logic environment: {} (ID: {})", logicEnvironment.getName(), logicEnvironment.getId());
            
            // 获取逻辑环境关联的环境组网
            List<LogicEnvironmentNetwork> environmentNetworks = logicEnvironmentNetworkService.getByLogicEnvironmentId(logicEnvironment.getId());
            log.debug("Logic environment {} associated environment network count: {}", logicEnvironment.getName(), environmentNetworks.size());
            
            // 检查是否有匹配的环境组网
            boolean hasMatchingNetwork = checkMatchingNetworks(environmentNetworks, requiredNetworks);
            List<String> logicEnvironmentNetworks = extractLogicEnvironmentNetworks(environmentNetworks);
            log.debug("Logic environment {} environment networks: {}", logicEnvironment.getName(), logicEnvironmentNetworks);
            
            // 如果有匹配的环境组网，则添加到可用列表
            if (hasMatchingNetwork) {
                log.info("Logic environment {} matched successfully, added to available list", logicEnvironment.getName());
                LogicEnvironmentDTO dto = logicEnvironmentService.getLogicEnvironmentDTO(logicEnvironment.getId());
                availableLogicEnvironments.add(dto);
            } else {
                log.debug("Logic environment {} doesn't match, skipping", logicEnvironment.getName());
            }
        }
        
        return availableLogicEnvironments;
    }

    /**
     * 检查是否有匹配的环境组网
     */
    private boolean checkMatchingNetworks(List<LogicEnvironmentNetwork> environmentNetworks, Set<String> requiredNetworks) {
        for (LogicEnvironmentNetwork envNetwork : environmentNetworks) {
            NetworkType network = networkTypeService.getById(envNetwork.getLogicNetworkId());
            if (network != null && requiredNetworks.contains(network.getName())) {
                log.debug("Found matching environment network: {} (logic environment: {})", network.getName(), network.getName());
                return true;
            }
        }
        return false;
    }

    /**
     * 提取逻辑环境的环境组网名称
     */
    private List<String> extractLogicEnvironmentNetworks(List<LogicEnvironmentNetwork> environmentNetworks) {
        List<String> logicEnvironmentNetworks = new ArrayList<>();
        for (LogicEnvironmentNetwork envNetwork : environmentNetworks) {
            NetworkType network = networkTypeService.getById(envNetwork.getLogicNetworkId());
            if (network != null) {
                logicEnvironmentNetworks.add(network.getName());
            }
        }
        return logicEnvironmentNetworks;
    }

    /**
     * 根据物理组网筛选匹配的逻辑环境
     */
    private List<LogicEnvironmentDTO> filterMatchingLogicEnvironmentsByPhysicalNetwork(
            List<LogicEnvironment> allLogicEnvironments, Set<String> requiredPhysicalNetworks) {
        log.info("Step 6: Filter matching logic environments by physical network");
        List<LogicEnvironmentDTO> availableLogicEnvironments = new ArrayList<>();
        
        for (LogicEnvironment logicEnvironment : allLogicEnvironments) {
            log.debug("Checking logic environment: {} (ID: {})", logicEnvironment.getName(), logicEnvironment.getId());
            
            // 获取逻辑环境的物理组网列表（JSON格式）
            String physicalNetworkJson = logicEnvironment.getPhysicalNetwork();
            if (physicalNetworkJson == null || physicalNetworkJson.trim().isEmpty()) {
                log.debug("Logic environment {} has no physical network data, skipping", logicEnvironment.getName());
                continue;
            }
            
            try {
                // 解析物理组网JSON数组
                List<String> physicalNetworks = JSON.parseArray(physicalNetworkJson, String.class);
                log.debug("Logic environment {} physical networks: {}", logicEnvironment.getName(), physicalNetworks);
                
                // 检查是否有匹配的物理组网
                boolean hasMatchingPhysicalNetwork = false;
                for (String physicalNetwork : physicalNetworks) {
                    if (requiredPhysicalNetworks.contains(physicalNetwork)) {
                        log.debug("Found matching physical network: {} in logic environment {}", 
                                physicalNetwork, logicEnvironment.getName());
                        hasMatchingPhysicalNetwork = true;
                        break;
                    }
                }
                
                // 如果有匹配的物理组网，则添加到可用列表
                if (hasMatchingPhysicalNetwork) {
                    log.info("Logic environment {} matched successfully by physical network, added to available list", 
                            logicEnvironment.getName());
                    LogicEnvironmentDTO dto = logicEnvironmentService.getLogicEnvironmentDTO(logicEnvironment.getId());
                    availableLogicEnvironments.add(dto);
                } else {
                    log.debug("Logic environment {} doesn't match by physical network, skipping", logicEnvironment.getName());
                }
            } catch (Exception e) {
                log.error("Failed to parse physical network JSON for logic environment {}: {}", 
                        logicEnvironment.getName(), e.getMessage());
            }
        }
        
        return availableLogicEnvironments;
    }

    /**
     * 批量检查执行机在线状态（通过WebSocket检查）
     * 
     * @param executorIps 执行机IP地址列表
     * @return 执行机在线状态映射，key为IP地址，value为是否在线
     */
    @PostMapping("/check-executors-online")
    public Result<Map<String, Boolean>> checkExecutorsOnline(@RequestBody List<String> executorIps) {
        log.info("Checking executors online status - IPs: {}", executorIps);
        
        try {
            Map<String, Boolean> onlineStatusMap = new HashMap<>();
            
            for (String ip : executorIps) {
                // 通过执行机IP查找执行机
                QueryWrapper<Executor> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("ip_address", ip);
                queryWrapper.eq("deleted", 0);
                Executor executor = executorService.getOne(queryWrapper);
                
                if (executor != null) {
                    // 检查执行机状态：status=1表示在线（通过WebSocket连接活跃）
                    // 这里可以根据实际的WebSocket服务来检查连接状态
                    // 目前先使用数据库中的status字段，如果WebSocket服务已实现，可以改为检查WebSocket连接状态
                    boolean isOnline = executor.getStatus() != null && executor.getStatus() == 1;
                    onlineStatusMap.put(ip, isOnline);
                    log.debug("Executor online status - IP: {}, status: {}, isOnline: {}", 
                            ip, executor.getStatus(), isOnline);
                } else {
                    // 执行机不存在，认为不在线
                    onlineStatusMap.put(ip, false);
                    log.warn("Executor not found - IP: {}", ip);
                }
            }
            
            log.info("Checked executors online status - result: {}", onlineStatusMap);
            return Result.success(onlineStatusMap);
            
        } catch (Exception e) {
            log.error("Failed to check executors online status", e);
            return Result.error("Failed to check executors online status: " + e.getMessage());
        }
    }

}
