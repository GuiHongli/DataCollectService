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
import com.datacollect.service.NetworkElementService;
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
import com.alibaba.fastjson.JSON;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

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
    
    @Lazy
    @Autowired
    private UeService ueService;
    
    @Autowired
    private NetworkTypeService networkTypeService;
    
    @Autowired
    private NetworkElementService networkElementService;
    
    @Autowired
    private com.datacollect.service.RegionService regionService;
    
    @Autowired
    private ExternalApiService externalApiService;
    
    @Autowired
    private ExecutorWebSocketService executorWebSocketService;
    
    @Autowired(required = false)
    private com.datacollect.service.ConfigService configService;
    
    @Value("${datacollect.service.base-url:http://localhost:8080}")
    private String dataCollectServiceBaseUrl;
    
    /**
     * 用例配置缓存（key: collectTaskId, value: Map<testCaseId, TestCaseConfig>）
     * 用于存储任务级别的用例配置（执行次数和自定义参数）
     */
    private final java.util.concurrent.ConcurrentHashMap<Long, Map<Long, TestCaseConfig>> testCaseConfigCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * 任务队列：按逻辑环境ID分组，存储等待执行的任务
     */
    private final Map<Long, BlockingQueue<QueuedTask>> taskQueues = new ConcurrentHashMap<>();
    
    /**
     * UE级别的锁：按UE ID分组，确保使用相同UE的任务不会同时执行
     */
    private final Map<Integer, ReentrantLock> ueLocks = new ConcurrentHashMap<>();
    
    /**
     * UE级别的任务队列：按UE ID分组，存储等待该UE的任务
     */
    private final Map<Integer, BlockingQueue<QueuedTask>> ueTaskQueues = new ConcurrentHashMap<>();
    
    /**
     * 任务ID到UE ID列表的映射：用于在任务完成时释放UE锁
     * key: taskId (String), value: UE ID列表 (List<Integer>)
     */
    private final Map<String, List<Integer>> taskIdToUeIdsMap = new ConcurrentHashMap<>();
    
    /**
     * UE占用状态：记录哪个任务ID占用了UE
     * key: UE ID (Integer), value: 占用该UE的任务ID (String)
     */
    private final Map<Integer, String> ueOccupiedByTask = new ConcurrentHashMap<>();
    
    /**
     * UE队列处理器运行状态：记录哪些UE的队列处理器正在运行
     * key: UE ID (Integer), value: 是否正在处理 (Boolean)
     */
    private final Map<Integer, Boolean> ueQueueProcessorRunning = new ConcurrentHashMap<>();
    
    /**
     * 定时任务执行器：用于每2分钟查询一次等待中的任务
     */
    private ScheduledExecutorService scheduledTaskExecutor;
    
    /**
     * 定时任务间隔（分钟）
     */
    private static final int SCHEDULED_TASK_INTERVAL_MINUTES = 2;
    
    /**
     * 排队任务信息
     */
    private static class QueuedTask {
        private final String taskId;
        private final List<TestCaseExecutionInstance> instances;
        private final Long logicEnvironmentId;
        private final long queueTime;
        
        public QueuedTask(String taskId, List<TestCaseExecutionInstance> instances, Long logicEnvironmentId) {
            this.taskId = taskId;
            this.instances = instances;
            this.logicEnvironmentId = logicEnvironmentId;
            this.queueTime = System.currentTimeMillis();
        }
        
        public String getTaskId() {
            return taskId;
        }
        
        public List<TestCaseExecutionInstance> getInstances() {
            return instances;
        }
        
        public Long getLogicEnvironmentId() {
            return logicEnvironmentId;
        }
        
        public long getQueueTime() {
            return queueTime;
        }
    }
    
    /**
     * 用例配置内部类
     */
    @lombok.Data
    private static class TestCaseConfig {
        private Long testCaseId;
        private Integer executionCount;
        private List<Map<String, Object>> customParams; // [{"key": "k", "value": ["v1", "v2"]}]
    }
    
    /**
     * 用例信息内部类（用于物理用例分组）
     */
    @lombok.Data
    private static class TestCaseInfo {
        private Long testCaseId;
        private TestCase testCase; // 用例实体
        private Integer executionCount; // 执行次数
        private TestCaseConfig config; // 用例配置
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
            
            // 4. 按物理用例分组用例（逻辑组网_网络_厂商）
            Map<String, List<TestCaseInfo>> testCasesByPhysicalNetwork = groupTestCasesByPhysicalNetwork(
                    testCaseIds, request.getNetwork(), request.getManufacturer(), testCaseConfigMap, request.getCollectCount());
            log.info("Test cases grouped by physical network - physical network count: {} - task ID: {}", 
                    testCasesByPhysicalNetwork.size(), collectTaskId);
            
            // 5. 匹配逻辑环境，按逻辑环境分组用例
            Map<Long, List<TestCaseInfo>> testCasesByLogicEnvironment = matchTestCasesToLogicEnvironments(
                    testCasesByPhysicalNetwork, request.getLogicEnvironmentIds());
            log.info("Test cases matched to logic environments - logic environment count: {} - task ID: {}", 
                    testCasesByLogicEnvironment.size(), collectTaskId);
            
            // 6. 为每个逻辑环境创建用例执行实例并保存
            List<TestCaseExecutionInstance> allInstances = createAndSaveInstancesForLogicEnvironments(
                    collectTaskId, testCasesByLogicEnvironment);
            log.info("Test case execution instances created and saved - instance count: {} - task ID: {}", 
                    allInstances.size(), collectTaskId);
            
            // 7. 更新任务总用例数
            collectTaskService.updateTaskProgress(collectTaskId, allInstances.size(), 0, 0);
            
            // 8. 保存用例配置到任务中（用于后续获取用例自定义参数）
            storeTestCaseConfigs(collectTaskId, testCaseConfigMap);
            
            // 9. 异步调用执行机服务
            callExecutorServicesAsync(allInstances, collectTaskId);
            
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
     * 获取测试用例（不再根据业务大类和app筛选）
     */
    private List<TestCase> filterTestCasesByStrategy(List<TestCase> allTestCases, CollectStrategy strategy) {
        // 直接返回所有用例，不再根据业务大类和app筛选
        return allTestCases;
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
                
                // 检查任务当前状态
                CollectTask collectTask = collectTaskService.getCollectTaskById(collectTaskId);
                if (collectTask == null) {
                    log.warn("任务不存在 - 任务ID: {}", collectTaskId);
                    return;
                }
                
                String currentStatus = collectTask.getStatus();
                
                if (!callSuccess) {
                    // 如果任务状态是 WAITING，说明任务已进入队列等待，不需要更新为 STOPPED
                    if ("WAITING".equals(currentStatus)) {
                        log.info("任务已进入等待队列，保持等待状态 - 任务ID: {}", collectTaskId);
                    } else {
                        // 如果任务状态不是 WAITING，说明是真正的失败，更新为 STOPPED
                        collectTaskService.updateTaskStatus(collectTaskId, "STOPPED");
                        collectTaskService.updateTaskFailureReason(collectTaskId, "执行机服务调用失败");
                        log.error("Executor service call failed - task ID: {}", collectTaskId);
                    }
                } else {
                    // 调用成功，如果任务状态是 WAITING，说明部分任务在等待，部分任务已执行
                    // 如果任务状态不是 WAITING，说明所有任务都已执行，更新为 RUNNING
                    if (!"WAITING".equals(currentStatus)) {
                        collectTaskService.updateTaskStatus(collectTaskId, "RUNNING");
                        log.info("Executor service call successful, task status updated to RUNNING - task ID: {}", collectTaskId);
                    } else {
                        log.info("Executor service call successful, but some tasks are waiting - task ID: {}", collectTaskId);
                    }
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
            
            // 记录用例的自定义参数信息（用于后续验证）
            boolean hasCustomParams = config != null && config.getCustomParams() != null && !config.getCustomParams().isEmpty();
            
            for (int round = 1; round <= executionCount; round++) {
                TestCaseExecutionInstance instance = new TestCaseExecutionInstance();
                instance.setCollectTaskId(collectTaskId);
                instance.setTestCaseId(testCaseId);
                instance.setRound(round);
                instance.setStatus("RUNNING");
                instances.add(instance);
            }
            
            log.info("Assembled instances for test case {} - execution count: {}, has custom params: {}", 
                    testCaseId, executionCount, hasCustomParams);
        }
        
        log.info("Test case execution instances assembly completed - instance count: {} - task ID: {}", instances.size(), collectTaskId);
        return instances;
    }

    @Override
    public List<TestCaseExecutionInstance> distributeInstancesToEnvironments(
            List<TestCaseExecutionInstance> instances, 
            List<Long> logicEnvironmentIds, 
            String network, 
            List<String> manufacturer) {
        log.info("Start distributing test case execution instances to logic environments - instance count: {}, logic environment count: {}, network: {}, manufacturer: {}", 
                instances.size(), logicEnvironmentIds.size(), network, manufacturer);
        
        validateLogicEnvironments(logicEnvironmentIds);
        
        // 如果没有网络或厂商，使用原来的均分逻辑
        if (network == null || network.trim().isEmpty() || manufacturer == null || manufacturer.isEmpty()) {
            log.info("Network or manufacturer not provided, using even distribution");
            Map<Long, String> environmentToExecutorMap = getEnvironmentToExecutorMap(logicEnvironmentIds);
            distributeInstancesEvenly(instances, logicEnvironmentIds, environmentToExecutorMap);
            return instances;
        }
        
        // 过滤掉空的厂商
        List<String> manufacturers = manufacturer.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        
        if (manufacturers.isEmpty()) {
            log.warn("No valid manufacturers found, using even distribution");
            Map<Long, String> environmentToExecutorMap = getEnvironmentToExecutorMap(logicEnvironmentIds);
            distributeInstancesEvenly(instances, logicEnvironmentIds, environmentToExecutorMap);
            return instances;
        }
        
        // 获取逻辑环境信息（包括物理组网）
        Map<Long, LogicEnvironmentInfo> environmentInfoMap = getLogicEnvironmentInfoMap(logicEnvironmentIds);
        
        // 获取用例信息缓存
        Map<Long, TestCase> testCaseCache = new HashMap<>();
        
        // 按物理组网分组用例执行实例（key: 物理组网, value: 实例列表）
        Map<String, List<TestCaseExecutionInstance>> instancesByPhysicalNetwork = new HashMap<>();
        
        for (TestCaseExecutionInstance instance : instances) {
            // 获取用例信息
            TestCase testCase = testCaseCache.get(instance.getTestCaseId());
            if (testCase == null) {
                testCase = testCaseService.getById(instance.getTestCaseId());
                if (testCase != null) {
                    testCaseCache.put(instance.getTestCaseId(), testCase);
                }
            }
            
            if (testCase == null || testCase.getLogicNetwork() == null || testCase.getLogicNetwork().trim().isEmpty()) {
                log.warn("TestCase {} not found or has no logic network, will use even distribution - instance round: {}", 
                        instance.getTestCaseId(), instance.getRound());
                // 如果没有逻辑组网，使用默认分组
                instancesByPhysicalNetwork.computeIfAbsent("default", k -> new ArrayList<>()).add(instance);
                continue;
            }
            
            // 解析用例的逻辑组网（可能有多个，用分号分隔）
            String[] logicNetworks = testCase.getLogicNetwork().split(";");
            boolean matched = false;
            String matchedPhysicalNetwork = null;
            
            // 为每个逻辑组网和厂商生成物理组网，找到第一个匹配的逻辑环境
            for (String logicNetwork : logicNetworks) {
                String trimmedLogicNetwork = logicNetwork.trim();
                if (trimmedLogicNetwork.isEmpty()) {
                    continue;
                }
                
                // 为每个厂商生成物理组网
                for (String manu : manufacturers) {
                    String physicalNetwork = trimmedLogicNetwork + "_" + network + "_" + manu;
                    
                    // 找到匹配该物理组网的逻辑环境
                    List<Long> matchingEnvironments = findMatchingEnvironments(physicalNetwork, environmentInfoMap);
                    
                    if (!matchingEnvironments.isEmpty()) {
                        // 找到匹配的物理组网，将实例添加到对应的分组中
                        instancesByPhysicalNetwork.computeIfAbsent(physicalNetwork, k -> new ArrayList<>()).add(instance);
                        matched = true;
                        matchedPhysicalNetwork = physicalNetwork;
                        log.debug("Matched physical network {} for test case {} (round: {}) - matching environments: {}", 
                                physicalNetwork, instance.getTestCaseId(), instance.getRound(), matchingEnvironments.size());
                        break; // 只使用第一个匹配的厂商
                    }
                }
                
                if (matched) {
                    break; // 已经找到匹配的物理组网，跳出循环
                }
            }
            
            // 如果没有找到匹配的物理组网，使用默认分组
            if (!matched) {
                log.warn("No matching physical network found for test case {} (round: {}), will use even distribution", 
                        instance.getTestCaseId(), instance.getRound());
                instancesByPhysicalNetwork.computeIfAbsent("default", k -> new ArrayList<>()).add(instance);
            } else {
                log.debug("Instance assigned to physical network {} - test case: {}, round: {}", 
                        matchedPhysicalNetwork, instance.getTestCaseId(), instance.getRound());
            }
        }
        
        // 为每个物理组网分组分配用例到匹配的逻辑环境
        distributeInstancesByPhysicalNetwork(instancesByPhysicalNetwork, environmentInfoMap);
        
        log.info("Test case execution instances distribution completed - task ID: {}", instances.get(0).getCollectTaskId());
        return instances;
    }
    
    /**
     * 逻辑环境信息内部类
     */
    @lombok.Data
    private static class LogicEnvironmentInfo {
        private Long id;
        private String executorIp;
        private List<String> physicalNetworks; // 物理组网列表
    }
    
    /**
     * 按物理用例分组用例（物理用例 = 逻辑组网_网络_厂商）
     * 
     * @param testCaseIds 用例ID列表
     * @param network 网络类型
     * @param manufacturer 厂商列表
     * @param testCaseConfigMap 用例配置Map
     * @param defaultCollectCount 默认采集次数
     * @return 按物理用例分组的用例信息Map（key: 物理用例, value: 用例信息列表）
     */
    private Map<String, List<TestCaseInfo>> groupTestCasesByPhysicalNetwork(
            List<Long> testCaseIds, String network, List<String> manufacturer, 
            Map<Long, TestCaseConfig> testCaseConfigMap, Integer defaultCollectCount) {
        log.info("Start grouping test cases by physical network - test case count: {}, network: {}, manufacturer: {}", 
                testCaseIds.size(), network, manufacturer);
        
        Map<String, List<TestCaseInfo>> testCasesByPhysicalNetwork = new HashMap<>();
        
        // 如果没有网络或厂商，使用默认分组
        if (network == null || network.trim().isEmpty() || manufacturer == null || manufacturer.isEmpty()) {
            log.info("Network or manufacturer not provided, using default grouping");
            String defaultPhysicalNetwork = "default";
            for (Long testCaseId : testCaseIds) {
                TestCase testCase = testCaseService.getById(testCaseId);
                if (testCase != null) {
                    TestCaseInfo testCaseInfo = createTestCaseInfo(testCase, testCaseConfigMap, defaultCollectCount);
                    testCasesByPhysicalNetwork.computeIfAbsent(defaultPhysicalNetwork, k -> new ArrayList<>()).add(testCaseInfo);
                }
            }
            return testCasesByPhysicalNetwork;
        }
        
        // 过滤掉空的厂商
        List<String> manufacturers = manufacturer.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        
        if (manufacturers.isEmpty()) {
            log.warn("No valid manufacturers found, using default grouping");
            String defaultPhysicalNetwork = "default";
            for (Long testCaseId : testCaseIds) {
                TestCase testCase = testCaseService.getById(testCaseId);
                if (testCase != null) {
                    TestCaseInfo testCaseInfo = createTestCaseInfo(testCase, testCaseConfigMap, defaultCollectCount);
                    testCasesByPhysicalNetwork.computeIfAbsent(defaultPhysicalNetwork, k -> new ArrayList<>()).add(testCaseInfo);
                }
            }
            return testCasesByPhysicalNetwork;
        }
        
        // 遍历用例，按物理用例分组
        for (Long testCaseId : testCaseIds) {
            TestCase testCase = testCaseService.getById(testCaseId);
            if (testCase == null) {
                log.warn("Test case not found - test case ID: {}", testCaseId);
                continue;
            }
            
            if (testCase.getLogicNetwork() == null || testCase.getLogicNetwork().trim().isEmpty()) {
                log.warn("Test case has no logic network - test case ID: {}, using default grouping", testCaseId);
                String defaultPhysicalNetwork = "default";
                TestCaseInfo testCaseInfo = createTestCaseInfo(testCase, testCaseConfigMap, defaultCollectCount);
                testCasesByPhysicalNetwork.computeIfAbsent(defaultPhysicalNetwork, k -> new ArrayList<>()).add(testCaseInfo);
                continue;
            }
            
            // 解析用例的逻辑组网（可能有多个，用分号分隔）
            String[] logicNetworks = testCase.getLogicNetwork().split(";");
            TestCaseInfo testCaseInfo = createTestCaseInfo(testCase, testCaseConfigMap, defaultCollectCount);
            
            // 为每个逻辑组网和厂商生成物理用例
            for (String logicNetwork : logicNetworks) {
                String trimmedLogicNetwork = logicNetwork.trim();
                if (trimmedLogicNetwork.isEmpty()) {
                    continue;
                }
                
                for (String manu : manufacturers) {
                    String physicalNetwork = trimmedLogicNetwork + "_" + network + "_" + manu;
                    testCasesByPhysicalNetwork.computeIfAbsent(physicalNetwork, k -> new ArrayList<>()).add(testCaseInfo);
                    log.debug("Grouped test case {} to physical network {} - logic network: {}, network: {}, manufacturer: {}", 
                            testCaseId, physicalNetwork, trimmedLogicNetwork, network, manu);
                }
            }
        }
        
        log.info("Test cases grouped by physical network completed - physical network count: {}", testCasesByPhysicalNetwork.size());
        return testCasesByPhysicalNetwork;
    }
    
    /**
     * 创建用例信息对象
     */
    private TestCaseInfo createTestCaseInfo(TestCase testCase, Map<Long, TestCaseConfig> testCaseConfigMap, Integer defaultCollectCount) {
        TestCaseInfo testCaseInfo = new TestCaseInfo();
        testCaseInfo.setTestCaseId(testCase.getId());
        testCaseInfo.setTestCase(testCase);
        TestCaseConfig config = testCaseConfigMap.get(testCase.getId());
        testCaseInfo.setConfig(config);
        int executionCount = (config != null && config.getExecutionCount() != null) ? config.getExecutionCount() : defaultCollectCount;
        testCaseInfo.setExecutionCount(executionCount);
        return testCaseInfo;
    }
    
    /**
     * 匹配逻辑环境，按逻辑环境分组用例
     * 
     * @param testCasesByPhysicalNetwork 按物理用例分组的用例信息Map
     * @param logicEnvironmentIds 逻辑环境ID列表
     * @return 按逻辑环境分组的用例信息Map（key: 逻辑环境ID, value: 用例信息列表）
     */
    private Map<Long, List<TestCaseInfo>> matchTestCasesToLogicEnvironments(
            Map<String, List<TestCaseInfo>> testCasesByPhysicalNetwork, List<Long> logicEnvironmentIds) {
        log.info("Start matching test cases to logic environments - physical network count: {}, logic environment count: {}", 
                testCasesByPhysicalNetwork.size(), logicEnvironmentIds.size());
        
        validateLogicEnvironments(logicEnvironmentIds);
        
        // 获取逻辑环境信息（包括物理组网）
        Map<Long, LogicEnvironmentInfo> environmentInfoMap = getLogicEnvironmentInfoMap(logicEnvironmentIds);
        
        // 按逻辑环境分组用例
        Map<Long, List<TestCaseInfo>> testCasesByLogicEnvironment = new HashMap<>();
        
        for (Map.Entry<String, List<TestCaseInfo>> entry : testCasesByPhysicalNetwork.entrySet()) {
            String physicalNetwork = entry.getKey();
            List<TestCaseInfo> testCaseInfos = entry.getValue();
            
            // 如果是默认分组，均分到所有逻辑环境
            if ("default".equals(physicalNetwork)) {
                log.info("Using even distribution for {} test cases without matching physical network", testCaseInfos.size());
                distributeTestCasesEvenly(testCaseInfos, logicEnvironmentIds, testCasesByLogicEnvironment);
                continue;
            }
            
            // 找到匹配该物理组网的逻辑环境
            List<Long> matchingEnvironments = findMatchingEnvironments(physicalNetwork, environmentInfoMap);
            
            if (matchingEnvironments.isEmpty()) {
                log.warn("No matching logic environment found for physical network: {}, using even distribution for {} test cases", 
                        physicalNetwork, testCaseInfos.size());
                distributeTestCasesEvenly(testCaseInfos, logicEnvironmentIds, testCasesByLogicEnvironment);
                continue;
            }
            
            // 均分用例到匹配的逻辑环境
            int totalTestCases = testCaseInfos.size();
            int environmentCount = matchingEnvironments.size();
            int baseCount = totalTestCases / environmentCount;
            int remainder = totalTestCases % environmentCount;
            
            log.info("Distributing {} test cases for physical network {} to {} environments - base: {}, remainder: {}", 
                    totalTestCases, physicalNetwork, environmentCount, baseCount, remainder);
            
            int testCaseIndex = 0;
            for (int i = 0; i < matchingEnvironments.size(); i++) {
                Long logicEnvironmentId = matchingEnvironments.get(i);
                int currentEnvironmentCount = baseCount + (i < remainder ? 1 : 0);
                
                for (int j = 0; j < currentEnvironmentCount; j++) {
                    if (testCaseIndex < testCaseInfos.size()) {
                        TestCaseInfo testCaseInfo = testCaseInfos.get(testCaseIndex);
                        testCasesByLogicEnvironment.computeIfAbsent(logicEnvironmentId, k -> new ArrayList<>()).add(testCaseInfo);
                        testCaseIndex++;
                    }
                }
                
                log.debug("Assigned {} test cases to logic environment {} for physical network {}", 
                        currentEnvironmentCount, logicEnvironmentId, physicalNetwork);
            }
        }
        
        log.info("Test cases matched to logic environments completed - logic environment count: {}", testCasesByLogicEnvironment.size());
        return testCasesByLogicEnvironment;
    }
    
    /**
     * 均分用例到所有逻辑环境
     */
    private void distributeTestCasesEvenly(List<TestCaseInfo> testCaseInfos, List<Long> logicEnvironmentIds, 
            Map<Long, List<TestCaseInfo>> testCasesByLogicEnvironment) {
        int totalTestCases = testCaseInfos.size();
        int environmentCount = logicEnvironmentIds.size();
        int baseCount = totalTestCases / environmentCount;
        int remainder = totalTestCases % environmentCount;
        
        int testCaseIndex = 0;
        for (int i = 0; i < logicEnvironmentIds.size(); i++) {
            Long logicEnvironmentId = logicEnvironmentIds.get(i);
            int currentEnvironmentCount = baseCount + (i < remainder ? 1 : 0);
            
            for (int j = 0; j < currentEnvironmentCount; j++) {
                if (testCaseIndex < testCaseInfos.size()) {
                    TestCaseInfo testCaseInfo = testCaseInfos.get(testCaseIndex);
                    testCasesByLogicEnvironment.computeIfAbsent(logicEnvironmentId, k -> new ArrayList<>()).add(testCaseInfo);
                    testCaseIndex++;
                }
            }
        }
    }
    
    /**
     * 为每个逻辑环境创建用例执行实例并保存
     * 
     * @param collectTaskId 采集任务ID
     * @param testCasesByLogicEnvironment 按逻辑环境分组的用例信息Map
     * @return 所有创建的用例执行实例列表
     */
    private List<TestCaseExecutionInstance> createAndSaveInstancesForLogicEnvironments(
            Long collectTaskId, Map<Long, List<TestCaseInfo>> testCasesByLogicEnvironment) {
        log.info("Start creating test case execution instances for logic environments - task ID: {}, logic environment count: {}", 
                collectTaskId, testCasesByLogicEnvironment.size());
        
        List<TestCaseExecutionInstance> allInstances = new ArrayList<>();
        Map<Long, LogicEnvironmentInfo> environmentInfoMap = new HashMap<>();
        
        // 获取所有逻辑环境信息
        for (Long logicEnvironmentId : testCasesByLogicEnvironment.keySet()) {
            LogicEnvironment logicEnvironment = logicEnvironmentService.getById(logicEnvironmentId);
            if (logicEnvironment != null && logicEnvironment.getExecutorId() != null) {
                Executor executor = executorService.getById(logicEnvironment.getExecutorId());
                if (executor != null && executor.getIpAddress() != null) {
                    LogicEnvironmentInfo info = new LogicEnvironmentInfo();
                    info.setId(logicEnvironmentId);
                    info.setExecutorIp(executor.getIpAddress());
                    // 解析物理组网
                    if (logicEnvironment.getPhysicalNetwork() != null && !logicEnvironment.getPhysicalNetwork().trim().isEmpty()) {
                        try {
                            List<String> physicalNetworks = JSON.parseArray(logicEnvironment.getPhysicalNetwork(), String.class);
                            info.setPhysicalNetworks(physicalNetworks);
                        } catch (Exception e) {
                            log.warn("Failed to parse physical network for logic environment {}: {}", logicEnvironmentId, e.getMessage());
                            info.setPhysicalNetworks(new ArrayList<>());
                        }
                    } else {
                        info.setPhysicalNetworks(new ArrayList<>());
                    }
                    environmentInfoMap.put(logicEnvironmentId, info);
                }
            }
        }
        
        // 为每个逻辑环境创建用例执行实例
        for (Map.Entry<Long, List<TestCaseInfo>> entry : testCasesByLogicEnvironment.entrySet()) {
            Long logicEnvironmentId = entry.getKey();
            List<TestCaseInfo> testCaseInfos = entry.getValue();
            LogicEnvironmentInfo info = environmentInfoMap.get(logicEnvironmentId);
            
            if (info == null || info.getExecutorIp() == null) {
                log.warn("Logic environment {} has no executor IP, skipping", logicEnvironmentId);
                continue;
            }
            
            // 为每个用例创建执行实例
            for (TestCaseInfo testCaseInfo : testCaseInfos) {
                int executionCount = testCaseInfo.getExecutionCount();
                for (int round = 1; round <= executionCount; round++) {
                    TestCaseExecutionInstance instance = new TestCaseExecutionInstance();
                    instance.setCollectTaskId(collectTaskId);
                    instance.setTestCaseId(testCaseInfo.getTestCaseId());
                    instance.setRound(round);
                    instance.setLogicEnvironmentId(logicEnvironmentId);
                    instance.setExecutorIp(info.getExecutorIp());
                    instance.setStatus("RUNNING");
                    allInstances.add(instance);
                }
            }
            
            log.info("Created {} instances for logic environment {} (executor IP: {})", 
                    testCaseInfos.stream().mapToInt(TestCaseInfo::getExecutionCount).sum(), logicEnvironmentId, info.getExecutorIp());
        }
        
        // 保存所有实例
        saveTestCaseInstances(allInstances, collectTaskId);
        
        log.info("Test case execution instances created and saved - total instance count: {} - task ID: {}", 
                allInstances.size(), collectTaskId);
        return allInstances;
    }
    
    /**
     * 获取逻辑环境信息映射（包括物理组网）
     */
    private Map<Long, LogicEnvironmentInfo> getLogicEnvironmentInfoMap(List<Long> logicEnvironmentIds) {
        Map<Long, LogicEnvironmentInfo> infoMap = new HashMap<>();
        
        for (Long logicEnvironmentId : logicEnvironmentIds) {
            LogicEnvironmentDTO dto = logicEnvironmentService.getLogicEnvironmentDTO(logicEnvironmentId);
            if (dto == null) {
                continue;
            }
            
            LogicEnvironmentInfo info = new LogicEnvironmentInfo();
            info.setId(logicEnvironmentId);
            
            // 获取执行机IP
            if (dto.getExecutorId() != null) {
                String executorIp = getExecutorIpByMacAddress(dto.getExecutorId());
                if (executorIp == null && dto.getExecutorIpAddress() != null) {
                    executorIp = dto.getExecutorIpAddress();
                }
                info.setExecutorIp(executorIp);
            }
            
            // 获取逻辑环境的物理组网列表
            LogicEnvironment logicEnvironment = logicEnvironmentService.getById(logicEnvironmentId);
            if (logicEnvironment != null && logicEnvironment.getPhysicalNetwork() != null 
                    && !logicEnvironment.getPhysicalNetwork().trim().isEmpty()) {
                try {
                    List<String> physicalNetworks = JSON.parseArray(logicEnvironment.getPhysicalNetwork(), String.class);
                    info.setPhysicalNetworks(physicalNetworks != null ? physicalNetworks : new ArrayList<>());
                } catch (Exception e) {
                    log.error("Failed to parse physical network JSON for logic environment {}: {}", 
                            logicEnvironmentId, e.getMessage());
                    info.setPhysicalNetworks(new ArrayList<>());
                }
            } else {
                info.setPhysicalNetworks(new ArrayList<>());
            }
            
            infoMap.put(logicEnvironmentId, info);
        }
        
        return infoMap;
    }
    
    /**
     * 找到匹配指定物理组网的逻辑环境列表
     */
    private List<Long> findMatchingEnvironments(String physicalNetwork, Map<Long, LogicEnvironmentInfo> environmentInfoMap) {
        List<Long> matchingEnvironments = new ArrayList<>();
        
        for (Map.Entry<Long, LogicEnvironmentInfo> entry : environmentInfoMap.entrySet()) {
            LogicEnvironmentInfo info = entry.getValue();
            if (info.getPhysicalNetworks() != null && info.getPhysicalNetworks().contains(physicalNetwork)) {
                matchingEnvironments.add(entry.getKey());
            }
        }
        
        return matchingEnvironments;
    }
    
    /**
     * 根据物理组网分配用例执行实例到逻辑环境
     */
    private void distributeInstancesByPhysicalNetwork(
            Map<String, List<TestCaseExecutionInstance>> instancesByPhysicalNetwork,
            Map<Long, LogicEnvironmentInfo> environmentInfoMap) {
        
        for (Map.Entry<String, List<TestCaseExecutionInstance>> entry : instancesByPhysicalNetwork.entrySet()) {
            String physicalNetwork = entry.getKey();
            List<TestCaseExecutionInstance> instances = entry.getValue();
            
            // 如果是默认分组，使用均分逻辑
            if ("default".equals(physicalNetwork)) {
                log.info("Using even distribution for {} instances without matching physical network", instances.size());
                List<Long> allEnvironmentIds = new ArrayList<>(environmentInfoMap.keySet());
                Map<Long, String> environmentToExecutorMap = new HashMap<>();
                for (Long envId : allEnvironmentIds) {
                    LogicEnvironmentInfo info = environmentInfoMap.get(envId);
                    if (info != null && info.getExecutorIp() != null) {
                        environmentToExecutorMap.put(envId, info.getExecutorIp());
                    }
                }
                distributeInstancesEvenly(instances, allEnvironmentIds, environmentToExecutorMap);
                continue;
            }
            
            // 找到匹配该物理组网的逻辑环境
            List<Long> matchingEnvironments = findMatchingEnvironments(physicalNetwork, environmentInfoMap);
            
            if (matchingEnvironments.isEmpty()) {
                log.warn("No matching logic environment found for physical network: {}, skipping {} instances", 
                        physicalNetwork, instances.size());
                // 如果没有匹配的逻辑环境，使用均分逻辑
                List<Long> allEnvironmentIds = new ArrayList<>(environmentInfoMap.keySet());
                Map<Long, String> environmentToExecutorMap = new HashMap<>();
                for (Long envId : allEnvironmentIds) {
                    LogicEnvironmentInfo info = environmentInfoMap.get(envId);
                    if (info != null && info.getExecutorIp() != null) {
                        environmentToExecutorMap.put(envId, info.getExecutorIp());
                    }
                }
                distributeInstancesEvenly(instances, allEnvironmentIds, environmentToExecutorMap);
                continue;
            }
            
            // 均分用例到匹配的逻辑环境
            int totalInstances = instances.size();
            int environmentCount = matchingEnvironments.size();
            int baseCount = totalInstances / environmentCount;
            int remainder = totalInstances % environmentCount;
            
            log.info("Distributing {} instances for physical network {} to {} environments - base: {}, remainder: {}", 
                    totalInstances, physicalNetwork, environmentCount, baseCount, remainder);
            
            int instanceIndex = 0;
            for (int i = 0; i < matchingEnvironments.size(); i++) {
                Long logicEnvironmentId = matchingEnvironments.get(i);
                LogicEnvironmentInfo info = environmentInfoMap.get(logicEnvironmentId);
                
                if (info == null || info.getExecutorIp() == null) {
                    log.warn("Logic environment {} has no executor IP, skipping", logicEnvironmentId);
                    continue;
                }
                
                int currentEnvironmentCount = baseCount + (i < remainder ? 1 : 0);
                
                log.info("Logic environment {} (executor IP: {}) allocated {} instances for physical network {}", 
                        logicEnvironmentId, info.getExecutorIp(), currentEnvironmentCount, physicalNetwork);
                
                for (int j = 0; j < currentEnvironmentCount; j++) {
                    if (instanceIndex < instances.size()) {
                        TestCaseExecutionInstance instance = instances.get(instanceIndex);
                        instance.setLogicEnvironmentId(logicEnvironmentId);
                        instance.setExecutorIp(info.getExecutorIp());
                        
                        log.debug("Assigned instance to logic environment - test case: {}, round: {}, physical network: {}, logic environment: {}, executor IP: {}", 
                                instance.getTestCaseId(), instance.getRound(), physicalNetwork, logicEnvironmentId, info.getExecutorIp());
                        instanceIndex++;
                    }
                }
            }
        }
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
     * 获取环境到执行机的映射（直接使用执行机表中的IP地址）
     */
    private Map<Long, String> getEnvironmentToExecutorMap(List<Long> logicEnvironmentIds) {
        Map<Long, String> environmentToExecutorMap = new HashMap<>();
        for (Long logicEnvironmentId : logicEnvironmentIds) {
            LogicEnvironmentDTO logicEnvironmentDTO = logicEnvironmentService.getLogicEnvironmentDTO(logicEnvironmentId);
            if (logicEnvironmentDTO != null && logicEnvironmentDTO.getExecutorId() != null) {
                // 直接使用执行机表中的IP地址
                String executorIp = getExecutorIpByMacAddress(logicEnvironmentDTO.getExecutorId());
                if (executorIp != null) {
                    environmentToExecutorMap.put(logicEnvironmentId, executorIp);
                } else {
                    // 如果执行机表中没有IP，则使用逻辑环境DTO中的IP地址（兼容旧逻辑）
                    if (logicEnvironmentDTO.getExecutorIpAddress() != null) {
                        environmentToExecutorMap.put(logicEnvironmentId, logicEnvironmentDTO.getExecutorIpAddress());
                        log.warn("执行机表中IP地址为空，使用逻辑环境DTO中的IP地址 - 执行机ID: {}, IP: {}", 
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
     * 获取执行机IP地址（直接使用执行机表中的IP地址）
     * 
     * @param executorId 执行机ID
     * @return 执行机IP地址
     */
    private String getExecutorIpByMacAddress(Long executorId) {
        try {
            // 获取执行机信息
            Executor executor = executorService.getById(executorId);
            if (executor == null) {
                log.warn("执行机不存在 - 执行机ID: {}", executorId);
                return null;
            }
            
            // 直接使用执行机表中的IP地址
            if (executor.getIpAddress() != null && !executor.getIpAddress().trim().isEmpty()) {
                log.info("获取执行机IP - 执行机ID: {}, IP: {}", executorId, executor.getIpAddress());
                return executor.getIpAddress();
            }
            
            log.warn("执行机IP地址为空 - 执行机ID: {}", executorId);
            return null;
        } catch (Exception e) {
            log.error("获取执行机IP失败 - 执行机ID: {}, 错误: {}", executorId, e.getMessage(), e);
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
            
            // 按逻辑环境分组
            java.util.Map<Long, List<TestCaseExecutionInstance>> instancesByLogicEnvironment = groupInstancesByLogicEnvironment(instances);
            
            // 修改逻辑：所有任务都先进入队列，由定时任务统一处理
            // 将任务加入队列，等待定时任务检查UE状态后下发
            for (java.util.Map.Entry<Long, List<TestCaseExecutionInstance>> entry : instancesByLogicEnvironment.entrySet()) {
                Long logicEnvironmentId = entry.getKey();
                List<TestCaseExecutionInstance> envInstances = entry.getValue();
                
                // 获取该逻辑环境的所有UE
                List<TestCaseExecutionRequest.UeInfo> ueList = getLogicEnvironmentUeList(logicEnvironmentId);
                List<Integer> ueIds = extractUeIds(ueList);
                
                if (ueIds.isEmpty()) {
                    log.warn("逻辑环境没有UE，跳过 - 逻辑环境ID: {}", logicEnvironmentId);
                    continue;
                }
                
                // 将任务加入UE队列，等待定时任务检查UE状态后下发
                String taskId = generateTaskId("queue");
                addTaskToUeQueue(ueIds, logicEnvironmentId, envInstances, taskId);
                log.info("任务已加入UE队列，等待定时任务检查 - 逻辑环境ID: {}, UE IDs: {}, 任务实例数: {}", 
                        logicEnvironmentId, ueIds, envInstances.size());
            }
            
            // 所有任务都已加入队列，返回true表示成功加入队列
            return true;
            
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
     * 按逻辑环境分组实例
     */
    private java.util.Map<Long, List<TestCaseExecutionInstance>> groupInstancesByLogicEnvironment(List<TestCaseExecutionInstance> instances) {
        java.util.Map<Long, List<TestCaseExecutionInstance>> instancesByLogicEnvironment = new java.util.HashMap<>();
        for (TestCaseExecutionInstance instance : instances) {
            if (instance.getLogicEnvironmentId() != null) {
                instancesByLogicEnvironment.computeIfAbsent(instance.getLogicEnvironmentId(), k -> new ArrayList<>()).add(instance);
            } else {
                log.warn("Instance has no logic environment ID - test case: {}, round: {}", instance.getTestCaseId(), instance.getRound());
            }
        }
        return instancesByLogicEnvironment;
    }

    /**
     * 为逻辑环境创建执行任务
     */
    /**
     * 为逻辑环境创建执行任务
     * 改进：允许部分逻辑环境失败，只要至少有一个成功就返回true
     */
    private boolean createExecutionTasksForLogicEnvironments(java.util.Map<Long, List<TestCaseExecutionInstance>> instancesByLogicEnvironment) {
        int totalEnvironments = instancesByLogicEnvironment.size();
        int successCount = 0;
        int failureCount = 0;
        
        for (java.util.Map.Entry<Long, List<TestCaseExecutionInstance>> entry : instancesByLogicEnvironment.entrySet()) {
            Long logicEnvironmentId = entry.getKey();
            List<TestCaseExecutionInstance> environmentInstances = entry.getValue();
            
            // 再次检查任务状态，确保在调用执行机前任务未被停止
            if (!checkTaskStatus(environmentInstances)) {
                log.warn("Task stopped, skip test case distribution for logic environment {}", logicEnvironmentId);
                failureCount++;
                continue;
            }
            
            try {
                boolean success = createExecutionTaskForLogicEnvironment(logicEnvironmentId, environmentInstances);
                if (success) {
                    successCount++;
                    log.info("Successfully created execution task for logic environment - logic environment ID: {}", logicEnvironmentId);
                } else {
                    failureCount++;
                    log.error("Failed to create execution task for logic environment - logic environment ID: {}", logicEnvironmentId);
                    // 继续处理其他逻辑环境，不立即返回false
                }
            } catch (Exception e) {
                failureCount++;
                log.error("Exception creating execution task for logic environment - logic environment ID: {}, error: {}", 
                        logicEnvironmentId, e.getMessage(), e);
                // 继续处理其他逻辑环境
            }
        }
        
        log.info("Logic environment service call completed - total: {}, success: {}, failure: {}", 
                totalEnvironments, successCount, failureCount);
        
        // 只要至少有一个逻辑环境成功，就返回true
        // 这样可以避免因为一个逻辑环境失败而导致整个任务失败
        return successCount > 0;
    }
    
    private boolean createExecutionTaskForLogicEnvironment(Long logicEnvironmentId, List<TestCaseExecutionInstance> instances) {
        try {
            log.info("Create execution task for logic environment - logic environment ID: {}, instance count: {}", logicEnvironmentId, instances.size());
            
            if (!checkTaskStatus(instances)) {
                log.warn("Task stopped, no longer create execution task for logic environment - logic environment ID: {}", logicEnvironmentId);
                return false;
            }
            
            // 获取逻辑环境信息
            LogicEnvironment logicEnvironment = logicEnvironmentService.getById(logicEnvironmentId);
            if (logicEnvironment == null) {
                log.error("Logic environment not found - logic environment ID: {}", logicEnvironmentId);
                return false;
            }
            
            // 获取执行机IP
            String executorIp = null;
            if (logicEnvironment.getExecutorId() != null) {
                Executor executor = executorService.getById(logicEnvironment.getExecutorId());
                if (executor != null && executor.getIpAddress() != null) {
                    executorIp = executor.getIpAddress();
                }
            }
            
            if (executorIp == null || executorIp.trim().isEmpty()) {
                log.error("Executor IP not found for logic environment - logic environment ID: {}", logicEnvironmentId);
                return false;
            }
            
            String taskId = generateTaskId(executorIp);
            
            // 检查任务ID是否已存在，如果存在则生成新的任务ID（避免并发冲突）
            int retryCount = 0;
            while (taskIdToUeIdsMap.containsKey(taskId) && retryCount < 10) {
                log.warn("任务ID冲突，重新生成 - 原任务ID: {}, 执行机IP: {}, 重试次数: {}", taskId, executorIp, retryCount);
                taskId = generateTaskId(executorIp);
                retryCount++;
            }
            if (retryCount >= 10) {
                log.error("任务ID生成失败，重试次数过多 - 执行机IP: {}", executorIp);
                return false;
            }
            
            TestCaseSetInfo testCaseSetInfo = getTestCaseSetInfo(instances);
            if (!validateTestCaseSetInfo(testCaseSetInfo, executorIp)) {
                return false;
            }
            
            List<TestCaseExecutionRequest.TestCaseInfo> testCaseList = buildTestCaseList(instances);
            // 获取逻辑环境绑定的UE信息
            List<TestCaseExecutionRequest.UeInfo> ueList = getLogicEnvironmentUeList(logicEnvironmentId);
            
            // 检查UE是否可用
            if (!checkAndHandleUeAvailability(ueList, logicEnvironmentId)) {
                log.warn("UE不可用，任务将排队等待 - 逻辑环境ID: {}, 任务ID: {}", logicEnvironmentId, taskId);
                // 将任务加入队列，等待UE可用
                addTaskToQueue(logicEnvironmentId, instances, taskId);
                return false; // 返回false表示任务未立即执行，已加入队列
            }
            
            // 尝试获取所有UE的锁，确保使用相同UE的任务不会同时执行
            List<Integer> ueIds = extractUeIds(ueList);
            if (!tryAcquireUeLocks(ueIds, logicEnvironmentId, instances, taskId)) {
                log.warn("无法获取UE锁，任务将排队等待 - 逻辑环境ID: {}, 任务ID: {}, UE IDs: {}", logicEnvironmentId, taskId, ueIds);
                // 将任务加入UE队列，等待UE锁释放
                addTaskToUeQueue(ueIds, logicEnvironmentId, instances, taskId);
                return false; // 返回false表示任务未立即执行，已加入队列
            }
            
            // 记录任务ID和UE ID列表的映射，用于任务完成时释放锁
            taskIdToUeIdsMap.put(taskId, new ArrayList<>(ueIds));
            
            TestCaseExecutionRequest.CollectStrategyInfo collectStrategyInfo = getCollectStrategyInfo(testCaseSetInfo.getCollectStrategyId());
            String taskCustomParams = getTaskCustomParams(instances);
            
            // 获取采集任务的网元ID列表、任务名称、任务描述、网络信息
            List<Long> networkElementIds = null;
            CollectTask collectTask = null;
            String network = null;
            if (!instances.isEmpty()) {
                Long collectTaskId = instances.get(0).getCollectTaskId();
                collectTask = collectTaskService.getCollectTaskById(collectTaskId);
                if (collectTask != null && collectTask.getNetworkElementIds() != null && !collectTask.getNetworkElementIds().trim().isEmpty()) {
                    try {
                        networkElementIds = com.alibaba.fastjson.JSON.parseArray(collectTask.getNetworkElementIds(), Long.class);
                    } catch (Exception e) {
                        log.warn("Failed to parse network element IDs from JSON - Task ID: {}, Error: {}", collectTaskId, e.getMessage());
                    }
                }
            }
            
            // 获取执行机城市信息（拼音）
            String executorCityPinyin = getExecutorCityPinyin(executorIp);
            
            // 获取网络信息（从逻辑环境的物理组网中提取，或从任务自定义参数中获取）
            if (network == null) {
                network = extractNetworkFromLogicEnvironment(logicEnvironment);
            }
            
            logExecutorInfo(executorIp, ueList, collectStrategyInfo, testCaseSetInfo.getCollectStrategyId());
            
            TestCaseExecutionRequest request = buildExecutionRequest(taskId, executorIp, testCaseSetInfo, testCaseList, ueList, collectStrategyInfo, taskCustomParams, networkElementIds, executorCityPinyin, network, collectTask);
            
            // 执行任务
            // 注意：任务执行是异步的，sendExecutionRequest只是发送请求
            // UE锁会在任务真正完成时释放（通过checkAndUpdateTaskCompletion方法）
            boolean success = sendExecutionRequest(request, instances, taskId, executorIp);
            
            // 如果任务发送失败，立即释放UE锁
            if (!success) {
                log.warn("任务发送失败，释放UE锁 - 任务ID: {}, UE IDs: {}", taskId, ueIds);
                releaseUeLocksAndProcessQueue(ueIds);
                // 清除任务ID映射
                taskIdToUeIdsMap.remove(taskId);
            }
            // 如果任务发送成功，锁会在任务完成时释放（在checkAndUpdateTaskCompletion中处理）
            
            return success;
            
        } catch (Exception e) {
            log.error("Exception creating execution task for logic environment - logic environment ID: {}, error: {}", logicEnvironmentId, e.getMessage(), e);
            // 发生异常时，尝试释放UE锁
            try {
                List<Integer> ueIds = extractUeIds(getLogicEnvironmentUeList(logicEnvironmentId));
                releaseUeLocksInternal(ueIds);
            } catch (Exception ex) {
                log.error("Failed to release UE locks after exception - logic environment ID: {}", logicEnvironmentId, ex);
            }
            return false;
        }
    }

    /**
     * 生成唯一的任务ID
     * 使用时间戳、执行机IP、随机数和线程ID确保唯一性
     */
    private String generateTaskId(String executorIp) {
        // 使用时间戳、执行机IP、随机数和线程ID确保唯一性
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        long threadId = Thread.currentThread().getId();
        return "TASK_" + timestamp + "_" + executorIp.replace(".", "_") + "_" + random + "_" + threadId;
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
            // 获取用例信息
            TestCase testCase = testCaseService.getById(instance.getTestCaseId());
            if (testCase != null) {
                TestCaseExecutionRequest.TestCaseInfo testCaseInfo = 
                    new TestCaseExecutionRequest.TestCaseInfo();
                testCaseInfo.setTestCaseId(instance.getTestCaseId());
                testCaseInfo.setTestCaseNumber(testCase.getNumber());
                testCaseInfo.setRound(instance.getRound());
                // 添加用例的业务大类、app、appEn字段
                testCaseInfo.setBusinessCategory(testCase.getBusinessCategory());
                testCaseInfo.setApp(testCase.getApp());
                testCaseInfo.setAppEn(testCase.getAppEn());
                testCaseList.add(testCaseInfo);
                
                log.debug("Built test case info - test case ID: {}, number: {}, businessCategory: {}, app: {}, appEn: {}", 
                        instance.getTestCaseId(), testCase.getNumber(), testCase.getBusinessCategory(), 
                        testCase.getApp(), testCase.getAppEn());
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
            TestCaseExecutionRequest.CollectStrategyInfo collectStrategyInfo, String taskCustomParams, List<Long> networkElementIds,
            String executorCityPinyin, String network, CollectTask collectTask) {
        
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
        
        // 设置执行机城市信息（拼音）
        request.setExecutorCityPinyin(executorCityPinyin);
        
        // 设置网络信息
        request.setNetwork(network);
        
        // 设置采集任务名称和描述
        if (collectTask != null) {
            request.setCollectTaskName(collectTask.getName());
            request.setCollectTaskDescription(collectTask.getDescription());
        }
        
        // 设置网元信息列表
        if (networkElementIds != null && !networkElementIds.isEmpty()) {
            List<TestCaseExecutionRequest.NetworkElementInfo> networkElementInfoList = new ArrayList<>();
            for (Long networkElementId : networkElementIds) {
                try {
                    com.datacollect.dto.NetworkElementDTO networkElementDTO = networkElementService.getNetworkElementWithAttributes(networkElementId);
                    if (networkElementDTO != null && networkElementDTO.getNetworkElement() != null) {
                        TestCaseExecutionRequest.NetworkElementInfo networkElementInfo = new TestCaseExecutionRequest.NetworkElementInfo();
                        networkElementInfo.setId(networkElementDTO.getNetworkElement().getId());
                        networkElementInfo.setName(networkElementDTO.getNetworkElement().getName());
                        networkElementInfo.setDescription(networkElementDTO.getNetworkElement().getDescription());
                        networkElementInfo.setStatus(networkElementDTO.getNetworkElement().getStatus());
                        
                        // 设置网元属性
                        if (networkElementDTO.getAttributes() != null && !networkElementDTO.getAttributes().isEmpty()) {
                            List<TestCaseExecutionRequest.NetworkElementInfo.AttributeInfo> attributeInfos = new ArrayList<>();
                            for (com.datacollect.entity.NetworkElementAttribute attribute : networkElementDTO.getAttributes()) {
                                TestCaseExecutionRequest.NetworkElementInfo.AttributeInfo attrInfo = new TestCaseExecutionRequest.NetworkElementInfo.AttributeInfo();
                                attrInfo.setName(attribute.getAttributeName());
                                attrInfo.setValue(attribute.getAttributeValue());
                                attributeInfos.add(attrInfo);
                            }
                            networkElementInfo.setAttributes(attributeInfos);
                        }
                        
                        networkElementInfoList.add(networkElementInfo);
                        log.info("Network element information added to execution request - Network element ID: {}, Name: {}", 
                                networkElementId, networkElementDTO.getNetworkElement().getName());
                    }
                } catch (Exception e) {
                    log.warn("Failed to get network element information - Network element ID: {}, Error: {}", networkElementId, e.getMessage());
                }
            }
            if (!networkElementInfoList.isEmpty()) {
                request.setNetworkElementInfoList(networkElementInfoList);
                log.info("Network element information list added to execution request - Count: {}", networkElementInfoList.size());
            }
        }
        
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
     * 获取执行机城市信息（拼音）
     * 
     * @param executorIp 执行机IP
     * @return 城市拼音，如果获取失败则返回null
     */
    private String getExecutorCityPinyin(String executorIp) {
        try {
            // 通过执行机IP获取执行机信息
            QueryWrapper<Executor> executorQuery = new QueryWrapper<>();
            executorQuery.eq("ip_address", executorIp);
            Executor executor = executorService.getOne(executorQuery);
            
            if (executor == null || executor.getRegionId() == null) {
                log.warn("Executor not found or region ID is null - Executor IP: {}", executorIp);
                return null;
            }
            
            // 获取地域信息
            com.datacollect.entity.Region region = regionService.getById(executor.getRegionId());
            if (region == null) {
                log.warn("Region not found - Region ID: {}", executor.getRegionId());
                return null;
            }
            
            // 如果地域级别是城市（level=4），直接使用；否则查找城市
            String cityName = null;
            if (region.getLevel() != null && region.getLevel() == 4) {
                cityName = region.getName();
            } else {
                // 如果不是城市，尝试查找城市（向上查找或向下查找）
                // 这里简化处理，如果level=3（省份），尝试查找其子城市
                if (region.getLevel() != null && region.getLevel() == 3) {
                    // 查找该省份下的第一个城市
                    QueryWrapper<com.datacollect.entity.Region> cityQuery = new QueryWrapper<>();
                    cityQuery.eq("parent_id", region.getId());
                    cityQuery.eq("level", 4);
                    cityQuery.last("LIMIT 1");
                    com.datacollect.entity.Region city = regionService.getOne(cityQuery);
                    if (city != null) {
                        cityName = city.getName();
                    }
                }
            }
            
            if (cityName == null || cityName.trim().isEmpty()) {
                log.warn("City name not found for executor - Executor IP: {}, Region ID: {}", executorIp, executor.getRegionId());
                return null;
            }
            
            // 将城市名称转换为拼音
            String pinyin = com.datacollect.util.PinyinUtil.toPinyin(cityName);
            log.info("Executor city pinyin - Executor IP: {}, City: {}, Pinyin: {}", executorIp, cityName, pinyin);
            return pinyin;
            
        } catch (Exception e) {
            log.error("Failed to get executor city pinyin - Executor IP: {}, Error: {}", executorIp, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从逻辑环境的物理组网中提取网络信息
     * 
     * @param logicEnvironment 逻辑环境
     * @return 网络信息，如果提取失败则返回null
     */
    private String extractNetworkFromLogicEnvironment(LogicEnvironment logicEnvironment) {
        if (logicEnvironment == null || logicEnvironment.getPhysicalNetwork() == null 
                || logicEnvironment.getPhysicalNetwork().trim().isEmpty()) {
            return null;
        }
        
        try {
            // 解析物理组网JSON数组
            List<String> physicalNetworks = JSON.parseArray(logicEnvironment.getPhysicalNetwork(), String.class);
            if (physicalNetworks == null || physicalNetworks.isEmpty()) {
                return null;
            }
            
            // 物理组网格式：逻辑组网_网络_厂商
            // 从第一个物理组网中提取网络信息
            String firstPhysicalNetwork = physicalNetworks.get(0);
            if (firstPhysicalNetwork != null && firstPhysicalNetwork.contains("_")) {
                String[] parts = firstPhysicalNetwork.split("_");
                if (parts.length >= 2) {
                    // parts[1] 是网络信息
                    String network = parts[1];
                    log.info("Extracted network from logic environment - Logic Environment ID: {}, Network: {}", 
                            logicEnvironment.getId(), network);
                    return network;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract network from logic environment - Logic Environment ID: {}, Error: {}", 
                    logicEnvironment.getId(), e.getMessage());
        }
        
        return null;
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
     * 发送执行请求
     */
    private boolean sendExecutionRequest(TestCaseExecutionRequest request, List<TestCaseExecutionInstance> instances, String taskId, String executorIp) {
        // 通过IP地址查找执行机，获取MAC地址
        String executorMac = getExecutorMacAddress(executorIp);
        if (executorMac == null || executorMac.trim().isEmpty()) {
            log.warn("无法获取执行机MAC地址，使用HTTP发送任务 - 执行机IP: {}, 任务ID: {}", executorIp, taskId);
        } else {
        // 优先使用WebSocket发送任务
            if (executorWebSocketService.isExecutorOnline(executorMac)) {
                log.info("执行机在线，通过WebSocket发送任务 - 执行机MAC地址: {}, 执行机IP: {}, 任务ID: {}", executorMac, executorIp, taskId);
                boolean sent = executorWebSocketService.sendTaskToExecutor(executorMac, request);
            if (sent) {
                // 更新例次状态
                updateInstanceStatus(instances, taskId);
                return true;
            } else {
                    log.warn("WebSocket发送任务失败，尝试使用HTTP发送 - 执行机MAC地址: {}, 执行机IP: {}, 任务ID: {}", executorMac, executorIp, taskId);
            }
        } else {
                log.info("执行机不在线，使用HTTP发送任务 - 执行机MAC地址: {}, 执行机IP: {}, 任务ID: {}", executorMac, executorIp, taskId);
            }
        }
        
        // 如果WebSocket不可用，回退到HTTP请求
        String caseExecuteServiceUrl = "http://" + executorIp + ":8081/api/test-case-execution/receive";
        log.info("Calling CaseExecuteService via HTTP - URL: {}, task ID: {}", caseExecuteServiceUrl, taskId);
        
        try {
            ResponseEntity<Map> response = httpClientUtil.post(caseExecuteServiceUrl, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.getBody();
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
     * 获取逻辑环境绑定的UE信息
     * 
     * @param logicEnvironmentId 逻辑环境ID
     * @return UE信息列表
     */
    private List<TestCaseExecutionRequest.UeInfo> getLogicEnvironmentUeList(Long logicEnvironmentId) {
        List<TestCaseExecutionRequest.UeInfo> ueList = new ArrayList<>();
        
        try {
            // 1. 获取逻辑环境关联的UE ID列表
            QueryWrapper<LogicEnvironmentUe> ueQuery = new QueryWrapper<>();
            ueQuery.eq("logic_environment_id", logicEnvironmentId);
            List<LogicEnvironmentUe> logicEnvironmentUes = logicEnvironmentUeService.list(ueQuery);
            
            if (logicEnvironmentUes.isEmpty()) {
                log.warn("Logic environment not associated with UE - logic environment ID: {}", logicEnvironmentId);
                return ueList;
            }
            
            // 2. 提取UE ID集合
            Set<Long> ueIds = logicEnvironmentUes.stream()
                    .map(LogicEnvironmentUe::getUeId)
                    .collect(java.util.stream.Collectors.toSet());
            
            // 3. 获取UE详细信息并转换为DTO格式
            ueList = convertUesToDto(ueIds);
            
            log.info("Successfully got UE information for logic environment - logic environment ID: {}, UE count: {}", logicEnvironmentId, ueList.size());
            
        } catch (Exception e) {
            log.error("Failed to get UE information for logic environment - logic environment ID: {}, error: {}", logicEnvironmentId, e.getMessage(), e);
        }
        
        return ueList;
    }
    
    /**
     * 获取执行机关联的UE全部信息（保留此方法用于兼容，但不再使用）
     * 
     * @param executorIp 执行机IP
     * @return UE信息列表
     * @deprecated 请使用 getLogicEnvironmentUeList 方法
     */
    @Deprecated
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
        ueInfo.setModel(ue.getModel());
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
    
    /**
     * 检查UE是否可用，如果不可用则标记逻辑环境为不可用
     * 
     * @param ueList UE信息列表
     * @param logicEnvironmentId 逻辑环境ID
     * @return true表示UE可用，false表示UE不可用
     */
    private boolean checkAndHandleUeAvailability(List<TestCaseExecutionRequest.UeInfo> ueList, Long logicEnvironmentId) {
        if (ueList == null || ueList.isEmpty()) {
            log.warn("UE列表为空，无法检查可用性 - 逻辑环境ID: {}", logicEnvironmentId);
            return false;
        }
        
        // 提取UE ID列表（转换为Integer）
        List<Integer> ueIds = new ArrayList<>();
        for (TestCaseExecutionRequest.UeInfo ueInfo : ueList) {
            if (ueInfo.getId() != null) {
                ueIds.add(ueInfo.getId().intValue());
            }
        }
        
        if (ueIds.isEmpty()) {
            log.warn("UE ID列表为空，无法检查可用性 - 逻辑环境ID: {}", logicEnvironmentId);
            return false;
        }
        
        // 检查UE是否可用
        List<Integer> unavailableUeIds = ueService.checkUesAvailability(ueIds);
        
        if (!unavailableUeIds.isEmpty()) {
            log.warn("部分UE不可用 - 逻辑环境ID: {}, 不可用UE IDs: {}", logicEnvironmentId, unavailableUeIds);
            
            // 检查配置：如果配置为false，则不禁用环境
            boolean shouldDisableEnvironment = true; // 默认值
            if (configService != null) {
                Boolean configValue = configService.getUeDisableEnvironmentWhenInUse();
                if (configValue != null) {
                    shouldDisableEnvironment = configValue;
                }
            }
            
            // 根据配置决定是否禁用逻辑环境
            if (shouldDisableEnvironment) {
                // 标记逻辑环境为不可用
                LogicEnvironment logicEnvironment = logicEnvironmentService.getById(logicEnvironmentId);
                if (logicEnvironment != null) {
                    logicEnvironment.setStatus(0); // 0: 不可用
                    logicEnvironmentService.updateById(logicEnvironment);
                    log.info("UE使用中，逻辑环境已标记为不可用 - 逻辑环境ID: {}", logicEnvironmentId);
                }
            } else {
                log.debug("配置为不禁用环境，跳过逻辑环境状态更新 - 逻辑环境ID: {}", logicEnvironmentId);
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * 提取UE ID列表
     * 
     * @param ueList UE信息列表
     * @return UE ID列表
     */
    private List<Integer> extractUeIds(List<TestCaseExecutionRequest.UeInfo> ueList) {
        List<Integer> ueIds = new ArrayList<>();
        if (ueList != null) {
            for (TestCaseExecutionRequest.UeInfo ueInfo : ueList) {
                if (ueInfo.getId() != null) {
                    ueIds.add(ueInfo.getId().intValue());
                }
            }
        }
        return ueIds;
    }
    
    /**
     * 尝试获取所有UE的锁
     * 
     * @param ueIds UE ID列表
     * @param logicEnvironmentId 逻辑环境ID
     * @param instances 执行实例列表
     * @param taskId 任务ID
     * @return true表示成功获取所有锁，false表示无法获取
     */
    private boolean tryAcquireUeLocks(List<Integer> ueIds, Long logicEnvironmentId, 
                                      List<TestCaseExecutionInstance> instances, String taskId) {
        if (ueIds == null || ueIds.isEmpty()) {
            log.warn("UE ID列表为空，无需获取锁 - 逻辑环境ID: {}, 任务ID: {}", logicEnvironmentId, taskId);
            return true; // 没有UE，直接返回true
        }
        
        // 首先检查UE是否被其他任务占用（使用占用状态检查）
        for (Integer ueId : ueIds) {
            String occupiedBy = ueOccupiedByTask.get(ueId);
            if (occupiedBy != null && !occupiedBy.equals(taskId)) {
                log.warn("UE被其他任务占用 - UE ID: {}, 占用任务ID: {}, 当前任务ID: {}", ueId, occupiedBy, taskId);
                return false;
            }
        }
        
        List<ReentrantLock> acquiredLocks = new ArrayList<>();
        List<Integer> acquiredUeIds = new ArrayList<>();
        
        try {
            // 尝试获取所有UE的锁
            // 注意：由于锁可能在不同线程中获取和释放，我们主要依赖 ueOccupiedByTask 来跟踪占用状态
            // ReentrantLock 主要用于同一线程内的同步，跨线程同步通过 ueOccupiedByTask 实现
            for (Integer ueId : ueIds) {
                ReentrantLock lock = ueLocks.computeIfAbsent(ueId, k -> new ReentrantLock());
                String occupiedBy = ueOccupiedByTask.get(ueId);
                
                // 首先检查占用状态（这是主要的同步机制）
                if (occupiedBy != null && !occupiedBy.equals(taskId)) {
                    log.warn("UE被其他任务占用 - UE ID: {}, 占用任务ID: {}, 当前任务ID: {}", ueId, occupiedBy, taskId);
                    // 释放已获取的锁
                    releaseAcquiredLocksAndClearOccupied(acquiredLocks, acquiredUeIds);
                    return false;
                }
                
                // 如果占用状态显示该UE可用（或已被当前任务占用），尝试获取锁
                // 注意：即使 ReentrantLock 被其他线程持有，只要 ueOccupiedByTask 中没有记录，我们也应该尝试获取
                boolean lockAcquired = false;
                
                // 检查 ReentrantLock 的状态
                boolean isLocked = lock.isLocked();
                boolean isHeldByCurrentThread = lock.isHeldByCurrentThread();
                
                if (isLocked && !isHeldByCurrentThread) {
                    // 锁被其他线程持有，但占用状态已清除（可能是之前的任务没有正确释放）
                    // 这种情况下，我们无法直接释放其他线程持有的锁
                    // 解决方案：创建一个新的锁对象来替换旧的，让系统恢复
                    if (occupiedBy == null) {
                        log.warn("UE锁被其他线程持有但占用状态已清除，检测到锁泄漏 - UE ID: {}, 任务ID: {}", ueId, taskId);
                        log.info("尝试恢复UE锁 - 创建新的锁对象替换旧的 - UE ID: {}", ueId);
                        
                        // 创建新的锁对象来替换旧的
                        ReentrantLock newLock = new ReentrantLock();
                        ueLocks.put(ueId, newLock);
                        lock = newLock;
                        
                        log.info("UE锁已恢复 - UE ID: {}, 任务ID: {}", ueId, taskId);
                        // 继续尝试获取新锁
                    }
                }
                
                // 尝试获取锁
                if (lock.tryLock()) {
                    lockAcquired = true;
                } else if (occupiedBy == null && !isLocked) {
                    // 占用状态显示可用，但 tryLock 失败且锁未被持有
                    // 这可能是并发竞争，重试一次
                    if (lock.tryLock()) {
                        lockAcquired = true;
                    }
                }
                
                if (lockAcquired) {
                    acquiredLocks.add(lock);
                    acquiredUeIds.add(ueId);
                    // 记录UE被该任务占用（这是主要的同步机制）
                    ueOccupiedByTask.put(ueId, taskId);
                    log.debug("成功获取UE锁 - UE ID: {}, 任务ID: {}", ueId, taskId);
                } else {
                    log.warn("无法获取UE锁 - UE ID: {}, 任务ID: {}, 锁状态: 被持有={}, 当前线程持有={}, 占用任务={}", 
                            ueId, taskId, lock.isLocked(), lock.isHeldByCurrentThread(), ueOccupiedByTask.get(ueId));
                    // 无法获取锁，释放已获取的锁
                    releaseAcquiredLocksAndClearOccupied(acquiredLocks, acquiredUeIds);
                    return false;
                }
            }
            
            log.info("成功获取所有UE锁 - UE IDs: {}, 任务ID: {}", ueIds, taskId);
            return true;
            
        } catch (Exception e) {
            log.error("获取UE锁时发生异常 - UE IDs: {}, 任务ID: {}, 错误: {}", ueIds, taskId, e.getMessage(), e);
            // 发生异常时，释放已获取的锁
            releaseAcquiredLocksAndClearOccupied(acquiredLocks, acquiredUeIds);
            return false;
        }
    }
    
    /**
     * 释放已获取的锁并清除占用状态
     * 
     * @param locks 已获取的锁列表
     * @param ueIds 已获取锁的UE ID列表
     */
    private void releaseAcquiredLocksAndClearOccupied(List<ReentrantLock> locks, List<Integer> ueIds) {
        for (int i = 0; i < locks.size() && i < ueIds.size(); i++) {
            ReentrantLock lock = locks.get(i);
            Integer ueId = ueIds.get(i);
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    // 清除占用状态
                    ueOccupiedByTask.remove(ueId);
                    log.debug("释放UE锁并清除占用状态 - UE ID: {}", ueId);
                }
            } catch (Exception e) {
                log.error("释放UE锁失败 - UE ID: {}, 错误: {}", ueId, e.getMessage(), e);
            }
        }
    }
    
    /**
     * 释放已获取的锁（兼容旧代码）
     * 
     * @param locks 已获取的锁列表
     */
    private void releaseAcquiredLocks(List<ReentrantLock> locks) {
        for (ReentrantLock lock : locks) {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (Exception e) {
                log.error("释放UE锁失败 - 错误: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 释放UE锁（内部方法）
     * 注意：由于任务完成可能在不同线程中处理，我们使用占用状态来跟踪，而不是依赖线程检查
     * 
     * @param ueIds UE ID列表
     */
    private void releaseUeLocksInternal(List<Integer> ueIds) {
        if (ueIds == null || ueIds.isEmpty()) {
            return;
        }
        
        for (Integer ueId : ueIds) {
            try {
                ReentrantLock lock = ueLocks.get(ueId);
                if (lock != null) {
                    // 检查锁是否被持有
                    boolean isLocked = lock.isLocked();
                    boolean isHeldByCurrentThread = lock.isHeldByCurrentThread();
                    
                    log.debug("释放UE锁 - UE ID: {}, 锁是否被持有: {}, 当前线程是否持有: {}", 
                            ueId, isLocked, isHeldByCurrentThread);
                    
                    // 尝试释放锁
                    // 注意：由于锁可能在不同线程中获取和释放，我们主要依赖 ueOccupiedByTask 来跟踪占用状态
                    // 如果当前线程持有锁，则正常释放；否则只清除占用状态
                    try {
                        if (isHeldByCurrentThread) {
                            // 当前线程持有锁，正常释放
                            lock.unlock();
                            log.debug("释放UE锁（当前线程持有） - UE ID: {}", ueId);
                        } else if (isLocked) {
                            // 锁被其他线程持有，这是正常情况（任务完成可能在不同线程中处理）
                            // 我们无法释放其他线程持有的锁，但可以清除占用状态
                            // 这样其他任务就可以通过检查 ueOccupiedByTask 来判断UE是否可用
                            log.debug("UE锁被其他线程持有，仅清除占用状态 - UE ID: {}", ueId);
                        } else {
                            // 锁未被持有，只需要清除占用状态
                            log.debug("UE锁未被持有，仅清除占用状态 - UE ID: {}", ueId);
                        }
                    } catch (IllegalMonitorStateException e) {
                        // 锁未被当前线程持有，这是正常的（任务完成可能在不同线程中）
                        log.debug("UE锁未被当前线程持有（正常情况） - UE ID: {}", ueId);
                    }
                }
                
                // 无论锁是否被释放，都清除占用状态
                // 这是主要的同步机制，清除后其他任务就可以尝试获取这个UE了
                // 注意：即使 ReentrantLock 仍被其他线程持有，只要 ueOccupiedByTask 中没有记录，
                // 其他任务在获取锁时会检测到这种情况并处理
                ueOccupiedByTask.remove(ueId);
                log.debug("清除UE占用状态 - UE ID: {}", ueId);
                
            } catch (Exception e) {
                log.error("释放UE锁失败 - UE ID: {}, 错误: {}", ueId, e.getMessage(), e);
                // 即使出错也尝试清除占用状态
                ueOccupiedByTask.remove(ueId);
            }
        }
    }
    
    /**
     * 释放UE锁并处理UE队列中的任务
     * 
     * @param ueIds UE ID列表
     */
    private void releaseUeLocksAndProcessQueue(List<Integer> ueIds) {
        if (ueIds == null || ueIds.isEmpty()) {
            return;
        }
        
        // 释放锁
        releaseUeLocksInternal(ueIds);
        
        // 处理每个UE队列中的任务
        for (Integer ueId : ueIds) {
            try {
                processUeQueue(ueId);
            } catch (Exception e) {
                log.error("处理UE队列任务失败 - UE ID: {}, 错误: {}", ueId, e.getMessage(), e);
            }
        }
    }
    
    /**
     * 将任务加入UE队列，等待UE锁释放
     * 
     * @param ueIds UE ID列表
     * @param logicEnvironmentId 逻辑环境ID
     * @param instances 执行实例列表
     * @param taskId 任务ID
     */
    private void addTaskToUeQueue(List<Integer> ueIds, Long logicEnvironmentId, 
                                  List<TestCaseExecutionInstance> instances, String taskId) {
        if (ueIds == null || ueIds.isEmpty()) {
            return;
        }
        
        try {
            // 获取任务ID并更新任务状态为等待中
            if (!instances.isEmpty()) {
                Long collectTaskId = instances.get(0).getCollectTaskId();
                if (collectTaskId != null) {
                    CollectTask collectTask = collectTaskService.getCollectTaskById(collectTaskId);
                    if (collectTask != null && !"WAITING".equals(collectTask.getStatus()) && !"STOPPED".equals(collectTask.getStatus())) {
                        // 只有在任务不是等待中或已停止时，才更新为等待中
                        collectTaskService.updateTaskStatus(collectTaskId, "WAITING");
                        log.info("任务状态已更新为等待中 - 任务ID: {}, 原因: UE被占用", collectTaskId);
                    }
                }
            }
            
            // 将任务加入所有相关UE的队列（使用第一个UE作为主队列）
            Integer primaryUeId = ueIds.get(0);
            BlockingQueue<QueuedTask> queue = ueTaskQueues.computeIfAbsent(primaryUeId, k -> new LinkedBlockingQueue<>());
            
            QueuedTask queuedTask = new QueuedTask(taskId, instances, logicEnvironmentId);
            queue.offer(queuedTask);
            
            log.info("任务已加入UE队列 - 主UE ID: {}, 所有UE IDs: {}, 任务ID: {}, 队列大小: {}", 
                    primaryUeId, ueIds, taskId, queue.size());
            
            // 启动UE队列处理器（如果还没有启动）
            startUeQueueProcessor(primaryUeId);
            
        } catch (Exception e) {
            log.error("将任务加入UE队列失败 - UE IDs: {}, 任务ID: {}, 错误: {}", ueIds, taskId, e.getMessage(), e);
        }
    }
    
    /**
     * 启动UE队列处理器，异步处理排队任务
     * 使用标志位防止重复启动
     * 
     * @param ueId UE ID
     */
    private void startUeQueueProcessor(Integer ueId) {
        // 检查是否已经有处理器在运行
        if (ueQueueProcessorRunning.putIfAbsent(ueId, true) != null) {
            log.debug("UE队列处理器已在运行，跳过启动 - UE ID: {}", ueId);
            return;
        }
        
        // 使用CompletableFuture异步处理队列
        CompletableFuture.runAsync(() -> {
            try {
                BlockingQueue<QueuedTask> queue = ueTaskQueues.get(ueId);
                if (queue == null) {
                    return;
                }
                
                // 持续处理队列，直到队列为空且没有新任务加入
                // 使用循环处理，确保能处理所有排队任务
                int emptyCount = 0; // 连续空队列次数
                while (true) {
                    try {
                        QueuedTask queuedTask = queue.peek(); // 查看队列头部任务，不取出
                        if (queuedTask == null) {
                            // 队列为空，检查是否需要退出
                            emptyCount++;
                            if (emptyCount >= 3) {
                                // 连续3次检查队列都为空，退出处理器
                                log.debug("UE队列连续为空，退出处理器 - UE ID: {}", ueId);
                                break;
                            }
                            Thread.sleep(2000); // 等待2秒后重试
                            continue;
                        }
                        
                        // 重置空队列计数
                        emptyCount = 0;
                        
                        // 在尝试执行前，先检查任务状态
                        if (!checkTaskStatus(queuedTask.getInstances())) {
                            // 任务已停止，从队列中移除并记录日志
                            QueuedTask removedTask = queue.poll();
                            if (removedTask != null) {
                                log.info("任务在队列中等待时已被停止，从UE队列中移除 - UE ID: {}, 任务ID: {}", ueId, removedTask.getTaskId());
                            }
                            continue; // 继续处理下一个任务
                        }
                        
                        // 获取任务使用的所有UE ID
                        List<TestCaseExecutionRequest.UeInfo> ueList = getLogicEnvironmentUeList(queuedTask.getLogicEnvironmentId());
                        List<Integer> taskUeIds = extractUeIds(ueList);
                        
                        // 尝试获取所有UE的锁
                        if (tryAcquireUeLocks(taskUeIds, queuedTask.getLogicEnvironmentId(), 
                                              queuedTask.getInstances(), queuedTask.getTaskId())) {
                            // 成功获取锁，取出任务并执行
                            QueuedTask task = queue.poll();
                            if (task != null) {
                                // 再次检查任务状态（可能在获取锁的过程中任务被停止）
                                if (!checkTaskStatus(task.getInstances())) {
                                    log.info("任务在获取锁后已被停止，取消执行 - UE ID: {}, 任务ID: {}", ueId, task.getTaskId());
                                    // 释放已获取的锁
                                    releaseUeLocksInternal(taskUeIds);
                                    continue;
                                }
                                
                                log.info("从UE队列中取出任务执行 - UE ID: {}, 任务ID: {}", ueId, task.getTaskId());
                                
                                // 更新任务状态为运行中（如果任务在等待中）
                                if (!task.getInstances().isEmpty()) {
                                    Long collectTaskId = task.getInstances().get(0).getCollectTaskId();
                                    if (collectTaskId != null) {
                                        CollectTask collectTask = collectTaskService.getCollectTaskById(collectTaskId);
                                        if (collectTask != null && "WAITING".equals(collectTask.getStatus())) {
                                            collectTaskService.updateTaskStatus(collectTaskId, "RUNNING");
                                            log.info("任务状态已从等待中更新为运行中 - 任务ID: {}", collectTaskId);
                                        }
                                    }
                                }
                                
                                // 重新执行任务创建流程
                                createExecutionTaskForLogicEnvironment(task.getLogicEnvironmentId(), task.getInstances());
                            }
                        } else {
                            // 无法获取锁，等待一段时间后重试
                            Thread.sleep(5000); // 等待5秒后重试
                        }
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("UE队列处理器被中断 - UE ID: {}", ueId);
                        break;
                    } catch (Exception e) {
                        log.error("处理UE队列任务失败 - UE ID: {}, 错误: {}", ueId, e.getMessage(), e);
                        // 发生错误时，等待一段时间后继续
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            } finally {
                // 处理完成后，清除运行标志
                ueQueueProcessorRunning.remove(ueId);
                log.debug("UE队列处理器处理完成 - UE ID: {}", ueId);
                
                // 检查队列中是否还有任务，如果有则重新启动处理器
                BlockingQueue<QueuedTask> queue = ueTaskQueues.get(ueId);
                if (queue != null && !queue.isEmpty()) {
                    log.info("UE队列中还有任务，重新启动处理器 - UE ID: {}, 队列大小: {}", ueId, queue.size());
                    startUeQueueProcessor(ueId);
                }
            }
        });
    }
    
    /**
     * 释放任务对应的UE锁
     * 当任务完成时调用此方法，释放任务占用的UE锁，并处理UE队列中的任务
     * 
     * @param taskId 任务ID
     */
    @Override
    public void releaseUeLocksForTask(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            log.warn("任务ID为空，无法释放UE锁");
            return;
        }
        
        List<Integer> ueIds = taskIdToUeIdsMap.remove(taskId);
        if (ueIds == null || ueIds.isEmpty()) {
            log.debug("任务未占用UE锁或锁已释放 - 任务ID: {}", taskId);
            return;
        }
        
        log.info("任务完成，释放UE锁 - 任务ID: {}, UE IDs: {}", taskId, ueIds);
        releaseUeLocksAndProcessQueue(ueIds);
    }
    
    /**
     * 处理UE队列中的任务
     * 
     * @param ueId UE ID
     */
    private void processUeQueue(Integer ueId) {
        BlockingQueue<QueuedTask> queue = ueTaskQueues.get(ueId);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        
        log.info("UE锁释放，检查排队任务 - UE ID: {}, 队列大小: {}", ueId, queue.size());
        startUeQueueProcessor(ueId);
    }
    
    /**
     * 释放UE锁
     * 当UE被手动释放时调用此方法，释放UE锁并处理队列中的任务
     * 
     * @param ueIds UE ID列表
     */
    @Override
    public void releaseUeLocks(List<Integer> ueIds) {
        if (ueIds == null || ueIds.isEmpty()) {
            return;
        }
        
        log.info("手动释放UE锁 - UE IDs: {}", ueIds);
        releaseUeLocksAndProcessQueue(ueIds);
    }
    
    /**
     * 将任务加入队列，等待UE可用
     * 
     * @param logicEnvironmentId 逻辑环境ID
     * @param instances 执行实例列表
     * @param taskId 任务ID
     */
    private void addTaskToQueue(Long logicEnvironmentId, List<TestCaseExecutionInstance> instances, String taskId) {
        try {
            // 获取任务ID并更新任务状态为等待中
            if (!instances.isEmpty()) {
                Long collectTaskId = instances.get(0).getCollectTaskId();
                if (collectTaskId != null) {
                    CollectTask collectTask = collectTaskService.getCollectTaskById(collectTaskId);
                    if (collectTask != null && !"WAITING".equals(collectTask.getStatus()) && !"STOPPED".equals(collectTask.getStatus())) {
                        // 只有在任务不是等待中或已停止时，才更新为等待中
                        collectTaskService.updateTaskStatus(collectTaskId, "WAITING");
                        log.info("任务状态已更新为等待中 - 任务ID: {}, 原因: UE不可用", collectTaskId);
                    }
                }
            }
            
            // 获取或创建该逻辑环境的任务队列
            BlockingQueue<QueuedTask> queue = taskQueues.computeIfAbsent(logicEnvironmentId, k -> new LinkedBlockingQueue<>());
            
            // 创建排队任务
            QueuedTask queuedTask = new QueuedTask(taskId, instances, logicEnvironmentId);
            
            // 加入队列
            queue.offer(queuedTask);
            log.info("任务已加入队列 - 逻辑环境ID: {}, 任务ID: {}, 队列大小: {}", logicEnvironmentId, taskId, queue.size());
            
            // 启动异步任务处理队列（如果还没有启动）
            startQueueProcessor(logicEnvironmentId);
            
        } catch (Exception e) {
            log.error("将任务加入队列失败 - 逻辑环境ID: {}, 任务ID: {}, 错误: {}", logicEnvironmentId, taskId, e.getMessage(), e);
        }
    }
    
    /**
     * 启动队列处理器，异步处理排队任务
     * 
     * @param logicEnvironmentId 逻辑环境ID
     */
    private void startQueueProcessor(Long logicEnvironmentId) {
        // 使用CompletableFuture异步处理队列
        CompletableFuture.runAsync(() -> {
            // 使用 computeIfAbsent 确保队列存在，如果不存在则创建
            BlockingQueue<QueuedTask> queue = taskQueues.computeIfAbsent(logicEnvironmentId, k -> new LinkedBlockingQueue<>());
            if (queue == null) {
                log.warn("无法创建或获取队列 - 逻辑环境ID: {}", logicEnvironmentId);
                return;
            }
            
            log.debug("开始处理队列 - 逻辑环境ID: {}, 队列大小: {}", logicEnvironmentId, queue.size());
            
            while (!queue.isEmpty()) {
                try {
                    QueuedTask queuedTask = queue.peek(); // 查看队列头部任务，不取出
                    if (queuedTask == null) {
                        break;
                    }
                    
                    // 在尝试执行前，先检查任务状态
                    if (!checkTaskStatus(queuedTask.getInstances())) {
                        // 任务已停止，从队列中移除并记录日志
                        QueuedTask removedTask = queue.poll();
                        if (removedTask != null) {
                            log.info("任务在队列中等待时已被停止，从队列中移除 - 逻辑环境ID: {}, 任务ID: {}", logicEnvironmentId, removedTask.getTaskId());
                        }
                        continue; // 继续处理下一个任务
                    }
                    
                    // 检查UE是否可用
                    List<TestCaseExecutionRequest.UeInfo> ueList = getLogicEnvironmentUeList(logicEnvironmentId);
                    if (checkUeAvailabilityForQueue(ueList, logicEnvironmentId)) {
                        // UE可用，取出任务并执行
                        QueuedTask task = queue.poll();
                        if (task != null) {
                            // 再次检查任务状态（可能在检查UE可用性的过程中任务被停止）
                            if (!checkTaskStatus(task.getInstances())) {
                                log.info("任务在检查UE可用性后已被停止，取消执行 - 逻辑环境ID: {}, 任务ID: {}", logicEnvironmentId, task.getTaskId());
                                continue;
                            }
                            
                            log.info("从队列中取出任务执行 - 逻辑环境ID: {}, 任务ID: {}", logicEnvironmentId, task.getTaskId());
                            
                            // 更新任务状态为运行中（如果任务在等待中）
                            if (!task.getInstances().isEmpty()) {
                                Long collectTaskId = task.getInstances().get(0).getCollectTaskId();
                                if (collectTaskId != null) {
                                    CollectTask collectTask = collectTaskService.getCollectTaskById(collectTaskId);
                                    if (collectTask != null && "WAITING".equals(collectTask.getStatus())) {
                                        collectTaskService.updateTaskStatus(collectTaskId, "RUNNING");
                                        log.info("任务状态已从等待中更新为运行中 - 任务ID: {}", collectTaskId);
                                    }
                                }
                            }
                            
                            // 重新执行任务创建流程
                            createExecutionTaskForLogicEnvironment(logicEnvironmentId, task.getInstances());
                        }
                    } else {
                        // UE不可用，等待一段时间后重试
                        Thread.sleep(5000); // 等待5秒后重试
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("队列处理器被中断 - 逻辑环境ID: {}", logicEnvironmentId);
                    break;
                } catch (Exception e) {
                    log.error("处理队列任务失败 - 逻辑环境ID: {}, 错误: {}", logicEnvironmentId, e.getMessage(), e);
                    // 发生错误时，等待一段时间后继续
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
    }
    
    /**
     * 检查UE是否可用（用于队列处理，不更新逻辑环境状态）
     * 
     * @param ueList UE信息列表
     * @param logicEnvironmentId 逻辑环境ID
     * @return true表示UE可用，false表示UE不可用
     */
    private boolean checkUeAvailabilityForQueue(List<TestCaseExecutionRequest.UeInfo> ueList, Long logicEnvironmentId) {
        if (ueList == null || ueList.isEmpty()) {
            return false;
        }
        
        // 提取UE ID列表（转换为Integer）
        List<Integer> ueIds = new ArrayList<>();
        for (TestCaseExecutionRequest.UeInfo ueInfo : ueList) {
            if (ueInfo.getId() != null) {
                ueIds.add(ueInfo.getId().intValue());
            }
        }
        
        if (ueIds.isEmpty()) {
            return false;
        }
        
        // 检查UE是否可用
        List<Integer> unavailableUeIds = ueService.checkUesAvailability(ueIds);
        
        return unavailableUeIds.isEmpty();
    }
    
    /**
     * 处理UE状态更新后的队列任务
     * 当UE从使用中变为可用时，检查是否有排队任务需要执行
     * 
     * @param ueIds 更新的UE ID列表（Long类型，因为从UeServiceImpl传入）
     */
    public void processQueuedTasksAfterUeAvailable(List<Integer> ueIds) {
        if (ueIds == null || ueIds.isEmpty()) {
            return;
        }
        
        try {
            // 首先处理UE级别的队列（优先级更高）
            for (Integer ueId : ueIds) {
                try {
                    processUeQueue(ueId);
                } catch (Exception e) {
                    log.error("处理UE队列失败 - UE ID: {}, 错误: {}", ueId, e.getMessage(), e);
                }
            }
            
            // 然后查找包含这些UE的逻辑环境
            QueryWrapper<LogicEnvironmentUe> ueQuery = new QueryWrapper<>();
            ueQuery.in("ue_id", ueIds);
            List<LogicEnvironmentUe> logicEnvironmentUes = logicEnvironmentUeService.list(ueQuery);
            
            if (logicEnvironmentUes.isEmpty()) {
                return;
            }
            
            // 获取所有相关的逻辑环境ID
            Set<Long> logicEnvironmentIds = logicEnvironmentUes.stream()
                    .map(LogicEnvironmentUe::getLogicEnvironmentId)
                    .collect(java.util.stream.Collectors.toSet());
            
            // 为每个逻辑环境启动队列处理器
            for (Long logicEnvironmentId : logicEnvironmentIds) {
                // 使用 computeIfAbsent 确保队列存在，如果不存在则创建（可能队列还没有被创建）
                BlockingQueue<QueuedTask> queue = taskQueues.computeIfAbsent(logicEnvironmentId, k -> new LinkedBlockingQueue<>());
                
                if (queue != null && !queue.isEmpty()) {
                    log.info("UE可用，检查排队任务 - 逻辑环境ID: {}, 队列大小: {}", logicEnvironmentId, queue.size());
                    
                    // 检查UE是否可用，如果可用则恢复逻辑环境状态
                    List<TestCaseExecutionRequest.UeInfo> ueList = getLogicEnvironmentUeList(logicEnvironmentId);
                    if (checkUeAvailabilityForQueue(ueList, logicEnvironmentId)) {
                        // UE可用，恢复逻辑环境状态
                        LogicEnvironment logicEnvironment = logicEnvironmentService.getById(logicEnvironmentId);
                        if (logicEnvironment != null && logicEnvironment.getStatus() != null && logicEnvironment.getStatus() == 0) {
                            logicEnvironment.setStatus(1); // 1: 可用
                            logicEnvironmentService.updateById(logicEnvironment);
                            log.info("逻辑环境已恢复为可用 - 逻辑环境ID: {}", logicEnvironmentId);
                        }
                    }
                    
                    startQueueProcessor(logicEnvironmentId);
                } else {
                    log.debug("逻辑环境队列为空或不存在 - 逻辑环境ID: {}", logicEnvironmentId);
                }
            }
            
        } catch (Exception e) {
            log.error("处理UE可用后的队列任务失败 - UE IDs: {}, 错误: {}", ueIds, e.getMessage(), e);
        }
    }
    
    /**
     * 初始化定时任务，在服务启动时调用
     * 每2分钟查询一次等待中的任务，检查UE状态，如果空闲则下发
     */
    @PostConstruct
    public void initScheduledTask() {
        log.info("初始化定时任务 - 每{}分钟查询一次等待中的任务", SCHEDULED_TASK_INTERVAL_MINUTES);
        
        scheduledTaskExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "TaskQueueProcessor");
            thread.setDaemon(true);
            return thread;
        });
        
        // 启动定时任务，每2分钟执行一次
        scheduledTaskExecutor.scheduleAtFixedRate(
            this::processWaitingTasks,
            0, // 初始延迟0秒，立即执行一次
            SCHEDULED_TASK_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
        
        log.info("定时任务已启动 - 每{}分钟执行一次", SCHEDULED_TASK_INTERVAL_MINUTES);
    }
    
    /**
     * 销毁定时任务，在服务关闭时调用
     */
    @PreDestroy
    public void destroyScheduledTask() {
        if (scheduledTaskExecutor != null && !scheduledTaskExecutor.isShutdown()) {
            log.info("正在关闭定时任务执行器...");
            scheduledTaskExecutor.shutdown();
            try {
                if (!scheduledTaskExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduledTaskExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledTaskExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("定时任务执行器已关闭");
        }
    }
    
    /**
     * 处理等待中的任务
     * 遍历所有UE队列，检查UE状态，如果空闲则下发任务
     */
    private void processWaitingTasks() {
        try {
            log.debug("开始处理等待中的任务...");
            
            int totalProcessed = 0;
            int totalQueued = 0;
            
            // 遍历所有UE队列
            for (java.util.Map.Entry<Integer, BlockingQueue<QueuedTask>> entry : ueTaskQueues.entrySet()) {
                BlockingQueue<QueuedTask> queue = entry.getValue();
                
                if (queue == null || queue.isEmpty()) {
                    continue;
                }
                
                totalQueued += queue.size();
                
                // 获取该UE相关的所有逻辑环境ID（从队列中的任务获取）
                java.util.Set<Long> logicEnvironmentIds = new java.util.HashSet<>();
                for (QueuedTask queuedTask : queue) {
                    if (queuedTask != null && queuedTask.getLogicEnvironmentId() != null) {
                        logicEnvironmentIds.add(queuedTask.getLogicEnvironmentId());
                    }
                }
                
                // 处理每个逻辑环境的任务
                for (Long logicEnvironmentId : logicEnvironmentIds) {
                    try {
                        // 获取该逻辑环境的所有UE
                        List<TestCaseExecutionRequest.UeInfo> ueList = getLogicEnvironmentUeList(logicEnvironmentId);
                        List<Integer> ueIds = extractUeIds(ueList);
                        
                        if (ueIds.isEmpty()) {
                            log.warn("逻辑环境没有UE，跳过 - 逻辑环境ID: {}", logicEnvironmentId);
                            continue;
                        }
                        
                        // 检查UE是否可用
                        List<Integer> unavailableUeIds = ueService.checkUesAvailability(ueIds);
                        
                        if (unavailableUeIds.isEmpty()) {
                            // UE都空闲，尝试处理队列中的任务
                            // 查找队列中属于该逻辑环境的任务
                            QueuedTask queuedTask = null;
                            for (QueuedTask task : queue) {
                                if (task != null && logicEnvironmentId.equals(task.getLogicEnvironmentId())) {
                                    // 检查任务状态
                                    if (checkTaskStatus(task.getInstances())) {
                                        queuedTask = task;
                                        break;
                                    } else {
                                        // 任务已停止，从队列中移除
                                        queue.remove(task);
                                        log.info("任务已停止，从队列中移除 - 逻辑环境ID: {}, 任务ID: {}", 
                                                logicEnvironmentId, task.getTaskId());
                                    }
                                }
                            }
                            
                            if (queuedTask != null) {
                                // 尝试获取UE锁并下发任务
                                String taskId = queuedTask.getTaskId();
                                if (tryAcquireUeLocks(ueIds, logicEnvironmentId, queuedTask.getInstances(), taskId)) {
                                    // 成功获取锁，从队列中移除任务
                                    queue.remove(queuedTask);
                                    
                                    // 记录任务ID和UE ID列表的映射
                                    taskIdToUeIdsMap.put(taskId, new ArrayList<>(ueIds));
                                    
                                    // 更新任务状态为运行中
                                    if (!queuedTask.getInstances().isEmpty()) {
                                        Long collectTaskId = queuedTask.getInstances().get(0).getCollectTaskId();
                                        if (collectTaskId != null) {
                                            collectTaskService.updateTaskStatus(collectTaskId, "RUNNING");
                                        }
                                    }
                                    
                                    // 创建执行任务
                                    boolean success = createExecutionTaskForLogicEnvironment(logicEnvironmentId, queuedTask.getInstances());
                                    
                                    if (success) {
                                        totalProcessed++;
                                        log.info("定时任务成功下发任务 - 逻辑环境ID: {}, UE IDs: {}, 任务ID: {}", 
                                                logicEnvironmentId, ueIds, taskId);
                                    } else {
                                        // 创建失败，释放锁并将任务重新加入队列
                                        log.warn("创建执行任务失败，释放UE锁并重新加入队列 - 逻辑环境ID: {}, UE IDs: {}, 任务ID: {}", 
                                                logicEnvironmentId, ueIds, taskId);
                                        releaseUeLocksInternal(ueIds);
                                        taskIdToUeIdsMap.remove(taskId);
                                        queue.offer(queuedTask);
                                    }
                                } else {
                                    log.debug("无法获取UE锁，任务继续等待 - 逻辑环境ID: {}, UE IDs: {}", 
                                            logicEnvironmentId, ueIds);
                                }
                            }
                        } else {
                            log.debug("UE使用中，任务继续等待 - 逻辑环境ID: {}, 不可用UE IDs: {}", 
                                    logicEnvironmentId, unavailableUeIds);
                        }
                    } catch (Exception e) {
                        log.error("处理逻辑环境任务失败 - 逻辑环境ID: {}, 错误: {}", logicEnvironmentId, e.getMessage(), e);
                    }
                }
            }
            
            if (totalQueued > 0) {
                log.info("定时任务处理完成 - 队列中任务数: {}, 成功下发: {}", totalQueued, totalProcessed);
            } else {
                log.debug("定时任务处理完成 - 没有等待中的任务");
            }
            
        } catch (Exception e) {
            log.error("处理等待中的任务异常 - 错误: {}", e.getMessage(), e);
        }
    }
}
