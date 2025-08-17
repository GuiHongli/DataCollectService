package com.datacollect.service.impl;

import com.datacollect.dto.CollectTaskRequest;
import com.datacollect.dto.TestCaseExecutionRequest;
import com.datacollect.entity.CollectTask;
import com.datacollect.entity.TestCase;
import com.datacollect.entity.TestCaseSet;
import com.datacollect.entity.TestCaseExecutionInstance;
import com.datacollect.entity.dto.LogicEnvironmentDTO;
import com.datacollect.service.CollectTaskProcessService;
import com.datacollect.service.CollectTaskService;
import com.datacollect.service.TestCaseExecutionInstanceService;
import com.datacollect.service.TestCaseService;
import com.datacollect.service.TestCaseSetService;
import com.datacollect.service.LogicEnvironmentService;
import com.datacollect.service.ExecutorService;
import com.datacollect.util.HttpClientUtil;
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
    
    @Value("${datacollect.service.base-url:http://localhost:8080}")
    private String dataCollectServiceBaseUrl;

    @Override
    public Long processCollectTaskCreation(CollectTaskRequest request) {
        log.info("开始处理采集任务创建 - 任务名称: {}", request.getName());
        
        try {
            // 1. 记录采集任务本身的信息
            Long collectTaskId = collectTaskService.createCollectTask(request);
            log.info("采集任务创建成功 - 任务ID: {}", collectTaskId);
            
            // 2. 获取采集策略关联的测试用例
            CollectTask collectTask = collectTaskService.getCollectTaskById(collectTaskId);
            List<TestCase> testCases = testCaseService.getByTestCaseSetId(collectTask.getTestCaseSetId());
            List<Long> testCaseIds = testCases.stream().map(TestCase::getId).collect(java.util.stream.Collectors.toList());
            log.info("获取到测试用例数量: {} - 任务ID: {}", testCaseIds.size(), collectTaskId);
            
            // 3. 组装用例执行例次列表
            List<TestCaseExecutionInstance> instances = assembleTestCaseInstances(collectTaskId, testCaseIds, request.getCollectCount());
            log.info("组装用例执行例次完成 - 例次数量: {} - 任务ID: {}", instances.size(), collectTaskId);
            
            // 4. 分配用例执行例次到逻辑环境
            List<TestCaseExecutionInstance> distributedInstances = distributeInstancesToEnvironments(instances, request.getLogicEnvironmentIds());
            log.info("分配用例执行例次完成 - 任务ID: {}", collectTaskId);
            
            // 5. 保存用例执行例次
            boolean saveSuccess = testCaseExecutionInstanceService.batchSaveInstances(distributedInstances);
            if (!saveSuccess) {
                throw new RuntimeException("保存用例执行例次失败");
            }
            log.info("用例执行例次保存成功 - 任务ID: {}", collectTaskId);
            
            // 6. 更新任务总用例数
            collectTaskService.updateTaskProgress(collectTaskId, instances.size(), 0, 0);
            
            // 7. 异步调用执行机服务
            CompletableFuture.runAsync(() -> {
                try {
                    boolean callSuccess = callExecutorServices(distributedInstances);
                    if (callSuccess) {
                        collectTaskService.updateTaskStatus(collectTaskId, "RUNNING");
                        log.info("执行机服务调用成功 - 任务ID: {}", collectTaskId);
                    } else {
                        collectTaskService.updateTaskStatus(collectTaskId, "FAILED");
                        collectTaskService.updateTaskFailureReason(collectTaskId, "执行机服务调用失败");
                        log.error("执行机服务调用失败 - 任务ID: {}", collectTaskId);
                    }
                } catch (Exception e) {
                    collectTaskService.updateTaskStatus(collectTaskId, "FAILED");
                    collectTaskService.updateTaskFailureReason(collectTaskId, "执行机服务调用异常: " + e.getMessage());
                    log.error("执行机服务调用异常 - 任务ID: {}, 错误: {}", collectTaskId, e.getMessage(), e);
                }
            });
            
            return collectTaskId;
            
        } catch (Exception e) {
            log.error("处理采集任务创建失败 - 任务名称: {}, 错误: {}", request.getName(), e.getMessage(), e);
            throw new RuntimeException("处理采集任务创建失败: " + e.getMessage());
        }
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
                instance.setStatus("PENDING");
                instances.add(instance);
            }
        }
        
        log.info("用例执行例次组装完成 - 例次数量: {} - 任务ID: {}", instances.size(), collectTaskId);
        return instances;
    }

    @Override
    public List<TestCaseExecutionInstance> distributeInstancesToEnvironments(List<TestCaseExecutionInstance> instances, List<Long> logicEnvironmentIds) {
        log.info("开始分配用例执行例次到逻辑环境 - 例次数量: {}, 逻辑环境数量: {}", instances.size(), logicEnvironmentIds.size());
        
        if (logicEnvironmentIds.isEmpty()) {
            log.error("逻辑环境列表为空，无法分配用例执行例次");
            throw new RuntimeException("逻辑环境列表为空");
        }
        
        // 获取逻辑环境关联的执行机IP
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
        
        // 均分分配用例执行例次到逻辑环境
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
        
        log.info("用例执行例次均分分配完成 - 任务ID: {}", instances.get(0).getCollectTaskId());
        return instances;
    }

    @Override
    public boolean callExecutorServices(List<TestCaseExecutionInstance> instances) {
        log.info("开始调用执行机服务 - 例次数量: {}", instances.size());
        
        try {
            // 按执行机IP分组
            java.util.Map<String, List<TestCaseExecutionInstance>> instancesByExecutor = new java.util.HashMap<>();
            for (TestCaseExecutionInstance instance : instances) {
                instancesByExecutor.computeIfAbsent(instance.getExecutorIp(), k -> new ArrayList<>()).add(instance);
            }
            
            // 为每个执行机创建执行任务
            for (java.util.Map.Entry<String, List<TestCaseExecutionInstance>> entry : instancesByExecutor.entrySet()) {
                String executorIp = entry.getKey();
                List<TestCaseExecutionInstance> executorInstances = entry.getValue();
                
                boolean success = createExecutionTaskForExecutor(executorIp, executorInstances);
                if (!success) {
                    log.error("为执行机创建执行任务失败 - 执行机IP: {}", executorIp);
                    return false;
                }
            }
            
            log.info("执行机服务调用完成");
            return true;
            
        } catch (Exception e) {
            log.error("调用执行机服务异常 - 错误: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 为执行机创建执行任务
     */
    private boolean createExecutionTaskForExecutor(String executorIp, List<TestCaseExecutionInstance> instances) {
        try {
            log.info("为执行机创建执行任务 - 执行机IP: {}, 例次数量: {}", executorIp, instances.size());
            
            // 1. 构建请求参数
            String taskId = "TASK_" + System.currentTimeMillis() + "_" + executorIp.replace(".", "_");
            
            // 获取用例集信息
            Long testCaseSetId = null;
            String testCaseSetPath = null;
            if (!instances.isEmpty()) {
                // 从第一个实例获取采集任务ID，然后查询用例集信息
                Long collectTaskId = instances.get(0).getCollectTaskId();
                CollectTask collectTask = collectTaskService.getCollectTaskById(collectTaskId);
                if (collectTask != null) {
                    testCaseSetId = collectTask.getTestCaseSetId();
                    
                    // 获取用例集详细信息
                    TestCaseSet testCaseSet = testCaseSetService.getById(testCaseSetId);
                    if (testCaseSet != null) {
                        // 优先使用gohttpserver URL，如果没有则使用本地文件路径
                        testCaseSetPath = testCaseSet.getGohttpserverUrl();
                        if (testCaseSetPath == null || testCaseSetPath.trim().isEmpty()) {
                            testCaseSetPath = testCaseSet.getFilePath();
                        }
                        log.info("获取到用例集路径 - 用例集ID: {}, 路径: {}", testCaseSetId, testCaseSetPath);
                    } else {
                        log.warn("用例集不存在 - 用例集ID: {}", testCaseSetId);
                    }
                }
            }
            
            if (testCaseSetId == null) {
                log.error("无法获取用例集ID - 执行机IP: {}", executorIp);
                return false;
            }
            
            if (testCaseSetPath == null || testCaseSetPath.trim().isEmpty()) {
                log.error("无法获取用例集路径 - 用例集ID: {}, 执行机IP: {}", testCaseSetId, executorIp);
                return false;
            }
            
            // 构建用例列表
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
            
            // 2. 构建HTTP请求
            TestCaseExecutionRequest request = new TestCaseExecutionRequest();
            request.setTaskId(taskId);
            request.setExecutorIp(executorIp);
            request.setTestCaseSetId(testCaseSetId);
            request.setTestCaseSetPath(testCaseSetPath);
            request.setTestCaseList(testCaseList);
            request.setResultReportUrl(dataCollectServiceBaseUrl + "/api/test-result/report");
            request.setLogReportUrl(dataCollectServiceBaseUrl + "/api/test-log/report");
            
            // 3. 发送HTTP请求到执行机
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
                        
                        // 4. 更新例次状态
                        for (TestCaseExecutionInstance instance : instances) {
                            testCaseExecutionInstanceService.updateExecutionStatus(instance.getId(), "RUNNING", taskId);
                        }
                        
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
            
        } catch (Exception e) {
            log.error("为执行机创建执行任务异常 - 执行机IP: {}, 错误: {}", executorIp, e.getMessage(), e);
            return false;
        }
    }
}
