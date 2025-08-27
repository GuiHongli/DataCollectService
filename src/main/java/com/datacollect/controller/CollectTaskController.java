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
import java.util.ArrayList;
import com.datacollect.entity.dto.LogicEnvironmentDTO;
import com.datacollect.service.LogicEnvironmentService;
import com.datacollect.service.CollectStrategyService;
import com.datacollect.service.TestCaseService;
import com.datacollect.service.ExecutorService;
import com.datacollect.entity.CollectStrategy;
import com.datacollect.entity.TestCase;
import com.datacollect.entity.Executor;
import com.datacollect.entity.LogicEnvironment;
import com.datacollect.entity.LogicEnvironmentNetwork;
import com.datacollect.entity.LogicNetwork;
import com.datacollect.service.LogicEnvironmentNetworkService;
import com.datacollect.service.LogicNetworkService;
import com.datacollect.service.CollectTaskProcessService;
import com.datacollect.dto.CollectTaskRequest;
import com.datacollect.service.CaseExecuteServiceClient;
import com.datacollect.service.TestCaseExecutionInstanceService;
import com.datacollect.entity.TestCaseExecutionInstance;

import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

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
        log.info("接收到创建采集任务请求 - 任务名称: {}, 采集策略ID: {}", request.getName(), request.getCollectStrategyId());
        
        try {
            // 调用处理服务创建采集任务
            Long collectTaskId = collectTaskProcessService.processCollectTaskCreation(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("collectTaskId", collectTaskId);
            result.put("message", "采集任务创建成功");
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("采集任务创建成功 - 任务ID: {}", collectTaskId);
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("创建采集任务失败 - 任务名称: {}, 错误: {}", request.getName(), e.getMessage(), e);
            return Result.error("创建采集任务失败: " + e.getMessage());
        }
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
        log.info("启动任务 - 任务ID: {}", id);
        
        try {
            CollectTask collectTask = collectTaskService.getById(id);
            if (collectTask == null) {
                return Result.error("任务不存在");
            }
            
            // 更新任务状态
            boolean taskUpdated = collectTaskService.updateTaskStatus(id, "RUNNING");
            if (!taskUpdated) {
                return Result.error("任务状态更新失败");
            }
            
            // 更新所有PENDING状态的用例执行例次为RUNNING
            List<TestCaseExecutionInstance> pendingInstances = testCaseExecutionInstanceService.getByCollectTaskIdAndStatus(id, "PENDING");
            for (TestCaseExecutionInstance instance : pendingInstances) {
                testCaseExecutionInstanceService.updateExecutionStatus(instance.getId(), "RUNNING", null);
            }
            
            log.info("任务启动成功 - 任务ID: {}, 更新例次数量: {}", id, pendingInstances.size());
            return Result.success(true);
            
        } catch (Exception e) {
            log.error("启动任务失败 - 任务ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error("启动任务失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/stop")
    public Result<Boolean> stopTask(@PathVariable @NotNull Long id) {
        log.info("停止任务 - 任务ID: {}", id);
        
        try {
            CollectTask collectTask = collectTaskService.getById(id);
            if (collectTask == null) {
                return Result.error("任务不存在");
            }
            
            // 更新任务状态
            boolean taskUpdated = collectTaskService.updateTaskStatus(id, "STOPPED");
            if (!taskUpdated) {
                return Result.error("任务状态更新失败");
            }
            
            // 获取所有RUNNING状态的用例执行例次
            List<TestCaseExecutionInstance> runningInstances = testCaseExecutionInstanceService.getByCollectTaskIdAndStatus(id, "RUNNING");
            
            // 按执行机IP分组，相同IP只发起一次停止调用
            Map<String, List<TestCaseExecutionInstance>> instancesByExecutor = runningInstances.stream()
                    .filter(instance -> instance.getExecutionTaskId() != null && instance.getExecutorIp() != null)
                    .collect(Collectors.groupingBy(TestCaseExecutionInstance::getExecutorIp));
            
            // 调用CaseExecuteService取消正在执行的任务
            for (Map.Entry<String, List<TestCaseExecutionInstance>> entry : instancesByExecutor.entrySet()) {
                String executorIp = entry.getKey();
                List<TestCaseExecutionInstance> instances = entry.getValue();
                
                // 取第一个实例的executionTaskId作为停止目标（因为同一执行机的任务通常使用相同的taskId）
                String executionTaskId = instances.get(0).getExecutionTaskId();
                
                try {
                    boolean cancelled = caseExecuteServiceClient.cancelTaskExecution(executorIp, executionTaskId);
                    if (cancelled) {
                        log.info("成功取消执行机任务 - 任务ID: {}, 执行机IP: {}, 执行任务ID: {}, 影响例次数: {}", 
                                id, executorIp, executionTaskId, instances.size());
                    } else {
                        log.warn("取消执行机任务失败 - 任务ID: {}, 执行机IP: {}, 执行任务ID: {}", 
                                id, executorIp, executionTaskId);
                    }
                } catch (Exception e) {
                    log.error("调用CaseExecuteService取消任务异常 - 任务ID: {}, 执行机IP: {}, 执行任务ID: {}, 错误: {}", 
                            id, executorIp, executionTaskId, e.getMessage());
                }
            }
            
            // 更新所有RUNNING状态的例次为STOPPED
            for (TestCaseExecutionInstance instance : runningInstances) {
                testCaseExecutionInstanceService.updateExecutionStatus(instance.getId(), "STOPPED", instance.getExecutionTaskId());
            }
            
            // 更新采集任务和用例执行例次的执行进度
            updateTaskExecutionProgress(id);
            
            log.info("任务停止成功 - 任务ID: {}, 更新例次数量: {}", id, runningInstances.size());
            return Result.success(true);
            
        } catch (Exception e) {
            log.error("停止任务失败 - 任务ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error("停止任务失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/pause")
    public Result<Boolean> pauseTask(@PathVariable @NotNull Long id) {
        log.info("暂停任务 - 任务ID: {}", id);
        
        try {
            CollectTask collectTask = collectTaskService.getById(id);
            if (collectTask == null) {
                return Result.error("任务不存在");
            }
            
            // 更新任务状态
            boolean taskUpdated = collectTaskService.updateTaskStatus(id, "PAUSED");
            if (!taskUpdated) {
                return Result.error("任务状态更新失败");
            }
            
            // 更新所有RUNNING状态的用例执行例次为PAUSED
            List<TestCaseExecutionInstance> runningInstances = testCaseExecutionInstanceService.getByCollectTaskIdAndStatus(id, "RUNNING");
            for (TestCaseExecutionInstance instance : runningInstances) {
                testCaseExecutionInstanceService.updateExecutionStatus(instance.getId(), "PAUSED", instance.getExecutionTaskId());
            }
            
            log.info("任务暂停成功 - 任务ID: {}, 更新例次数量: {}", id, runningInstances.size());
            return Result.success(true);
            
        } catch (Exception e) {
            log.error("暂停任务失败 - 任务ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error("暂停任务失败: " + e.getMessage());
        }
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
        log.info("更新任务状态 - 任务ID: {}, 新状态: {}", id, status);
        
        try {
            CollectTask collectTask = collectTaskService.getById(id);
            if (collectTask == null) {
                return Result.error("任务不存在");
            }
            
            // 验证状态转换是否有效
            if (!isValidStatusTransition(collectTask.getStatus(), status)) {
                return Result.error("无效的状态转换: " + collectTask.getStatus() + " -> " + status);
            }
            
            // 更新任务状态
            boolean taskUpdated = collectTaskService.updateTaskStatus(id, status);
            if (!taskUpdated) {
                return Result.error("任务状态更新失败");
            }
            
            // 根据状态转换更新相关的用例执行例次状态
            updateExecutionInstancesStatus(id, collectTask.getStatus(), status);
            
            log.info("任务状态更新成功 - 任务ID: {}, 状态: {} -> {}", id, collectTask.getStatus(), status);
            return Result.success(true);
            
        } catch (Exception e) {
            log.error("更新任务状态失败 - 任务ID: {}, 新状态: {}, 错误: {}", id, status, e.getMessage(), e);
            return Result.error("更新任务状态失败: " + e.getMessage());
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
            String targetInstanceStatus = null;
            String sourceInstanceStatus = null;
            
            // 根据状态转换确定需要更新的例次状态
            if ("PENDING".equals(oldStatus) && "RUNNING".equals(newStatus)) {
                sourceInstanceStatus = "PENDING";
                targetInstanceStatus = "RUNNING";
            } else if ("RUNNING".equals(oldStatus) && "STOPPED".equals(newStatus)) {
                sourceInstanceStatus = "RUNNING";
                targetInstanceStatus = "STOPPED";
            } else if ("RUNNING".equals(oldStatus) && "PAUSED".equals(newStatus)) {
                sourceInstanceStatus = "RUNNING";
                targetInstanceStatus = "PAUSED";
            } else if ("PAUSED".equals(oldStatus) && "RUNNING".equals(newStatus)) {
                sourceInstanceStatus = "PAUSED";
                targetInstanceStatus = "RUNNING";
            } else if ("STOPPED".equals(oldStatus) && "PENDING".equals(newStatus)) {
                sourceInstanceStatus = "STOPPED";
                targetInstanceStatus = "PENDING";
            } else if ("STOPPED".equals(oldStatus) && "RUNNING".equals(newStatus)) {
                sourceInstanceStatus = "STOPPED";
                targetInstanceStatus = "RUNNING";
            }
            
            if (sourceInstanceStatus != null && targetInstanceStatus != null) {
                List<TestCaseExecutionInstance> instances = testCaseExecutionInstanceService.getByCollectTaskIdAndStatus(taskId, sourceInstanceStatus);
                for (TestCaseExecutionInstance instance : instances) {
                    testCaseExecutionInstanceService.updateExecutionStatus(instance.getId(), targetInstanceStatus, null);
                }
                log.info("更新用例执行例次状态 - 任务ID: {}, 例次数量: {}, 状态: {} -> {}", 
                        taskId, instances.size(), sourceInstanceStatus, targetInstanceStatus);
            }
            
        } catch (Exception e) {
            log.error("更新用例执行例次状态失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
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
        
        log.info("开始获取可用逻辑环境列表 - 策略ID: {}, 地域筛选: regionId={}, countryId={}, provinceId={}, cityId={}", 
                strategyId, regionId, countryId, provinceId, cityId);
        
        try {
            // 1. 获取采集策略信息
            log.info("步骤1: 获取采集策略信息 - 策略ID: {}", strategyId);
            CollectStrategy strategy = collectStrategyService.getById(strategyId);
            if (strategy == null) {
                log.error("采集策略不存在 - 策略ID: {}", strategyId);
                return Result.error("采集策略不存在");
            }
            log.info("获取到采集策略: {} (用例集ID: {})", strategy.getName(), strategy.getTestCaseSetId());
            
            // 2. 获取策略关联的用例集的所有测试用例（基于筛选条件）
            log.info("步骤2: 获取策略关联的测试用例 - 用例集ID: {}, 筛选条件: 业务大类={}, APP={}", 
                    strategy.getTestCaseSetId(), strategy.getBusinessCategory(), strategy.getApp());
            List<TestCase> allTestCases = testCaseService.getByTestCaseSetId(strategy.getTestCaseSetId());
            log.info("获取到原始测试用例数量: {}", allTestCases.size());
            
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
            log.info("筛选后的测试用例数量: {}", testCases.size());
            
            // 3. 提取测试用例中的环境组网列表A
            log.info("步骤3: 提取测试用例中的环境组网需求");
            Set<String> requiredNetworks = new HashSet<>();
            for (TestCase testCase : testCases) {
                log.debug("处理测试用例: {} (编号: {})", testCase.getName(), testCase.getNumber());
                if (testCase.getLogicNetwork() != null && !testCase.getLogicNetwork().trim().isEmpty()) {
                    String[] networks = testCase.getLogicNetwork().split(";");
                    log.debug("测试用例 {} 的环境组网需求: {}", testCase.getNumber(), testCase.getLogicNetwork());
                    for (String network : networks) {
                        String trimmedNetwork = network.trim();
                        requiredNetworks.add(trimmedNetwork);
                        log.debug("添加环境组网需求: {}", trimmedNetwork);
                    }
                } else {
                    log.debug("测试用例 {} 没有环境组网需求", testCase.getNumber());
                }
            }
            log.info("提取的环境组网需求列表A: {}", requiredNetworks);
            
            // 4. 根据环境筛选条件获取可用的执行机
            log.info("步骤4: 根据地域筛选条件获取执行机 - regionId={}, countryId={}, provinceId={}, cityId={}", 
                    regionId, countryId, provinceId, cityId);
            List<Executor> availableExecutors = executorService.getExecutorsByRegion(
                regionId, countryId, provinceId, cityId);
            log.info("获取到符合条件的执行机数量: {}", availableExecutors.size());
            for (Executor executor : availableExecutors) {
                log.debug("匹配的执行机: {} (ID: {}, IP: {})", executor.getName(), executor.getId(), executor.getIpAddress());
            }
            
            // 5. 获取执行机关联的逻辑环境列表B
            log.info("步骤5: 获取执行机关联的逻辑环境");
            List<LogicEnvironment> allLogicEnvironments = new ArrayList<>();
            for (Executor executor : availableExecutors) {
                log.debug("获取执行机 {} 关联的逻辑环境", executor.getName());
                List<LogicEnvironment> environments = logicEnvironmentService.getByExecutorId(executor.getId());
                log.debug("执行机 {} 关联的逻辑环境数量: {}", executor.getName(), environments.size());
                for (LogicEnvironment env : environments) {
                    log.debug("执行机 {} 关联的逻辑环境: {} (ID: {})", executor.getName(), env.getName(), env.getId());
                }
                allLogicEnvironments.addAll(environments);
            }
            log.info("获取到所有逻辑环境数量: {}", allLogicEnvironments.size());
            
            // 6. 筛选逻辑环境，检查其环境组网是否存在于环境组网列表A中
            log.info("步骤6: 筛选匹配的逻辑环境");
            List<LogicEnvironmentDTO> availableLogicEnvironments = new ArrayList<>();
            for (LogicEnvironment logicEnvironment : allLogicEnvironments) {
                log.debug("检查逻辑环境: {} (ID: {})", logicEnvironment.getName(), logicEnvironment.getId());
                
                // 获取逻辑环境关联的环境组网
                List<LogicEnvironmentNetwork> environmentNetworks = logicEnvironmentNetworkService.getByLogicEnvironmentId(logicEnvironment.getId());
                log.debug("逻辑环境 {} 关联的环境组网数量: {}", logicEnvironment.getName(), environmentNetworks.size());
                
                // 检查是否有匹配的环境组网
                boolean hasMatchingNetwork = false;
                List<String> logicEnvironmentNetworks = new ArrayList<>();
                for (LogicEnvironmentNetwork envNetwork : environmentNetworks) {
                    LogicNetwork network = logicNetworkService.getById(envNetwork.getLogicNetworkId());
                    if (network != null) {
                        logicEnvironmentNetworks.add(network.getName());
                        if (requiredNetworks.contains(network.getName())) {
                            hasMatchingNetwork = true;
                            log.debug("找到匹配的环境组网: {} (逻辑环境: {})", network.getName(), logicEnvironment.getName());
                        }
                    }
                }
                log.debug("逻辑环境 {} 的环境组网: {}", logicEnvironment.getName(), logicEnvironmentNetworks);
                
                // 如果有匹配的环境组网，则添加到可用列表
                if (hasMatchingNetwork) {
                    log.info("逻辑环境 {} 匹配成功，添加到可用列表", logicEnvironment.getName());
                    LogicEnvironmentDTO dto = logicEnvironmentService.getLogicEnvironmentDTO(logicEnvironment.getId());
                    availableLogicEnvironments.add(dto);
                } else {
                    log.debug("逻辑环境 {} 不匹配，跳过", logicEnvironment.getName());
                }
            }
            
            log.info("匹配完成 - 可用逻辑环境数量: {}", availableLogicEnvironments.size());
            for (LogicEnvironmentDTO dto : availableLogicEnvironments) {
                log.debug("可用逻辑环境: {} (ID: {})", dto.getName(), dto.getId());
            }
            
            return Result.success(availableLogicEnvironments);
            
        } catch (Exception e) {
            log.error("获取可用逻辑环境列表失败", e);
            return Result.error("获取可用逻辑环境列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务执行进度
     */
    @GetMapping("/{id}/progress")
    public Result<Map<String, Object>> getTaskProgress(@PathVariable @NotNull Long id) {
        log.info("获取任务执行进度 - 任务ID: {}", id);
        
        try {
            CollectTask task = collectTaskService.getById(id);
            if (task == null) {
                return Result.error("任务不存在");
            }
            
            // 获取执行例次列表，基于真实数据计算进度
            List<TestCaseExecutionInstance> instances = testCaseExecutionInstanceService.getByCollectTaskId(id);
            
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
            
            return Result.success(progress);
        } catch (Exception e) {
            log.error("获取任务执行进度失败 - 任务ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error("获取任务执行进度失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务执行例次列表
     */
    @GetMapping("/{id}/execution-instances")
    public Result<List<Map<String, Object>>> getExecutionInstances(@PathVariable @NotNull Long id) {
        log.info("获取任务执行例次列表 - 任务ID: {}", id);
        
        try {
            CollectTask task = collectTaskService.getById(id);
            if (task == null) {
                return Result.error("任务不存在");
            }
            
            // 获取执行例次列表
            List<TestCaseExecutionInstance> instances = testCaseExecutionInstanceService.getByCollectTaskId(id);
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (TestCaseExecutionInstance instance : instances) {
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
                
                result.add(instanceMap);
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取任务执行例次列表失败 - 任务ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error("获取任务执行例次列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新任务执行进度
     * 
     * @param taskId 任务ID
     */
    private void updateTaskExecutionProgress(Long taskId) {
        try {
            log.info("开始更新任务执行进度 - 任务ID: {}", taskId);
            
            // 获取任务的所有用例执行例次
            List<TestCaseExecutionInstance> instances = testCaseExecutionInstanceService.getByCollectTaskId(taskId);
            
            if (instances.isEmpty()) {
                log.warn("任务没有用例执行例次 - 任务ID: {}", taskId);
                return;
            }
            
            // 统计各种状态和结果的数量
            int totalCount = instances.size();
            int successCount = 0;
            int failedCount = 0;
            int blockedCount = 0;
            int stoppedCount = 0;
            int runningCount = 0;
            
            for (TestCaseExecutionInstance instance : instances) {
                String status = instance.getStatus();
                String result = instance.getResult();
                
                // 统计状态
                switch (status) {
                    case "RUNNING":
                        runningCount++;
                        break;
                    case "STOPPED":
                        stoppedCount++;
                        break;
                    case "COMPLETED":
                        // 根据执行结果统计
                        if ("SUCCESS".equals(result)) {
                            successCount++;
                        } else if ("FAILED".equals(result)) {
                            failedCount++;
                        } else if ("BLOCKED".equals(result)) {
                            blockedCount++;
                        }
                        break;
                    default:
                        // 其他状态（如PENDING）不计入统计
                        break;
                }
            }
            
            log.info("任务执行进度统计 - 任务ID: {}, 总数: {}, 成功: {}, 失败: {}, 阻塞: {}, 停止: {}, 执行中: {}", 
                    taskId, totalCount, successCount, failedCount, blockedCount, stoppedCount, runningCount);
            
            // 更新采集任务的执行进度
            boolean progressUpdated = collectTaskService.updateTaskProgress(taskId, totalCount, successCount, failedCount);
            if (progressUpdated) {
                log.info("采集任务执行进度更新成功 - 任务ID: {}", taskId);
            } else {
                log.error("采集任务执行进度更新失败 - 任务ID: {}", taskId);
            }
            
        } catch (Exception e) {
            log.error("更新任务执行进度异常 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
        }
    }
}
