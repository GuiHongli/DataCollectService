package com.datacollect.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.dto.CollectTaskRequest;
import com.datacollect.entity.CollectStrategy;
import com.datacollect.entity.CollectTask;
import com.datacollect.entity.Executor;
import com.datacollect.entity.LogicEnvironment;
import com.datacollect.entity.LogicEnvironmentNetwork;
import com.datacollect.entity.LogicNetwork;
import com.datacollect.entity.TestCase;
import com.datacollect.entity.TestCaseExecutionInstance;
import com.datacollect.entity.dto.LogicEnvironmentDTO;
import com.datacollect.service.CaseExecuteServiceClient;
import com.datacollect.service.CollectStrategyService;
import com.datacollect.service.CollectTaskProcessService;
import com.datacollect.service.CollectTaskService;
import com.datacollect.service.ExecutorService;
import com.datacollect.service.LogicEnvironmentNetworkService;
import com.datacollect.service.LogicEnvironmentService;
import com.datacollect.service.LogicNetworkService;
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
    private LogicEnvironmentService logicEnvironmentService;

    @Autowired
    private LogicEnvironmentNetworkService logicEnvironmentNetworkService;

    @Autowired
    private LogicNetworkService logicNetworkService;

    @Autowired
    private CollectTaskProcessService collectTaskProcessService;

    @Autowired
    private TestCaseExecutionInstanceService testCaseExecutionInstanceService;
    
    @Autowired
    private CaseExecuteServiceClient caseExecuteServiceClient;

    @PostMapping
    public Result<CollectTask> create(@Valid @RequestBody CollectTask collectTask) {
        collectTaskService.save(collectTask);
        return Result.success(collectTask);
    }

    /**
     * 创建采集任务（新接口）
     * 
     * @param request 采集任务请求
     * @return 采集任务ID
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createCollectTask(@Valid @RequestBody CollectTaskRequest request) {
        log.info("Received create collect task request - task name: {}, collect strategy ID: {}", request.getName(), request.getCollectStrategyId());
        
        try {
            // 调用处理服务创建采集任务
            Long collectTaskId = collectTaskProcessService.processCollectTaskCreation(request);
            
            Map<String, Object> result = createSuccessResult(collectTaskId);
            
            log.info("Collect task created successfully - task ID: {}", collectTaskId);
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
            @RequestParam(required = false) String status) {
        
        Page<CollectTask> page = new Page<>(current, size);
        QueryWrapper<CollectTask> queryWrapper = buildPageQueryWrapper(name, strategyId, status);
        Page<CollectTask> result = collectTaskService.page(page, queryWrapper);
        return Result.success(result);
    }

    /**
     * 构建分页查询条件
     */
    private QueryWrapper<CollectTask> buildPageQueryWrapper(String name, Long strategyId, String status) {
        QueryWrapper<CollectTask> queryWrapper = new QueryWrapper<>();
        
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
     * 取消执行机任务
     */
    private void cancelExecutorTasks(Long taskId, Map<String, List<TestCaseExecutionInstance>> instancesByExecutor) {
        for (Map.Entry<String, List<TestCaseExecutionInstance>> entry : instancesByExecutor.entrySet()) {
            String executorIp = entry.getKey();
            List<TestCaseExecutionInstance> instances = entry.getValue();
            
            // 取第一个实例的executionTaskId作为停止目标（因为同一执行机的任务通常使用相同的taskId）
            String executionTaskId = instances.get(0).getExecutionTaskId();
            
            try {
                boolean cancelled = caseExecuteServiceClient.cancelTaskExecution(executorIp, executionTaskId);
                if (cancelled) {
                    log.info("Successfully cancelled executor task - task ID: {}, executor IP: {}, execution task ID: {}, affected instance count: {}", 
                            taskId, executorIp, executionTaskId, instances.size());
                } else {
                    log.warn("Failed to cancel executor task - task ID: {}, executor IP: {}, execution task ID: {}", 
                            taskId, executorIp, executionTaskId);
                }
            } catch (Exception e) {
                log.error("Exception calling CaseExecuteService to cancel task - task ID: {}, executor IP: {}, execution task ID: {}, error: {}", 
                        taskId, executorIp, executionTaskId, e.getMessage());
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
            @RequestParam(required = false) Long cityId) {
        
        log.info("Start getting available logic environment list - strategy ID: {}, region filter: regionId={}, countryId={}, provinceId={}, cityId={}", 
                strategyId, regionId, countryId, provinceId, cityId);
        
        try {
            // 1. 获取采集策略信息
            CollectStrategy strategy = validateAndGetStrategy(strategyId);
            if (strategy == null) {
                return Result.error("Collect strategy not found");
            }
            
            // 2. 获取策略关联的测试用例（基于筛选条件）
            List<TestCase> testCases = getFilteredTestCases(strategy);
            
            // 3. 提取测试用例中的环境组网列表A
            Set<String> requiredNetworks = extractRequiredNetworks(testCases);
            
            // 4. 根据环境筛选条件获取可用的执行机
            List<Executor> availableExecutors = getAvailableExecutors(regionId, countryId, provinceId, cityId);
            
            // 5. 获取执行机关联的逻辑环境列表B
            List<LogicEnvironment> allLogicEnvironments = getAllLogicEnvironments(availableExecutors);
            
            // 6. 筛选逻辑环境，检查其环境组网是否存在于环境组网列表A中
            List<LogicEnvironmentDTO> availableLogicEnvironments = filterMatchingLogicEnvironments(allLogicEnvironments, requiredNetworks);
            
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
     * 获取筛选后的测试用例
     */
    private List<TestCase> getFilteredTestCases(CollectStrategy strategy) {
        log.info("Step 2: Get test cases associated with strategy - test case set ID: {}, filter criteria: business category={}, APP={}", 
                strategy.getTestCaseSetId(), strategy.getBusinessCategory(), strategy.getApp());
        List<TestCase> allTestCases = testCaseService.getByTestCaseSetId(strategy.getTestCaseSetId());
        log.info("Got original test case count: {}", allTestCases.size());
        
        // 根据策略的筛选条件过滤用例
        List<TestCase> testCases = allTestCases.stream()
            .filter(testCase -> {
                // 业务大类筛选
                if (strategy.getBusinessCategory() != null && !strategy.getBusinessCategory().isEmpty()) {
                    if (!strategy.getBusinessCategory().equals(testCase.getBusinessCategory())) {
                        return false;
                    }
                }
                
                // APP筛选
                if (strategy.getApp() != null && !strategy.getApp().isEmpty()) {
                    if (!strategy.getApp().equals(testCase.getApp())) {
                        return false;
                    }
                }
                
                return true;
            })
            .collect(java.util.stream.Collectors.toList());
        log.info("Filtered test case count: {}", testCases.size());
        
        return testCases;
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
            LogicNetwork network = logicNetworkService.getById(envNetwork.getLogicNetworkId());
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
            LogicNetwork network = logicNetworkService.getById(envNetwork.getLogicNetworkId());
            if (network != null) {
                logicEnvironmentNetworks.add(network.getName());
            }
        }
        return logicEnvironmentNetworks;
    }
}
