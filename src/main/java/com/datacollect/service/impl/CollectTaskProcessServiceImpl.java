package com.datacollect.service.impl;

import com.datacollect.dto.CollectTaskRequest;
import com.datacollect.dto.TestCaseExecutionRequest;
import com.datacollect.entity.CollectTask;
import com.datacollect.entity.CollectStrategy;
import com.datacollect.entity.TestCase;
import com.datacollect.entity.TestCaseSet;
import com.datacollect.entity.TestCaseExecutionInstance;
import com.datacollect.entity.dto.LogicEnvironmentDTO;
import com.datacollect.service.CollectTaskProcessService;
import com.datacollect.service.CollectTaskService;
import com.datacollect.service.CollectStrategyService;
import com.datacollect.service.TestCaseExecutionInstanceService;
import com.datacollect.service.TestCaseService;
import com.datacollect.service.TestCaseSetService;
import com.datacollect.service.LogicEnvironmentService;
import com.datacollect.service.ExecutorService;
import com.datacollect.service.ExecutorMacAddressService;
import com.datacollect.service.LogicEnvironmentUeService;
import com.datacollect.service.UeService;
import com.datacollect.service.NetworkTypeService;
import com.datacollect.service.ExternalApiService;
import com.datacollect.service.ExecutorWebSocketService;
import com.datacollect.entity.LogicEnvironmentUe;
import com.datacollect.entity.Ue;
import com.datacollect.entity.NetworkType;
import com.datacollect.entity.Executor;
import com.datacollect.entity.ExecutorMacAddress;
import com.datacollect.entity.LogicEnvironment;
import com.datacollect.common.exception.CollectTaskException;
import com.datacollect.util.HttpClientUtil;
import com.datacollect.dto.AppCheckRequest;
import com.datacollect.dto.AppCheckResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.HashSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 采集任务处理服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class CollectTaskProcessServiceImpl implements CollectTaskProcessService {

    @Autowired
    private CollectTaskService collectTaskService;
    
    @Autowired
    private CollectStrategyService collectStrategyService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private TestCaseExecutionInstanceService testCaseExecutionInstanceService;
    
    @Autowired
    private TestCaseService testCaseService;
    
    @Autowired
    private TestCaseSetService testCaseSetService;
    
    @Autowired
    private LogicEnvironmentService logicEnvironmentService;
    
    @Autowired
    private ExecutorService executorService;
    
    @Autowired
    private ExecutorMacAddressService executorMacAddressService;
    
    @Autowired
    private HttpClientUtil httpClientUtil;
    
    @Autowired
    private LogicEnvironmentUeService logicEnvironmentUeService;
    
    @Autowired
    private UeService ueService;
    
    @Autowired
    private NetworkTypeService networkTypeService;
    
    @Autowired
    private ExternalApiService externalApiService;
    
    @Autowired
    private ExecutorWebSocketService executorWebSocketService;
    
    @Value("${datacollect.service.base-url:http://localhost:8080}")
    private String dataCollectServiceBaseUrl;
    
    /**
     * 用例配置缓存（key: collectTaskId, value: Map<testCaseId, TestCaseConfig>）
     * 用于存储任务级别的用例配置（执行次数和自定义参数）
     */
    private final java.util.concurrent.ConcurrentHashMap<Long, Map<Long, TestCaseConfig>> testCaseConfigCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * 用例配置内部类
     */
    @lombok.Data
    private static class TestCaseConfig {
        private Long testCaseId;
        private Integer executionCount;
        private List<Map<String, Object>> customParams; // [{"key": "k", "value": ["v1", "v2"]}]
    }

    @Override
    public Long processCollectTaskCreation(CollectTaskRequest request, String createBy) {
        log.info("Start processing collect task creation - task name: {}, createBy: {}", request.getName(), createBy);
        
        try {
            // 1. 记录采集任务本身的信息
            Long collectTaskId = collectTaskService.createCollectTask(request, createBy);
            log.info("Collect task created successfully - task ID: {}", collectTaskId);
            
            // 2. 解析用例配置（如果提供）
            Map<Long, TestCaseConfig> testCaseConfigMap = parseTestCaseConfigs(request.getCustomParams());
            
            // 3. 获取要下发的用例ID列表（优先使用customParams中的用例，如果没有则从策略筛选）
            CollectTask collectTask = collectTaskService.getCollectTaskById(collectTaskId);
            List<Long> testCaseIds = getTestCaseIds(collectTask, request, testCaseConfigMap);
            
            // 4. 组装用例执行例次列表（根据用例配置中的执行次数）
            List<TestCaseExecutionInstance> instances = assembleTestCaseInstances(collectTaskId, testCaseIds, request.getCollectCount(), testCaseConfigMap);
            log.info("Test case execution instances assembled - instance count: {} - task ID: {}", instances.size(), collectTaskId);
            
            // 5. 分配用例执行例次到逻辑环境
            List<TestCaseExecutionInstance> distributedInstances = distributeInstancesToEnvironments(instances, request.getLogicEnvironmentIds());
            log.info("Test case execution instances distribution completed - task ID: {}", collectTaskId);
            
            // 6. 保存用例执行例次
            saveTestCaseInstances(distributedInstances, collectTaskId);
            
            // 7. 更新任务总用例数
            collectTaskService.updateTaskProgress(collectTaskId, instances.size(), 0, 0);
            
            // 8. 保存用例配置到任务中（用于后续获取用例自定义参数）
            // 将用例配置存储到CollectTask中，以便后续使用
            storeTestCaseConfigs(collectTaskId, testCaseConfigMap);
            
            // 9. 异步调用执行机服务
            callExecutorServicesAsync(distributedInstances, collectTaskId);
            
            return collectTaskId;
            
        } catch (Exception e) {
            log.error("Failed to process collect task creation - task name: {}, error: {}", request.getName(), e.getMessage(), e);
            throw new CollectTaskException("COLLECT_TASK_CREATE_FAILED", "Failed to process collect task creation: " + e.getMessage(), e);
        }
    }

    /**
     * 获取要下发的测试用例ID列表
     * 优先使用customParams中配置的用例ID，如果没有提供则从策略中筛选
     * 
     * @param collectTask 采集任务
     * @param request 采集任务请求
     * @param testCaseConfigMap 用例配置Map
     * @return 用例ID列表
     */
    private List<Long> getTestCaseIds(CollectTask collectTask, CollectTaskRequest request, Map<Long, TestCaseConfig> testCaseConfigMap) {
        // 如果提供了用例配置，则使用配置中的用例ID
        if (testCaseConfigMap != null && !testCaseConfigMap.isEmpty()) {
            List<Long> testCaseIds = new ArrayList<>(testCaseConfigMap.keySet());
            log.info("Using test case IDs from customParams config - task ID: {}, test case count: {}", 
                    collectTask.getId(), testCaseIds.size());
            return testCaseIds;
        }
        
        // 如果没有提供用例配置，则从策略中筛选（兼容旧逻辑）
        log.info("No customParams config provided, falling back to strategy filtering - task ID: {}", collectTask.getId());
        return getFilteredTestCaseIds(collectTask, request);
    }
    
    /**
     * 获取筛选后的测试用例ID列表（从策略中筛选）
     */
    private List<Long> getFilteredTestCaseIds(CollectTask collectTask, CollectTaskRequest request) {
        // 获取策略信息
        CollectStrategy strategy = collectStrategyService.getById(collectTask.getCollectStrategyId());
        if (strategy == null) {
            log.error("Collect strategy not found - strategy ID: {}", collectTask.getCollectStrategyId());
            throw new CollectTaskException("COLLECT_STRATEGY_NOT_FOUND", "采集策略不存在");
        }
        
        // 获取用例集中的所有用例
        List<TestCase> allTestCases = testCaseService.getByTestCaseSetId(collectTask.getTestCaseSetId());
        
        // 根据策略的筛选条件过滤用例
        List<TestCase> filteredTestCases = filterTestCasesByStrategy(allTestCases, strategy);
        
        List<Long> testCaseIds = filteredTestCases.stream()
            .map(TestCase::getId)
            .collect(java.util.stream.Collectors.toList());
            
        log.info("Filtered test cases count: {} (original count: {}) - task ID: {}, filter criteria: business category={}, APP={}", 
                testCaseIds.size(), allTestCases.size(), collectTask.getId(), 
                strategy.getBusinessCategory(), strategy.getApp());
                
        return testCaseIds;
    }

    /**
     * 根据策略筛选测试用例
     */
    private List<TestCase> filterTestCasesByStrategy(List<TestCase> allTestCases, CollectStrategy strategy) {
        return allTestCases.stream()
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
    }

    /**
     * 解析用例配置JSON字符串
     * 
     * @param testCaseConfigsJson 用例配置JSON字符串
     * @return 用例配置Map（key: testCaseId, value: TestCaseConfig）
     */
    private Map<Long, TestCaseConfig> parseTestCaseConfigs(String testCaseConfigsJson) {
        Map<Long, TestCaseConfig> configMap = new HashMap<>();
        
        if (testCaseConfigsJson == null || testCaseConfigsJson.trim().isEmpty()) {
            log.debug("TestCase configs is empty, using default configuration");
            return configMap;
        }
        
        try {
            // 解析JSON数组：[{"testCaseId": 1, "executionCount": 2, "customParams": [...]}]
            List<Map<String, Object>> configList = objectMapper.readValue(
                testCaseConfigsJson, 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            for (Map<String, Object> configObj : configList) {
                TestCaseConfig config = new TestCaseConfig();
                
                // 解析testCaseId
                Object testCaseIdObj = configObj.get("testCaseId");
                if (testCaseIdObj != null) {
                    Long testCaseId = testCaseIdObj instanceof Number 
                        ? ((Number) testCaseIdObj).longValue() 
                        : Long.parseLong(String.valueOf(testCaseIdObj));
                    config.setTestCaseId(testCaseId);
                    
                    // 解析executionCount
                    Object executionCountObj = configObj.get("executionCount");
                    if (executionCountObj != null) {
                        Integer executionCount = executionCountObj instanceof Number 
                            ? ((Number) executionCountObj).intValue() 
                            : Integer.parseInt(String.valueOf(executionCountObj));
                        config.setExecutionCount(executionCount);
                    }
                    
                    // 解析customParams
                    Object customParamsObj = configObj.get("customParams");
                    if (customParamsObj != null) {
                        // customParams 应该是 List<Map<String, Object>> 格式
                        if (customParamsObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> customParamsList = (List<Map<String, Object>>) customParamsObj;
                            config.setCustomParams(customParamsList);
                        }
                    }
                    
                    configMap.put(testCaseId, config);
                    log.debug("Parsed test case config - testCaseId: {}, executionCount: {}, customParams: {}", 
                            testCaseId, config.getExecutionCount(), config.getCustomParams());
                }
            }
            
            log.info("Parsed test case configs - config count: {}", configMap.size());
        } catch (Exception e) {
            log.error("Failed to parse test case configs JSON: {}", e.getMessage(), e);
            // 解析失败时返回空Map，使用默认配置
        }
        
        return configMap;
    }
    
    /**
     * 存储用例配置到缓存
     * 
     * @param collectTaskId 采集任务ID
     * @param testCaseConfigMap 用例配置Map
     */
    private void storeTestCaseConfigs(Long collectTaskId, Map<Long, TestCaseConfig> testCaseConfigMap) {
        if (testCaseConfigMap != null && !testCaseConfigMap.isEmpty()) {
            testCaseConfigCache.put(collectTaskId, testCaseConfigMap);
            log.info("Stored test case configs to cache - task ID: {}, config count: {}", collectTaskId, testCaseConfigMap.size());
        }
    }
    
    /**
     * 保存测试用例执行实例
     */
    private void saveTestCaseInstances(List<TestCaseExecutionInstance> distributedInstances, Long collectTaskId) {
        boolean saveSuccess = testCaseExecutionInstanceService.batchSaveInstances(distributedInstances);
        if (!saveSuccess) {
            throw new CollectTaskException("TEST_CASE_INSTANCE_SAVE_FAILED", "保存用例执行例次失败");
        }
        log.info("Test case execution instances saved successfully - task ID: {}", collectTaskId);
    }

    /**
     * 异步调用执行机服务
     */
    private void callExecutorServicesAsync(List<TestCaseExecutionInstance> distributedInstances, Long collectTaskId) {
        CompletableFuture.runAsync(() -> {
            try {
                boolean callSuccess = callExecutorServices(distributedInstances);
                if (!callSuccess) {
                    collectTaskService.updateTaskStatus(collectTaskId, "STOPPED");
                    collectTaskService.updateTaskFailureReason(collectTaskId, "执行机服务调用失败");
                    log.error("Executor service call failed - task ID: {}", collectTaskId);
                } else {
                    log.info("Executor service call successful - task ID: {}", collectTaskId);
                }
            } catch (Exception e) {
                collectTaskService.updateTaskStatus(collectTaskId, "STOPPED");
                collectTaskService.updateTaskFailureReason(collectTaskId, "执行机服务调用异常: " + e.getMessage());
                                    log.error("Executor service call exception - task ID: {}, error: {}", collectTaskId, e.getMessage(), e);
            }
        });
    }

    @Override
    public List<TestCaseExecutionInstance> assembleTestCaseInstances(Long collectTaskId, List<Long> testCaseIds, Integer collectCount) {
        // 兼容旧接口，使用默认配置
        return assembleTestCaseInstances(collectTaskId, testCaseIds, collectCount, new HashMap<>());
    }
    
    /**
     * 组装用例执行例次列表（支持用例配置）
     * 
     * @param collectTaskId 采集任务ID
     * @param testCaseIds 测试用例ID列表
     * @param collectCount 默认采集次数（如果用例配置中没有指定，则使用此值）
     * @param testCaseConfigMap 用例配置Map（key: testCaseId, value: TestCaseConfig）
     * @return 用例执行例次列表
     */
    private List<TestCaseExecutionInstance> assembleTestCaseInstances(Long collectTaskId, List<Long> testCaseIds, Integer collectCount, Map<Long, TestCaseConfig> testCaseConfigMap) {
        log.info("Start assembling test case execution instances - task ID: {}, test case count: {}, default collect count: {}, config count: {}", 
                collectTaskId, testCaseIds.size(), collectCount, testCaseConfigMap.size());
        
        List<TestCaseExecutionInstance> instances = new ArrayList<>();
        
        for (Long testCaseId : testCaseIds) {
            // 从用例配置中获取执行次数，如果没有配置则使用默认值
            TestCaseConfig config = testCaseConfigMap.get(testCaseId);
            int executionCount = (config != null && config.getExecutionCount() != null) ? config.getExecutionCount() : collectCount;
            
            for (int round = 1; round <= executionCount; round++) {
                TestCaseExecutionInstance instance = new TestCaseExecutionInstance();
                instance.setCollectTaskId(collectTaskId);
                instance.setTestCaseId(testCaseId);
                instance.setRound(round);
                instance.setStatus("RUNNING");
                instances.add(instance);
            }
            
            log.debug("Assembled instances for test case {} - execution count: {}", testCaseId, executionCount);
        }
        
        log.info("Test case execution instances assembly completed - instance count: {} - task ID: {}", instances.size(), collectTaskId);
        return instances;
    }

    @Override
    public List<TestCaseExecutionInstance> distributeInstancesToEnvironments(List<TestCaseExecutionInstance> instances, List<Long> logicEnvironmentIds) {
        log.info("Start distributing test case execution instances to logic environments - instance count: {}, logic environment count: {}", instances.size(), logicEnvironmentIds.size());
        
        validateLogicEnvironments(logicEnvironmentIds);
        
        // 获取逻辑环境关联的执行机IP
        Map<Long, String> environmentToExecutorMap = getEnvironmentToExecutorMap(logicEnvironmentIds);
        
        // 均分分配用例执行例次到逻辑环境
        distributeInstancesEvenly(instances, logicEnvironmentIds, environmentToExecutorMap);
        
        log.info("Test case execution instances distribution completed - task ID: {}", instances.get(0).getCollectTaskId());
        return instances;
    }

    /**
     * 验证逻辑环境列表
     */
    private void validateLogicEnvironments(List<Long> logicEnvironmentIds) {
        if (logicEnvironmentIds.isEmpty()) {
            log.error("Logic environment list is empty, cannot distribute test case execution instances");
            throw new CollectTaskException("LOGIC_ENVIRONMENT_EMPTY", "逻辑环境列表为空");
        }
    }

    /**
     * 获取环境到执行机的映射（通过MAC地址查找执行机IP）
     */
    private Map<Long, String> getEnvironmentToExecutorMap(List<Long> logicEnvironmentIds) {
        Map<Long, String> environmentToExecutorMap = new HashMap<>();
        for (Long logicEnvironmentId : logicEnvironmentIds) {
            LogicEnvironmentDTO logicEnvironmentDTO = logicEnvironmentService.getLogicEnvironmentDTO(logicEnvironmentId);
            if (logicEnvironmentDTO != null && logicEnvironmentDTO.getExecutorId() != null) {
                // 通过执行机ID获取MAC地址，然后通过MAC地址查找执行机IP
                String executorIp = getExecutorIpByMacAddress(logicEnvironmentDTO.getExecutorId());
                if (executorIp != null) {
                    environmentToExecutorMap.put(logicEnvironmentId, executorIp);
                } else {
                    // 如果没有MAC地址，则使用原来的IP地址（兼容旧逻辑）
                    if (logicEnvironmentDTO.getExecutorIpAddress() != null) {
                        environmentToExecutorMap.put(logicEnvironmentId, logicEnvironmentDTO.getExecutorIpAddress());
                        log.warn("执行机未关联MAC地址，使用IP地址 - 执行机ID: {}, IP: {}", 
                                logicEnvironmentDTO.getExecutorId(), logicEnvironmentDTO.getExecutorIpAddress());
                    }
                }
            }
        }
        
        if (environmentToExecutorMap.isEmpty()) {
            log.error("No available executor IP found");
            throw new CollectTaskException("EXECUTOR_IP_NOT_FOUND", "未找到可用的执行机IP");
        }
        
        return environmentToExecutorMap;
    }
    
    /**
     * 通过MAC地址查找执行机IP
     * 
     * @param executorId 执行机ID
     * @return 执行机IP地址
     */
    private String getExecutorIpByMacAddress(Long executorId) {
        try {
            // 获取执行机关联的MAC地址
            List<ExecutorMacAddress> macAddresses = executorMacAddressService.getByExecutorId(executorId);
            if (macAddresses != null && !macAddresses.isEmpty()) {
                // 使用第一个MAC地址（如果有多个，使用第一个）
                ExecutorMacAddress macAddress = macAddresses.get(0);
                String macAddressStr = macAddress.getMacAddress();
                
                // 通过MAC地址查找执行机（优先查找关联的执行机）
                if (macAddress.getExecutorId() != null) {
                    Executor executor = executorService.getById(macAddress.getExecutorId());
                    if (executor != null) {
                        log.info("通过MAC地址查找执行机IP - MAC地址: {}, 执行机ID: {}, IP: {}", 
                                macAddressStr, executorId, executor.getIpAddress());
                        return executor.getIpAddress();
                    }
                }
                
                // 如果没有关联执行机，通过MAC地址查找执行机（通过executor表的mac_address字段）
                QueryWrapper<Executor> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("mac_address", macAddressStr);
                queryWrapper.eq("deleted", 0);
                Executor executor = executorService.getOne(queryWrapper);
                if (executor != null) {
                    log.info("通过MAC地址查找执行机IP - MAC地址: {}, 执行机ID: {}, IP: {}", 
                            macAddressStr, executor.getId(), executor.getIpAddress());
                    return executor.getIpAddress();
                }
            }
            
            log.warn("执行机未关联MAC地址 - 执行机ID: {}", executorId);
            return null;
        } catch (Exception e) {
            log.error("通过MAC地址查找执行机IP失败 - 执行机ID: {}, 错误: {}", executorId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 均分分配实例到环境
     */
    private void distributeInstancesEvenly(List<TestCaseExecutionInstance> instances, List<Long> logicEnvironmentIds, Map<Long, String> environmentToExecutorMap) {
        int totalInstances = instances.size();
        int environmentCount = logicEnvironmentIds.size();
        int baseCount = totalInstances / environmentCount; // 每个环境的基础数量
        int remainder = totalInstances % environmentCount; // 剩余数量
        
        log.info("Distribution strategy - total instances: {}, logic environment count: {}, base allocation: {}, remainder: {}", 
                totalInstances, environmentCount, baseCount, remainder);
        
        int instanceIndex = 0;
        for (int i = 0; i < logicEnvironmentIds.size(); i++) {
            Long logicEnvironmentId = logicEnvironmentIds.get(i);
            String executorIp = environmentToExecutorMap.get(logicEnvironmentId);
            
            // 计算当前环境应分配的例次数量
            int currentEnvironmentCount = baseCount + (i < remainder ? 1 : 0);
            
            log.info("Logic environment {} (executor IP: {}) allocated {} instances", logicEnvironmentId, executorIp, currentEnvironmentCount);
            
            // 为当前环境分配例次
            for (int j = 0; j < currentEnvironmentCount; j++) {
                if (instanceIndex < instances.size()) {
                    TestCaseExecutionInstance instance = instances.get(instanceIndex);
                    instance.setLogicEnvironmentId(logicEnvironmentId);
                    instance.setExecutorIp(executorIp);
                    instanceIndex++;
                }
            }
        }
    }

    @Override
    public boolean callExecutorServices(List<TestCaseExecutionInstance> instances) {
        log.info("Start calling executor service - instance count: {}", instances.size());
        
        try {
            // 检查任务状态，如果任务已停止则不再继续下发
            if (!checkTaskStatus(instances)) {
                return false;
            }
            
            // 按执行机IP分组
            java.util.Map<String, List<TestCaseExecutionInstance>> instancesByExecutor = groupInstancesByExecutor(instances);
            
            // 为每个执行机创建执行任务
            return createExecutionTasksForExecutors(instancesByExecutor);
            
        } catch (Exception e) {
            log.error("Executor service call exception - error: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查任务状态
     */
    private boolean checkTaskStatus(List<TestCaseExecutionInstance> instances) {
        if (!instances.isEmpty()) {
            Long collectTaskId = instances.get(0).getCollectTaskId();
            CollectTask collectTask = collectTaskService.getCollectTaskById(collectTaskId);
            if (collectTask != null && "STOPPED".equals(collectTask.getStatus())) {
                log.warn("Task stopped, no longer continue to distribute test case execution - task ID: {}, status: {}", collectTaskId, collectTask.getStatus());
                return false;
            }
        }
        return true;
    }

    /**
     * 按执行机分组实例
     */
    private java.util.Map<String, List<TestCaseExecutionInstance>> groupInstancesByExecutor(List<TestCaseExecutionInstance> instances) {
        java.util.Map<String, List<TestCaseExecutionInstance>> instancesByExecutor = new java.util.HashMap<>();
        for (TestCaseExecutionInstance instance : instances) {
            instancesByExecutor.computeIfAbsent(instance.getExecutorIp(), k -> new ArrayList<>()).add(instance);
        }
        return instancesByExecutor;
    }

    /**
     * 为执行机创建执行任务
     */
    private boolean createExecutionTasksForExecutors(java.util.Map<String, List<TestCaseExecutionInstance>> instancesByExecutor) {
        for (java.util.Map.Entry<String, List<TestCaseExecutionInstance>> entry : instancesByExecutor.entrySet()) {
            String executorIp = entry.getKey();
            List<TestCaseExecutionInstance> executorInstances = entry.getValue();
            
            // 再次检查任务状态，确保在调用执行机前任务未被停止
            if (!checkTaskStatus(executorInstances)) {
                log.warn("Task stopped, skip test case distribution for executor {}", executorIp);
                continue;
            }
            
            boolean success = createExecutionTaskForExecutor(executorIp, executorInstances);
            if (!success) {
                log.error("Failed to create execution task for executor - executor IP: {}", executorIp);
                return false;
            }
        }
        
        log.info("Executor service call completed");
        return true;
    }
    
    private boolean createExecutionTaskForExecutor(String executorIp, List<TestCaseExecutionInstance> instances) {
        try {
            log.info("Create execution task for executor - executor IP: {}, instance count: {}", executorIp, instances.size());
            
            if (!checkTaskStatus(instances)) {
                log.warn("Task stopped, no longer create execution task for executor - executor IP: {}", executorIp);
                return false;
            }
            
            String taskId = generateTaskId(executorIp);
            TestCaseSetInfo testCaseSetInfo = getTestCaseSetInfo(instances);
            if (!validateTestCaseSetInfo(testCaseSetInfo, executorIp)) {
                return false;
            }
            
            List<TestCaseExecutionRequest.TestCaseInfo> testCaseList = buildTestCaseList(instances);
            List<TestCaseExecutionRequest.UeInfo> ueList = getExecutorUeList(executorIp);
            TestCaseExecutionRequest.CollectStrategyInfo collectStrategyInfo = getCollectStrategyInfo(testCaseSetInfo.getCollectStrategyId());
            String taskCustomParams = getTaskCustomParams(instances);
            
            logExecutorInfo(executorIp, ueList, collectStrategyInfo, testCaseSetInfo.getCollectStrategyId());
            
            TestCaseExecutionRequest request = buildExecutionRequest(taskId, executorIp, testCaseSetInfo, testCaseList, ueList, collectStrategyInfo, taskCustomParams);
            return sendExecutionRequest(request, instances, taskId, executorIp);
            
        } catch (Exception e) {
            log.error("Exception creating execution task for executor - executor IP: {}, error: {}", executorIp, e.getMessage(), e);
            return false;
        }
    }

    private String generateTaskId(String executorIp) {
        return "TASK_" + System.currentTimeMillis() + "_" + executorIp.replace(".", "_");
    }

    private void logExecutorInfo(String executorIp, List<TestCaseExecutionRequest.UeInfo> ueList, TestCaseExecutionRequest.CollectStrategyInfo collectStrategyInfo, Long collectStrategyId) {
        log.info("Get UE information associated with executor - executor IP: {}, UE count: {}", executorIp, ueList.size());
                log.info("Get collect strategy information - strategy ID: {}, strategy name: {}", collectStrategyId,
                collectStrategyInfo != null ? collectStrategyInfo.getName() : "Unknown");
    }

    /**
     * 用例集信息
     */
    private static class TestCaseSetInfo {
        private Long testCaseSetId;
        private String testCaseSetPath;
        private Long collectStrategyId;
        
        public Long getTestCaseSetId() { return testCaseSetId; }
        public void setTestCaseSetId(Long testCaseSetId) { this.testCaseSetId = testCaseSetId; }
        public String getTestCaseSetPath() { return testCaseSetPath; }
        public void setTestCaseSetPath(String testCaseSetPath) { this.testCaseSetPath = testCaseSetPath; }
        public Long getCollectStrategyId() { return collectStrategyId; }
        public void setCollectStrategyId(Long collectStrategyId) { this.collectStrategyId = collectStrategyId; }
    }

    /**
     * 获取用例集信息
     */
    private TestCaseSetInfo getTestCaseSetInfo(List<TestCaseExecutionInstance> instances) {
        TestCaseSetInfo info = new TestCaseSetInfo();
        
        if (instances.isEmpty()) {
            return info;
        }
        
        Long collectTaskId = instances.get(0).getCollectTaskId();
        CollectTask collectTask = collectTaskService.getCollectTaskById(collectTaskId);
        if (collectTask == null) {
            return info;
        }
        
        info.setTestCaseSetId(collectTask.getTestCaseSetId());
        info.setCollectStrategyId(collectTask.getCollectStrategyId());
        
        enrichTestCaseSetPath(info, collectTask.getTestCaseSetId());
        return info;
    }

    private void enrichTestCaseSetPath(TestCaseSetInfo info, Long testCaseSetId) {
        TestCaseSet testCaseSet = testCaseSetService.getById(testCaseSetId);
        if (testCaseSet == null) {
                            log.warn("Test case set not found - test case set ID: {}", testCaseSetId);
            return;
        }
        
        String testCaseSetPath = determineTestCaseSetPath(testCaseSet);
        info.setTestCaseSetPath(testCaseSetPath);
        log.info("Got test case set path - test case set ID: {}, path: {}", testCaseSetId, testCaseSetPath);
    }

    private String determineTestCaseSetPath(TestCaseSet testCaseSet) {
        String testCaseSetPath = testCaseSet.getGohttpserverUrl();
        if (testCaseSetPath == null || testCaseSetPath.trim().isEmpty()) {
            testCaseSetPath = testCaseSet.getFilePath();
        }
        return testCaseSetPath;
    }

    /**
     * 验证用例集信息
     */
    private boolean validateTestCaseSetInfo(TestCaseSetInfo testCaseSetInfo, String executorIp) {
        if (testCaseSetInfo.getTestCaseSetId() == null) {
            log.error("Cannot get test case set ID - executor IP: {}", executorIp);
            return false;
        }
        
        if (testCaseSetInfo.getTestCaseSetPath() == null || testCaseSetInfo.getTestCaseSetPath().trim().isEmpty()) {
            log.error("Cannot get test case set path - test case set ID: {}, executor IP: {}", testCaseSetInfo.getTestCaseSetId(), executorIp);
            return false;
        }
        
        return true;
    }

    /**
     * 构建用例列表
     */
    private List<TestCaseExecutionRequest.TestCaseInfo> buildTestCaseList(List<TestCaseExecutionInstance> instances) {
        List<TestCaseExecutionRequest.TestCaseInfo> testCaseList = new ArrayList<>();
        for (TestCaseExecutionInstance instance : instances) {
            // 获取用例编号
            TestCase testCase = testCaseService.getById(instance.getTestCaseId());
            if (testCase != null) {
                TestCaseExecutionRequest.TestCaseInfo testCaseInfo = 
                    new TestCaseExecutionRequest.TestCaseInfo();
                testCaseInfo.setTestCaseId(instance.getTestCaseId());
                testCaseInfo.setTestCaseNumber(testCase.getNumber());
                testCaseInfo.setRound(instance.getRound());
                testCaseList.add(testCaseInfo);
            } else {
                log.warn("Test case not found - test case ID: {}", instance.getTestCaseId());
            }
        }
        return testCaseList;
    }

    /**
     * 获取任务自定义参数（包含用例级别的自定义参数）
     */
    private String getTaskCustomParams(List<TestCaseExecutionInstance> instances) {
        if (instances.isEmpty()) {
            log.info("No instances to build custom params");
            return null;
        }

        Long collectTaskId = instances.get(0).getCollectTaskId();
        CollectTask collectTask = collectTaskService.getCollectTaskById(collectTaskId);
        if (collectTask == null) {
            log.info("Collect task not found when building custom params - task ID: {}", collectTaskId);
            return null;
        }

        // 准备用例ID集合
        java.util.Set<Long> caseIds = instances.stream()
            .map(TestCaseExecutionInstance::getTestCaseId)
            .collect(java.util.stream.Collectors.toSet());

        // 任务级参数（最高优先级）
        String taskCustomParams = collectTask.getCustomParams();
        List<Map<String, String>> taskParamsList = parseKeyValueArraySafely(taskCustomParams);
        if (taskCustomParams != null && !taskCustomParams.trim().isEmpty()) {
            log.info("Get collect task custom parameters - task ID: {}, custom parameters: {}", collectTaskId, taskCustomParams);
        }

        // 策略级参数（中优先级）
        List<Map<String, String>> strategyParamsList = new ArrayList<>();
        String filteredTestCaseParamsJson = null;
        Long collectStrategyId = collectTask.getCollectStrategyId();
        if (collectStrategyId != null) {
            CollectStrategy collectStrategy = collectStrategyService.getById(collectStrategyId);
            if (collectStrategy != null) {
                String strategyCustomParams = collectStrategy.getCustomParams();
                strategyParamsList = parseKeyValueArraySafely(strategyCustomParams);
                if (strategyCustomParams != null && !strategyCustomParams.trim().isEmpty()) {
                    log.info("Get collect strategy custom parameters - strategy ID: {}, custom parameters: {}", collectStrategyId, strategyCustomParams);
                }

                // 用例级参数（最低优先级，但最具体）- 仅保留当前实例涉及的用例
                String testCaseCustomParams = collectStrategy.getTestCaseCustomParams();
                if (testCaseCustomParams != null && !testCaseCustomParams.trim().isEmpty()) {
                    try {
                        filteredTestCaseParamsJson = filterTestCaseCustomParams(testCaseCustomParams, instances);
                        if (filteredTestCaseParamsJson != null && !filteredTestCaseParamsJson.trim().isEmpty()) {
                            log.info("Get test case custom parameters - strategy ID: {}, test case custom parameters: {}", collectStrategyId, filteredTestCaseParamsJson);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse test case custom parameters - strategy ID: {}, error: {}", collectStrategyId, e.getMessage());
                    }
                }
            }
        }

        // 将数组形式参数转换为map，便于优先级覆盖
        Map<String, String> taskParamMap = toKeyValueMap(taskParamsList);
        Map<String, String> strategyParamMap = toKeyValueMap(strategyParamsList);

        // 解析已过滤的用例级参数对象 {"caseId":[{"key":"k","value":"v"}...]}
        Map<String, List<Map<String, String>>> testCaseParamsById = parseTestCaseParamsObjectSafely(filteredTestCaseParamsJson);

        // 获取任务级别的用例自定义参数（从testCaseConfigs中）
        Map<String, List<Map<String, String>>> taskTestCaseParamsById = getTaskTestCaseCustomParams(collectTaskId, caseIds);

        // 为每个用例生成合并后的参数数组，并输出为 { caseId: [ {key,value}... ] }
        Map<String, List<Map<String, String>>> mergedByCase = new HashMap<>();
        
        // 获取用例信息并调用checkAppIsNew方法
        Map<Long, Boolean> appIsNewMap = getAppIsNewMap(instances);
        
        for (Long caseId : caseIds) {
            String caseIdStr = String.valueOf(caseId);
            
            // 策略级用例参数（从策略的testCaseCustomParams中获取）
            List<Map<String, String>> strategyTestCaseList = testCaseParamsById.getOrDefault(caseIdStr, new ArrayList<>());
            
            // 任务级用例参数（从testCaseConfigs中获取，最高优先级）
            List<Map<String, String>> taskTestCaseList = taskTestCaseParamsById.getOrDefault(caseIdStr, new ArrayList<>());
            
            Map<String, String> merged = new java.util.LinkedHashMap<>();

            // 合并顺序：策略级用例参数(最低) -> 策略级参数 -> 任务级参数 -> 任务级用例参数(最高)
            merged.putAll(toKeyValueMap(strategyTestCaseList));
            merged.putAll(strategyParamMap);
            merged.putAll(taskParamMap);
            merged.putAll(toKeyValueMap(taskTestCaseList)); // 任务级用例参数最后合并，覆盖前面的
            
            // 添加isnew参数
            Boolean isNew = appIsNewMap.get(caseId);
            if (isNew != null) {
                merged.put("isnew", isNew ? "1" : "0");
            }

            mergedByCase.put(caseIdStr, toKeyValueList(merged));
        }

        try {
            String result = objectMapper.writeValueAsString(mergedByCase);
            log.info("Combined custom parameters for task: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Failed to serialize combined custom parameters - error: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取任务级别的用例自定义参数
     * 
     * @param collectTaskId 采集任务ID
     * @param caseIds 用例ID集合
     * @return 用例自定义参数Map（key: caseId字符串, value: 参数列表）
     */
    private Map<String, List<Map<String, String>>> getTaskTestCaseCustomParams(Long collectTaskId, java.util.Set<Long> caseIds) {
        Map<String, List<Map<String, String>>> result = new HashMap<>();
        
        // 从缓存中获取用例配置
        Map<Long, TestCaseConfig> configMap = testCaseConfigCache.get(collectTaskId);
        if (configMap == null || configMap.isEmpty()) {
            log.debug("No task-level test case configs found - task ID: {}", collectTaskId);
            return result;
        }
        
        // 转换格式：从 TestCaseConfig 的 customParams 转换为 List<Map<String, String>>
        for (Long caseId : caseIds) {
            TestCaseConfig config = configMap.get(caseId);
            if (config != null && config.getCustomParams() != null && !config.getCustomParams().isEmpty()) {
                List<Map<String, String>> paramList = new ArrayList<>();
                
                for (Map<String, Object> paramObj : config.getCustomParams()) {
                    Map<String, String> param = new HashMap<>();
                    
                    // 处理key
                    Object keyObj = paramObj.get("key");
                    if (keyObj != null) {
                        param.put("key", String.valueOf(keyObj));
                    }
                    
                    // 处理value（可能是数组或字符串）
                    Object valueObj = paramObj.get("value");
                    if (valueObj != null) {
                        if (valueObj instanceof List) {
                            // 如果是数组，转换为逗号分隔的字符串
                            @SuppressWarnings("unchecked")
                            List<Object> valueList = (List<Object>) valueObj;
                            String valueStr = valueList.stream()
                                .filter(v -> v != null && !String.valueOf(v).trim().isEmpty())
                                .map(v -> String.valueOf(v).trim())
                                .collect(Collectors.joining(","));
                            param.put("value", valueStr);
                        } else {
                            // 如果是字符串，直接使用
                            param.put("value", String.valueOf(valueObj));
                        }
                    }
                    
                    if (param.containsKey("key") && param.containsKey("value")) {
                        paramList.add(param);
                    }
                }
                
                if (!paramList.isEmpty()) {
                    result.put(String.valueOf(caseId), paramList);
                    log.debug("Got task-level custom params for test case {} - param count: {}", caseId, paramList.size());
                }
            }
        }
        
        log.info("Got task-level test case custom params - task ID: {}, case count: {}", collectTaskId, result.size());
        return result;
    }
    
    // 解析形如 [{"key":"a","value":"b"}] 的JSON数组
    private List<Map<String, String>> parseKeyValueArraySafely(String jsonArray) {
        List<Map<String, String>> list = new ArrayList<>();
        if (jsonArray == null) {
            return list;
        }
        String trimmed = jsonArray.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return list;
        }
        try {
            List<Map<String, String>> parsed = objectMapper.readValue(trimmed, new TypeReference<List<Map<String, String>>>() {});
            if (parsed != null) {
                return parsed;
            }
        } catch (Exception e) {
            log.warn("Failed to parse key-value array: {}", e.getMessage());
        }
        return list;
    }

    private Map<String, String> toKeyValueMap(List<Map<String, String>> list) {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        if (list == null) {
            return map;
        }
        for (Map<String, String> item : list) {
            if (item == null) {
                continue;
            }
            String key = item.get("key");
            String value = item.get("value");
            if (key != null && !key.trim().isEmpty() && value != null) {
                map.put(key.trim(), value);
            }
        }
        return map;
    }

    private List<Map<String, String>> toKeyValueList(Map<String, String> map) {
        List<Map<String, String>> list = new ArrayList<>();
        if (map == null || map.isEmpty()) {
            return list;
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            Map<String, String> kv = new java.util.HashMap<>();
            kv.put("key", entry.getKey());
            kv.put("value", entry.getValue());
            list.add(kv);
        }
        return list;
    }

    private Map<String, List<Map<String, String>>> parseTestCaseParamsObjectSafely(String jsonObject) {
        Map<String, List<Map<String, String>>> map = new HashMap<>();
        if (jsonObject == null) {
            return map;
        }
        String trimmed = jsonObject.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return map;
        }
        try {
            Map<String, List<Map<String, String>>> parsed = objectMapper.readValue(
                trimmed,
                new TypeReference<Map<String, List<Map<String, String>>>>() {}
            );
            if (parsed != null) {
                return parsed;
            }
        } catch (Exception e) {
            log.warn("Failed to parse test case params object: {}", e.getMessage());
        }
        return map;
    }
    
    /**
     * 过滤用例自定义参数，只返回当前执行实例中用例的参数
     */
    private String filterTestCaseCustomParams(String testCaseCustomParams, List<TestCaseExecutionInstance> instances) {
        try {
            // 解析用例自定义参数JSON（先解析为Object类型，兼容value可能是数组或字符串）
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, List<Map<String, Object>>> allTestCaseParamsRaw = objectMapper.readValue(
                testCaseCustomParams, 
                new TypeReference<Map<String, List<Map<String, Object>>>>() {}
            );
            
            // 转换参数，将value统一转换为字符串格式（如果value是数组，转换为逗号分隔的字符串）
            Map<String, List<Map<String, String>>> allTestCaseParams = new HashMap<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : allTestCaseParamsRaw.entrySet()) {
                List<Map<String, String>> convertedParams = new ArrayList<>();
                for (Map<String, Object> param : entry.getValue()) {
                    Map<String, String> convertedParam = new HashMap<>();
                    String key = param.get("key") != null ? String.valueOf(param.get("key")) : "";
                    Object valueObj = param.get("value");
                    String value = "";
                    
                    if (valueObj != null) {
                        if (valueObj instanceof List) {
                            // 如果value是数组，转换为逗号分隔的字符串
                            List<?> valueList = (List<?>) valueObj;
                            value = valueList.stream()
                                .filter(v -> v != null && !String.valueOf(v).trim().isEmpty())
                                .map(v -> String.valueOf(v).trim())
                                .collect(java.util.stream.Collectors.joining(","));
                        } else {
                            // 如果value是字符串，直接使用
                            value = String.valueOf(valueObj);
                        }
                    }
                    
                    if (!key.isEmpty() && !value.isEmpty()) {
                        convertedParam.put("key", key);
                        convertedParam.put("value", value);
                        convertedParams.add(convertedParam);
                    }
                }
                if (!convertedParams.isEmpty()) {
                    allTestCaseParams.put(entry.getKey(), convertedParams);
                }
            }
            
            // 获取当前执行实例中的用例ID
            Set<Long> currentTestCaseIds = instances.stream()
                .map(TestCaseExecutionInstance::getTestCaseId)
                .collect(Collectors.toSet());
            
            // 过滤出当前用例的参数
            Map<String, List<Map<String, String>>> filteredParams = new HashMap<>();
            for (Map.Entry<String, List<Map<String, String>>> entry : allTestCaseParams.entrySet()) {
                Long testCaseId = Long.valueOf(entry.getKey());
                if (currentTestCaseIds.contains(testCaseId)) {
                    filteredParams.put(entry.getKey(), entry.getValue());
                }
            }
            
            // 将过滤后的参数转换为JSON字符串
            if (!filteredParams.isEmpty()) {
                return objectMapper.writeValueAsString(filteredParams);
            }
            
        } catch (Exception e) {
            log.error("Failed to filter test case custom parameters: {}", e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * 构建执行请求
     */
    private TestCaseExecutionRequest buildExecutionRequest(String taskId, String executorIp, TestCaseSetInfo testCaseSetInfo, 
            List<TestCaseExecutionRequest.TestCaseInfo> testCaseList, List<TestCaseExecutionRequest.UeInfo> ueList, 
            TestCaseExecutionRequest.CollectStrategyInfo collectStrategyInfo, String taskCustomParams) {
        
        TestCaseExecutionRequest request = new TestCaseExecutionRequest();
        request.setTaskId(taskId);
        request.setExecutorIp(executorIp);
        request.setTestCaseSetId(testCaseSetInfo.getTestCaseSetId());
        request.setTestCaseSetPath(testCaseSetInfo.getTestCaseSetPath());
        request.setTestCaseList(testCaseList);
        request.setUeList(ueList);
        request.setCollectStrategyInfo(collectStrategyInfo);
        request.setTaskCustomParams(taskCustomParams);
        request.setResultReportUrl(dataCollectServiceBaseUrl + "/api/test-result/report");
        
        // 使用gohttpserver地址作为日志上报URL，这样CaseExecuteService就可以上传日志文件到gohttpserver
        String goHttpServerUrl = extractGoHttpServerUrl(testCaseSetInfo.getTestCaseSetPath());
        
        if (goHttpServerUrl != null && !goHttpServerUrl.trim().isEmpty()) {
            request.setLogReportUrl(goHttpServerUrl);
            log.info("Using gohttpserver address as log report URL: {}", goHttpServerUrl);
        } else {
            request.setLogReportUrl(dataCollectServiceBaseUrl + "/api/test-result/log");
            log.info("Using default log report URL: {}", dataCollectServiceBaseUrl + "/api/test-result/log");
        }
        
        return request;
    }

    /**
     * 提取gohttpserver URL
     */
    private String extractGoHttpServerUrl(String testCaseSetPath) {
        String goHttpServerUrl = null;
        if (testCaseSetPath != null && testCaseSetPath.startsWith("http")) {
            // 从用例集路径中提取gohttpserver地址
            int uploadIndex = testCaseSetPath.indexOf("/upload/");
            if (uploadIndex > 0) {
                goHttpServerUrl = testCaseSetPath.substring(0, uploadIndex);
            }
        }
        return goHttpServerUrl;
    }

    /**
     * 发送执行请求
     */
    private boolean sendExecutionRequest(TestCaseExecutionRequest request, List<TestCaseExecutionInstance> instances, String taskId, String executorIp) {
        // 优先使用WebSocket发送任务
        if (executorWebSocketService.isExecutorOnline(executorIp)) {
            log.info("执行机在线，通过WebSocket发送任务 - 执行机IP: {}, 任务ID: {}", executorIp, taskId);
            boolean sent = executorWebSocketService.sendTaskToExecutor(executorIp, request);
            if (sent) {
                // 更新例次状态
                updateInstanceStatus(instances, taskId);
                return true;
            } else {
                log.warn("WebSocket发送任务失败，尝试使用HTTP发送 - 执行机IP: {}, 任务ID: {}", executorIp, taskId);
            }
        } else {
            log.info("执行机不在线，使用HTTP发送任务 - 执行机IP: {}, 任务ID: {}", executorIp, taskId);
        }
        
        // 如果WebSocket不可用，回退到HTTP请求
        String caseExecuteServiceUrl = "http://" + executorIp + ":8081/api/test-case-execution/receive";
        log.info("Calling CaseExecuteService via HTTP - URL: {}, task ID: {}", caseExecuteServiceUrl, taskId);
        
        try {
            org.springframework.http.ResponseEntity<Map> response = 
                httpClientUtil.post(caseExecuteServiceUrl, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                Integer code = (Integer) result.get("code");
                
                if (code != null && code == 200) {
                    log.info("CaseExecuteService call successful via HTTP - task ID: {}, executor IP: {}", taskId, executorIp);
                    
                    // 更新例次状态
                    updateInstanceStatus(instances, taskId);
                    
                    return true;
                } else {
                    String message = (String) result.get("message");
                    log.error("CaseExecuteService returned error - task ID: {}, error message: {}", taskId, message);
                    return false;
                }
            } else {
                log.error("CaseExecuteService call failed - task ID: {}, HTTP status: {}", taskId, response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            log.error("CaseExecuteService network call exception - task ID: {}, executor IP: {}, error: {}", 
                    taskId, executorIp, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 更新实例状态
     */
    private void updateInstanceStatus(List<TestCaseExecutionInstance> instances, String taskId) {
        for (TestCaseExecutionInstance instance : instances) {
            testCaseExecutionInstanceService.updateExecutionStatus(instance.getId(), "RUNNING", taskId);
        }
    }
    
    /**
     * 获取执行机关联的UE全部信息
     * 
     * @param executorIp 执行机IP
     * @return UE信息列表
     */
    private List<TestCaseExecutionRequest.UeInfo> getExecutorUeList(String executorIp) {
        List<TestCaseExecutionRequest.UeInfo> ueList = new ArrayList<>();
        
        try {
            // 1. 根据执行机IP获取执行机信息
            QueryWrapper<Executor> executorQuery = new QueryWrapper<>();
            executorQuery.eq("ip_address", executorIp);
            Executor executor = executorService.getOne(executorQuery);
            
            if (executor == null) {
                log.warn("Executor information not found - executor IP: {}", executorIp);
                return ueList;
            }
            
            // 2. 获取执行机关联的逻辑环境
            List<LogicEnvironment> logicEnvironments = getExecutorLogicEnvironments(executor);
            
            if (logicEnvironments.isEmpty()) {
                log.warn("Executor not associated with logic environment - executor IP: {}, executor ID: {}", executorIp, executor.getId());
                return ueList;
            }
            
            // 3. 获取所有逻辑环境关联的UE
            Set<Long> allUeIds = getAllUeIds(logicEnvironments);
            
            if (allUeIds.isEmpty()) {
                log.warn("Logic environment associated with executor not associated with UE - executor IP: {}", executorIp);
                return ueList;
            }
            
            // 4. 获取UE详细信息并转换为DTO格式
            ueList = convertUesToDto(allUeIds);
            
            log.info("Successfully got UE information associated with executor - executor IP: {}, UE count: {}", executorIp, ueList.size());
            
        } catch (Exception e) {
            log.error("Failed to get UE information associated with executor - executor IP: {}, error: {}", executorIp, e.getMessage(), e);
        }
        
        return ueList;
    }

    /**
     * 获取执行机关联的逻辑环境
     */
    private List<LogicEnvironment> getExecutorLogicEnvironments(Executor executor) {
        QueryWrapper<LogicEnvironment> envQuery = new QueryWrapper<>();
        envQuery.eq("executor_id", executor.getId());
        return logicEnvironmentService.list(envQuery);
    }

    /**
     * 获取所有UE ID
     */
    private Set<Long> getAllUeIds(List<LogicEnvironment> logicEnvironments) {
        Set<Long> allUeIds = new HashSet<>();
        for (LogicEnvironment logicEnvironment : logicEnvironments) {
            QueryWrapper<LogicEnvironmentUe> ueQuery = new QueryWrapper<>();
            ueQuery.eq("logic_environment_id", logicEnvironment.getId());
            List<LogicEnvironmentUe> logicEnvironmentUes = logicEnvironmentUeService.list(ueQuery);
            
            for (LogicEnvironmentUe logicEnvironmentUe : logicEnvironmentUes) {
                allUeIds.add(logicEnvironmentUe.getUeId());
            }
        }
        return allUeIds;
    }

    /**
     * 转换UE为DTO格式
     */
    private List<TestCaseExecutionRequest.UeInfo> convertUesToDto(Set<Long> allUeIds) {
        List<TestCaseExecutionRequest.UeInfo> ueList = new ArrayList<>();
        
        QueryWrapper<Ue> ueQuery = new QueryWrapper<>();
        ueQuery.in("id", allUeIds);
        List<Ue> ues = ueService.list(ueQuery);
        
        for (Ue ue : ues) {
            TestCaseExecutionRequest.UeInfo ueInfo = createUeInfo(ue);
            ueList.add(ueInfo);
        }
        
        return ueList;
    }

    private TestCaseExecutionRequest.UeInfo createUeInfo(Ue ue) {
        TestCaseExecutionRequest.UeInfo ueInfo = new TestCaseExecutionRequest.UeInfo();
        ueInfo.setId(ue.getId());
        ueInfo.setUeId(ue.getUeId());
        ueInfo.setName(ue.getName());
        ueInfo.setPurpose(ue.getPurpose());
        ueInfo.setNetworkTypeId(ue.getNetworkTypeId());
        ueInfo.setVendor(ue.getVendor());
        ueInfo.setPort(ue.getPort());
        ueInfo.setDescription(ue.getDescription());
        ueInfo.setStatus(ue.getStatus());
        
        setNetworkTypeName(ueInfo, ue.getNetworkTypeId());
        return ueInfo;
    }

    private void setNetworkTypeName(TestCaseExecutionRequest.UeInfo ueInfo, Long networkTypeId) {
        NetworkType networkType = networkTypeService.getById(networkTypeId);
        if (networkType != null) {
            ueInfo.setNetworkTypeName(networkType.getName());
        } else {
            ueInfo.setNetworkTypeName("Unknown network type");
        }
    }
    
    /**
     * 获取采集策略的所有信息
     * 
     * @param collectStrategyId 采集策略ID
     * @return 采集策略信息
     */
    private TestCaseExecutionRequest.CollectStrategyInfo getCollectStrategyInfo(Long collectStrategyId) {
        if (collectStrategyId == null) {
                            log.warn("Collect strategy ID is empty");
            return null;
        }
        
        try {
            CollectStrategy collectStrategy = collectStrategyService.getById(collectStrategyId);
            if (collectStrategy == null) {
                log.warn("Collect strategy not found - strategy ID: {}", collectStrategyId);
                return null;
            }
            
            TestCaseExecutionRequest.CollectStrategyInfo strategyInfo = createCollectStrategyInfo(collectStrategy);
            log.info("Successfully got collect strategy information - strategy ID: {}, strategy name: {}", collectStrategyId, collectStrategy.getName());
            return strategyInfo;
            
        } catch (Exception e) {
            log.error("Failed to get collect strategy information - strategy ID: {}, error: {}", collectStrategyId, e.getMessage(), e);
            return null;
        }
    }

    private TestCaseExecutionRequest.CollectStrategyInfo createCollectStrategyInfo(CollectStrategy collectStrategy) {
        TestCaseExecutionRequest.CollectStrategyInfo strategyInfo = new TestCaseExecutionRequest.CollectStrategyInfo();
        strategyInfo.setId(collectStrategy.getId());
        strategyInfo.setName(collectStrategy.getName());
        strategyInfo.setCollectCount(collectStrategy.getCollectCount());
        strategyInfo.setTestCaseSetId(collectStrategy.getTestCaseSetId());
        strategyInfo.setBusinessCategory(collectStrategy.getBusinessCategory());
        strategyInfo.setApp(collectStrategy.getApp());
        
        // 设置appen字段：从用例集中查找对应的appen值
        String appEn = getAppEnFromTestCaseSet(collectStrategy.getTestCaseSetId(), collectStrategy.getApp());
        strategyInfo.setAppEn(appEn);
        
        strategyInfo.setIntent(collectStrategy.getIntent());
        strategyInfo.setCustomParams(collectStrategy.getCustomParams());
        strategyInfo.setDescription(collectStrategy.getDescription());
        strategyInfo.setStatus(collectStrategy.getStatus());
        return strategyInfo;
    }
    
    /**
     * 从用例集中查找对应的appen值
     */
    private String getAppEnFromTestCaseSet(Long testCaseSetId, String app) {
        if (testCaseSetId == null || app == null || app.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 获取用例集中的用例列表
            List<TestCase> testCases = testCaseService.getByTestCaseSetId(testCaseSetId);
            
            // 查找匹配的用例
            for (TestCase testCase : testCases) {
                if (app.equals(testCase.getApp())) {
                    return testCase.getAppEn();
                }
            }
            
            log.warn("No matching test case found for app: {} in test case set: {}", app, testCaseSetId);
            return null;
            
        } catch (Exception e) {
            log.error("Failed to get appEn from test case set - test case set ID: {}, app: {}, error: {}", 
                     testCaseSetId, app, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 获取用例的is_new状态映射
     * 
     * @param instances 用例执行实例列表
     * @return 用例ID到is_new状态的映射
     */
    private Map<Long, Boolean> getAppIsNewMap(List<TestCaseExecutionInstance> instances) {
        Map<Long, Boolean> appIsNewMap = new HashMap<>();
        
        if (instances == null || instances.isEmpty()) {
            return appIsNewMap;
        }
        
        try {
            // 收集所有唯一的APP名称
            Set<String> uniqueApps = new HashSet<>();
            for (TestCaseExecutionInstance instance : instances) {
                TestCase testCase = testCaseService.getById(instance.getTestCaseId());
                if (testCase != null && testCase.getApp() != null && !testCase.getApp().trim().isEmpty()) {
                    uniqueApps.add(testCase.getApp());
                }
            }
            
            if (!uniqueApps.isEmpty()) {
                // 为每个APP查询iOS和非iOS两种版本
                List<AppCheckRequest> appCheckRequests = new ArrayList<>();
                for (String appName : uniqueApps) {
                    // 查询iOS版本
                    appCheckRequests.add(new AppCheckRequest(appName, true));
                    // 查询非iOS版本
                    appCheckRequests.add(new AppCheckRequest(appName, false));
                }
                
                log.info("调用checkAppIsNew方法 - 请求参数: {}", appCheckRequests);
                
                AppCheckResponse response = externalApiService.checkAppIsNew(appCheckRequests);
                
                if (response != null && response.getData() != null) {
                    // 创建APP+is_ios到is_new的映射
                    Map<String, Boolean> appIosToIsNewMap = new HashMap<>();
                    for (AppCheckResponse.AppCheckData data : response.getData()) {
                        if (data.getAppName() != null && data.getIsIos() != null && data.getIsNew() != null) {
                            String key = data.getAppName() + "_" + data.getIsIos();
                            appIosToIsNewMap.put(key, data.getIsNew());
                        }
                    }
                    
                    // 为每个用例匹配对应的is_new状态
                    for (TestCaseExecutionInstance instance : instances) {
                        TestCase testCase = testCaseService.getById(instance.getTestCaseId());
                        if (testCase != null && testCase.getApp() != null && !testCase.getApp().trim().isEmpty()) {
                            String appName = testCase.getApp();
                            String testCaseNumber = testCase.getNumber();
                            
                            // 判断是否为iOS应用：用例编号包含_ios_
                            boolean isIos = testCaseNumber != null && testCaseNumber.contains("_ios_");
                            
                            // 构建查询键
                            String queryKey = appName + "_" + isIos;
                            Boolean isNew = appIosToIsNewMap.get(queryKey);
                            
                            if (isNew != null) {
                                appIsNewMap.put(instance.getTestCaseId(), isNew);
                                log.info("用例 {} (编号: {}) 的APP {} (is_ios: {}) is_new状态: {}", 
                                    testCase.getName(), testCase.getNumber(), testCase.getApp(), isIos, isNew);
                            } else {
                                // 没有查询到结果，设置默认值
                                appIsNewMap.put(instance.getTestCaseId(), false);
                                log.warn("用例 {} (编号: {}) 的APP {} (is_ios: {}) 未查询到结果，设置默认值is_new: false", 
                                    testCase.getName(), testCase.getNumber(), testCase.getApp(), isIos);
                            }
                        }
                    }
                } else {
                    log.warn("外部接口响应为空，所有用例将使用默认值is_new: false");
                    // 设置所有用例的默认值
                    for (TestCaseExecutionInstance instance : instances) {
                        appIsNewMap.put(instance.getTestCaseId(), false);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("获取APP is_new状态失败 - 错误信息: {}", e.getMessage(), e);
        }
        
        return appIsNewMap;
    }
}
