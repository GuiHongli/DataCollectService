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
import com.datacollect.service.LogicEnvironmentUeService;
import com.datacollect.service.UeService;
import com.datacollect.service.NetworkTypeService;
import com.datacollect.entity.LogicEnvironmentUe;
import com.datacollect.entity.Ue;
import com.datacollect.entity.NetworkType;
import com.datacollect.entity.Executor;
import com.datacollect.entity.LogicEnvironment;
import com.datacollect.util.HttpClientUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.Set;
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
    private HttpClientUtil httpClientUtil;
    
    @Autowired
    private LogicEnvironmentUeService logicEnvironmentUeService;
    
    @Autowired
    private UeService ueService;
    
    @Autowired
    private NetworkTypeService networkTypeService;
    
    @Value("${datacollect.service.base-url:http://localhost:8080}")
    private String dataCollectServiceBaseUrl;

    @Override
    public Long processCollectTaskCreation(CollectTaskRequest request) {
        log.info("开始处理采集任务创建 - 任务名称: {}", request.getName());
        
        try {
            // 1. 记录采集任务本身的信息
            Long collectTaskId = collectTaskService.createCollectTask(request);
            log.info("采集任务创建成功 - 任务ID: {}", collectTaskId);
            
            // 2. 获取采集策略关联的测试用例（基于筛选条件）
            CollectTask collectTask = collectTaskService.getCollectTaskById(collectTaskId);
            List<Long> testCaseIds = getFilteredTestCaseIds(collectTask, request);
            
            // 3. 组装用例执行例次列表
            List<TestCaseExecutionInstance> instances = assembleTestCaseInstances(collectTaskId, testCaseIds, request.getCollectCount());
            log.info("组装用例执行例次完成 - 例次数量: {} - 任务ID: {}", instances.size(), collectTaskId);
            
            // 4. 分配用例执行例次到逻辑环境
            List<TestCaseExecutionInstance> distributedInstances = distributeInstancesToEnvironments(instances, request.getLogicEnvironmentIds());
            log.info("分配用例执行例次完成 - 任务ID: {}", collectTaskId);
            
            // 5. 保存用例执行例次
            saveTestCaseInstances(distributedInstances, collectTaskId);
            
            // 6. 更新任务总用例数
            collectTaskService.updateTaskProgress(collectTaskId, instances.size(), 0, 0);
            
            // 7. 异步调用执行机服务
            callExecutorServicesAsync(distributedInstances, collectTaskId);
            
            return collectTaskId;
            
        } catch (Exception e) {
            log.error("处理采集任务创建失败 - 任务名称: {}, 错误: {}", request.getName(), e.getMessage(), e);
            throw new RuntimeException("处理采集任务创建失败: " + e.getMessage());
        }
    }

    /**
     * 获取筛选后的测试用例ID列表
     */
    private List<Long> getFilteredTestCaseIds(CollectTask collectTask, CollectTaskRequest request) {
        // 获取策略信息
        CollectStrategy strategy = collectStrategyService.getById(collectTask.getCollectStrategyId());
        if (strategy == null) {
            log.error("采集策略不存在 - 策略ID: {}", collectTask.getCollectStrategyId());
            throw new RuntimeException("采集策略不存在");
        }
        
        // 获取用例集中的所有用例
        List<TestCase> allTestCases = testCaseService.getByTestCaseSetId(collectTask.getTestCaseSetId());
        
        // 根据策略的筛选条件过滤用例
        List<TestCase> filteredTestCases = filterTestCasesByStrategy(allTestCases, strategy);
        
        List<Long> testCaseIds = filteredTestCases.stream()
            .map(TestCase::getId)
            .collect(java.util.stream.Collectors.toList());
            
        log.info("获取到筛选后的测试用例数量: {} (原始数量: {}) - 任务ID: {}, 筛选条件: 业务大类={}, APP={}", 
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
     * 保存测试用例执行实例
     */
    private void saveTestCaseInstances(List<TestCaseExecutionInstance> distributedInstances, Long collectTaskId) {
        boolean saveSuccess = testCaseExecutionInstanceService.batchSaveInstances(distributedInstances);
        if (!saveSuccess) {
            throw new RuntimeException("保存用例执行例次失败");
        }
        log.info("用例执行例次保存成功 - 任务ID: {}", collectTaskId);
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
                    log.error("执行机服务调用失败 - 任务ID: {}", collectTaskId);
                } else {
                    log.info("执行机服务调用成功 - 任务ID: {}", collectTaskId);
                }
            } catch (Exception e) {
                collectTaskService.updateTaskStatus(collectTaskId, "STOPPED");
                collectTaskService.updateTaskFailureReason(collectTaskId, "执行机服务调用异常: " + e.getMessage());
                log.error("执行机服务调用异常 - 任务ID: {}, 错误: {}", collectTaskId, e.getMessage(), e);
            }
        });
    }

    @Override
    public List<TestCaseExecutionInstance> assembleTestCaseInstances(Long collectTaskId, List<Long> testCaseIds, Integer collectCount) {
        log.info("开始组装用例执行例次 - 任务ID: {}, 用例数量: {}, 采集次数: {}", collectTaskId, testCaseIds.size(), collectCount);
        
        List<TestCaseExecutionInstance> instances = new ArrayList<>();
        
        for (Long testCaseId : testCaseIds) {
            for (int round = 1; round <= collectCount; round++) {
                TestCaseExecutionInstance instance = new TestCaseExecutionInstance();
                instance.setCollectTaskId(collectTaskId);
                instance.setTestCaseId(testCaseId);
                instance.setRound(round);
                instance.setStatus("RUNNING");
                instances.add(instance);
            }
        }
        
        log.info("用例执行例次组装完成 - 例次数量: {} - 任务ID: {}", instances.size(), collectTaskId);
        return instances;
    }

    @Override
    public List<TestCaseExecutionInstance> distributeInstancesToEnvironments(List<TestCaseExecutionInstance> instances, List<Long> logicEnvironmentIds) {
        log.info("开始分配用例执行例次到逻辑环境 - 例次数量: {}, 逻辑环境数量: {}", instances.size(), logicEnvironmentIds.size());
        
        validateLogicEnvironments(logicEnvironmentIds);
        
        // 获取逻辑环境关联的执行机IP
        Map<Long, String> environmentToExecutorMap = getEnvironmentToExecutorMap(logicEnvironmentIds);
        
        // 均分分配用例执行例次到逻辑环境
        distributeInstancesEvenly(instances, logicEnvironmentIds, environmentToExecutorMap);
        
        log.info("用例执行例次均分分配完成 - 任务ID: {}", instances.get(0).getCollectTaskId());
        return instances;
    }

    /**
     * 验证逻辑环境列表
     */
    private void validateLogicEnvironments(List<Long> logicEnvironmentIds) {
        if (logicEnvironmentIds.isEmpty()) {
            log.error("逻辑环境列表为空，无法分配用例执行例次");
            throw new RuntimeException("逻辑环境列表为空");
        }
    }

    /**
     * 获取环境到执行机的映射
     */
    private Map<Long, String> getEnvironmentToExecutorMap(List<Long> logicEnvironmentIds) {
        Map<Long, String> environmentToExecutorMap = new HashMap<>();
        for (Long logicEnvironmentId : logicEnvironmentIds) {
            LogicEnvironmentDTO logicEnvironmentDTO = logicEnvironmentService.getLogicEnvironmentDTO(logicEnvironmentId);
            if (logicEnvironmentDTO != null && logicEnvironmentDTO.getExecutorIpAddress() != null) {
                environmentToExecutorMap.put(logicEnvironmentId, logicEnvironmentDTO.getExecutorIpAddress());
            }
        }
        
        if (environmentToExecutorMap.isEmpty()) {
            log.error("未找到可用的执行机IP");
            throw new RuntimeException("未找到可用的执行机IP");
        }
        
        return environmentToExecutorMap;
    }

    /**
     * 均分分配实例到环境
     */
    private void distributeInstancesEvenly(List<TestCaseExecutionInstance> instances, List<Long> logicEnvironmentIds, Map<Long, String> environmentToExecutorMap) {
        int totalInstances = instances.size();
        int environmentCount = logicEnvironmentIds.size();
        int baseCount = totalInstances / environmentCount; // 每个环境的基础数量
        int remainder = totalInstances % environmentCount; // 剩余数量
        
        log.info("分配策略 - 总例次数: {}, 逻辑环境数: {}, 基础分配数: {}, 剩余数: {}", 
                totalInstances, environmentCount, baseCount, remainder);
        
        int instanceIndex = 0;
        for (int i = 0; i < logicEnvironmentIds.size(); i++) {
            Long logicEnvironmentId = logicEnvironmentIds.get(i);
            String executorIp = environmentToExecutorMap.get(logicEnvironmentId);
            
            // 计算当前环境应分配的例次数量
            int currentEnvironmentCount = baseCount + (i < remainder ? 1 : 0);
            
            log.info("逻辑环境 {} (执行机IP: {}) 分配 {} 个例次", logicEnvironmentId, executorIp, currentEnvironmentCount);
            
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
        log.info("开始调用执行机服务 - 例次数量: {}", instances.size());
        
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
            log.error("调用执行机服务异常 - 错误: {}", e.getMessage(), e);
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
                log.warn("任务已停止，不再继续下发用例执行 - 任务ID: {}, 状态: {}", collectTaskId, collectTask.getStatus());
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
                log.warn("任务已停止，跳过执行机 {} 的用例下发", executorIp);
                continue;
            }
            
            boolean success = createExecutionTaskForExecutor(executorIp, executorInstances);
            if (!success) {
                log.error("为执行机创建执行任务失败 - 执行机IP: {}", executorIp);
                return false;
            }
        }
        
        log.info("执行机服务调用完成");
        return true;
    }
    
    private boolean createExecutionTaskForExecutor(String executorIp, List<TestCaseExecutionInstance> instances) {
        try {
            log.info("为执行机创建执行任务 - 执行机IP: {}, 例次数量: {}", executorIp, instances.size());
            
            if (!checkTaskStatus(instances)) {
                log.warn("任务已停止，不再为执行机创建执行任务 - 执行机IP: {}", executorIp);
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
            log.error("为执行机创建执行任务异常 - 执行机IP: {}, 错误: {}", executorIp, e.getMessage(), e);
            return false;
        }
    }

    private String generateTaskId(String executorIp) {
        return "TASK_" + System.currentTimeMillis() + "_" + executorIp.replace(".", "_");
    }

    private void logExecutorInfo(String executorIp, List<TestCaseExecutionRequest.UeInfo> ueList, TestCaseExecutionRequest.CollectStrategyInfo collectStrategyInfo, Long collectStrategyId) {
        log.info("获取执行机关联的UE信息 - 执行机IP: {}, UE数量: {}", executorIp, ueList.size());
        log.info("获取采集策略信息 - 策略ID: {}, 策略名称: {}", collectStrategyId, 
                collectStrategyInfo != null ? collectStrategyInfo.getName() : "未知");
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
            log.warn("用例集不存在 - 用例集ID: {}", testCaseSetId);
            return;
        }
        
        String testCaseSetPath = determineTestCaseSetPath(testCaseSet);
        info.setTestCaseSetPath(testCaseSetPath);
        log.info("获取到用例集路径 - 用例集ID: {}, 路径: {}", testCaseSetId, testCaseSetPath);
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
            log.error("无法获取用例集ID - 执行机IP: {}", executorIp);
            return false;
        }
        
        if (testCaseSetInfo.getTestCaseSetPath() == null || testCaseSetInfo.getTestCaseSetPath().trim().isEmpty()) {
            log.error("无法获取用例集路径 - 用例集ID: {}, 执行机IP: {}", testCaseSetInfo.getTestCaseSetId(), executorIp);
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
                log.warn("用例不存在 - 用例ID: {}", instance.getTestCaseId());
            }
        }
        return testCaseList;
    }

    /**
     * 获取任务自定义参数
     */
    private String getTaskCustomParams(List<TestCaseExecutionInstance> instances) {
        String taskCustomParams = null;
        if (!instances.isEmpty()) {
            Long collectTaskId = instances.get(0).getCollectTaskId();
            CollectTask collectTask = collectTaskService.getCollectTaskById(collectTaskId);
            if (collectTask != null) {
                taskCustomParams = collectTask.getCustomParams();
                log.info("获取采集任务自定义参数 - 任务ID: {}, 自定义参数: {}", collectTaskId, taskCustomParams);
            }
        }
        return taskCustomParams;
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
            log.info("使用gohttpserver地址作为日志上报URL: {}", goHttpServerUrl);
        } else {
            request.setLogReportUrl(dataCollectServiceBaseUrl + "/api/test-result/log");
            log.info("使用默认日志上报URL: {}", dataCollectServiceBaseUrl + "/api/test-result/log");
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
        String caseExecuteServiceUrl = "http://" + executorIp + ":8081/api/test-case-execution/receive";
        log.info("调用CaseExecuteService - URL: {}, 任务ID: {}", caseExecuteServiceUrl, taskId);
        
        try {
            org.springframework.http.ResponseEntity<Map> response = 
                httpClientUtil.post(caseExecuteServiceUrl, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                Integer code = (Integer) result.get("code");
                
                if (code != null && code == 200) {
                    log.info("CaseExecuteService调用成功 - 任务ID: {}, 执行机IP: {}", taskId, executorIp);
                    
                    // 更新例次状态
                    updateInstanceStatus(instances, taskId);
                    
                    return true;
                } else {
                    String message = (String) result.get("message");
                    log.error("CaseExecuteService返回错误 - 任务ID: {}, 错误信息: {}", taskId, message);
                    return false;
                }
            } else {
                log.error("CaseExecuteService调用失败 - 任务ID: {}, HTTP状态: {}", taskId, response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            log.error("CaseExecuteService网络调用异常 - 任务ID: {}, 执行机IP: {}, 错误: {}", 
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
                log.warn("未找到执行机信息 - 执行机IP: {}", executorIp);
                return ueList;
            }
            
            // 2. 获取执行机关联的逻辑环境
            List<LogicEnvironment> logicEnvironments = getExecutorLogicEnvironments(executor);
            
            if (logicEnvironments.isEmpty()) {
                log.warn("执行机未关联逻辑环境 - 执行机IP: {}, 执行机ID: {}", executorIp, executor.getId());
                return ueList;
            }
            
            // 3. 获取所有逻辑环境关联的UE
            Set<Long> allUeIds = getAllUeIds(logicEnvironments);
            
            if (allUeIds.isEmpty()) {
                log.warn("执行机关联的逻辑环境未关联UE - 执行机IP: {}", executorIp);
                return ueList;
            }
            
            // 4. 获取UE详细信息并转换为DTO格式
            ueList = convertUesToDto(allUeIds);
            
            log.info("获取执行机关联的UE信息成功 - 执行机IP: {}, UE数量: {}", executorIp, ueList.size());
            
        } catch (Exception e) {
            log.error("获取执行机关联的UE信息失败 - 执行机IP: {}, 错误: {}", executorIp, e.getMessage(), e);
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
            ueInfo.setNetworkTypeName("未知网络类型");
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
            log.warn("采集策略ID为空");
            return null;
        }
        
        try {
            CollectStrategy collectStrategy = collectStrategyService.getById(collectStrategyId);
            if (collectStrategy == null) {
                log.warn("采集策略不存在 - 策略ID: {}", collectStrategyId);
                return null;
            }
            
            TestCaseExecutionRequest.CollectStrategyInfo strategyInfo = createCollectStrategyInfo(collectStrategy);
            log.info("获取采集策略信息成功 - 策略ID: {}, 策略名称: {}", collectStrategyId, collectStrategy.getName());
            return strategyInfo;
            
        } catch (Exception e) {
            log.error("获取采集策略信息失败 - 策略ID: {}, 错误: {}", collectStrategyId, e.getMessage(), e);
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
        strategyInfo.setIntent(collectStrategy.getIntent());
        strategyInfo.setCustomParams(collectStrategy.getCustomParams());
        strategyInfo.setDescription(collectStrategy.getDescription());
        strategyInfo.setStatus(collectStrategy.getStatus());
        return strategyInfo;
    }
}
