package com.datacollect.service;

import com.datacollect.dto.CollectTaskRequest;
import com.datacollect.entity.CollectTask;
import com.datacollect.entity.TestCaseExecutionInstance;

import java.util.List;

/**
 * 采集任务处理服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface CollectTaskProcessService {
    
    /**
     * 处理采集任务创建
     * 
     * @param request 采集任务请求
     * @return 采集任务ID
     */
    Long processCollectTaskCreation(CollectTaskRequest request);
    
    /**
     * 组装用例执行例次列表
     * 
     * @param collectTaskId 采集任务ID
     * @param testCaseIds 测试用例ID列表
     * @param collectCount 采集次数
     * @return 用例执行例次列表
     */
    List<TestCaseExecutionInstance> assembleTestCaseInstances(Long collectTaskId, List<Long> testCaseIds, Integer collectCount);
    
    /**
     * 分配用例执行例次到逻辑环境
     * 
     * @param instances 用例执行例次列表
     * @param logicEnvironmentIds 逻辑环境ID列表
     * @return 分配后的用例执行例次列表
     */
    List<TestCaseExecutionInstance> distributeInstancesToEnvironments(List<TestCaseExecutionInstance> instances, List<Long> logicEnvironmentIds);
    
    /**
     * 调用执行机服务
     * 
     * @param instances 用例执行例次列表
     * @return 是否调用成功
     */
    boolean callExecutorServices(List<TestCaseExecutionInstance> instances);
}
