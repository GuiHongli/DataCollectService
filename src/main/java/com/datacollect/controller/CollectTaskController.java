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
        CollectTask collectTask = collectTaskService.getById(id);
        if (collectTask != null) {
            collectTask.setStatus("RUNNING"); // 运行中
            collectTask.setStartTime(LocalDateTime.now());
            collectTaskService.updateById(collectTask);
            return Result.success(true);
        }
        return Result.error("任务不存在");
    }

    @PostMapping("/{id}/stop")
    public Result<Boolean> stopTask(@PathVariable @NotNull Long id) {
        CollectTask collectTask = collectTaskService.getById(id);
        if (collectTask != null) {
            collectTask.setStatus("STOPPED"); // 停止
            collectTaskService.updateById(collectTask);
            return Result.success(true);
        }
        return Result.error("任务不存在");
    }

    @PostMapping("/{id}/pause")
    public Result<Boolean> pauseTask(@PathVariable @NotNull Long id) {
        CollectTask collectTask = collectTaskService.getById(id);
        if (collectTask != null) {
            collectTask.setStatus("PAUSED"); // 暂停
            collectTaskService.updateById(collectTask);
            return Result.success(true);
        }
        return Result.error("任务不存在");
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
            
            // 2. 获取策略关联的用例集的所有测试用例
            log.info("步骤2: 获取策略关联的测试用例 - 用例集ID: {}", strategy.getTestCaseSetId());
            List<TestCase> testCases = testCaseService.getByTestCaseSetId(strategy.getTestCaseSetId());
            log.info("获取到测试用例数量: {}", testCases.size());
            
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
            
            Map<String, Object> progress = new HashMap<>();
            progress.put("totalCount", task.getTotalTestCaseCount());
            progress.put("successCount", task.getSuccessTestCaseCount());
            progress.put("failedCount", task.getFailedTestCaseCount());
            progress.put("runningCount", task.getTotalTestCaseCount() - task.getSuccessTestCaseCount() - task.getFailedTestCaseCount());
            
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
}
