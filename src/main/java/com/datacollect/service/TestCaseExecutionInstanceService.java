package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.TestCaseExecutionInstance;

import java.util.List;

/**
 * 用例执行例次服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface TestCaseExecutionInstanceService extends IService<TestCaseExecutionInstance> {
    
    /**
     * 批量保存用例执行例次
     * 
     * @param instances 用例执行例次列表
     * @return 是否保存成功
     */
    boolean batchSaveInstances(List<TestCaseExecutionInstance> instances);
    
    /**
     * 根据采集任务ID查询用例执行例次
     * 
     * @param collectTaskId 采集任务ID
     * @return 用例执行例次列表
     */
    List<TestCaseExecutionInstance> getByCollectTaskId(Long collectTaskId);
    
    /**
     * 更新执行状态
     * 
     * @param id 例次ID
     * @param status 状态
     * @param executionTaskId 执行任务ID
     * @return 是否更新成功
     */
    boolean updateExecutionStatus(Long id, String status, String executionTaskId);
    
    /**
     * 根据用例ID和轮次更新执行状态
     * 
     * @param collectTaskId 采集任务ID
     * @param testCaseId 用例ID
     * @param round 轮次
     * @param status 状态
     * @return 是否更新成功
     */
    boolean updateExecutionStatusByTestCaseAndRound(Long collectTaskId, Long testCaseId, Integer round, String status);
    
    /**
     * 根据用例ID和轮次更新执行状态和结果
     * 
     * @param collectTaskId 采集任务ID
     * @param testCaseId 用例ID
     * @param round 轮次
     * @param status 执行状态
     * @param result 执行结果
     * @return 是否更新成功
     */
    boolean updateExecutionStatusAndResultByTestCaseAndRound(Long collectTaskId, Long testCaseId, Integer round, String status, String result);
    
    /**
     * 根据用例ID和轮次更新执行状态、结果和失败原因
     * 
     * @param collectTaskId 采集任务ID
     * @param testCaseId 用例ID
     * @param round 轮次
     * @param status 执行状态
     * @param result 执行结果
     * @param failureReason 失败原因
     * @return 是否更新成功
     */
    boolean updateExecutionStatusAndResultAndFailureReasonByTestCaseAndRound(Long collectTaskId, Long testCaseId, Integer round, String status, String result, String failureReason);
}
