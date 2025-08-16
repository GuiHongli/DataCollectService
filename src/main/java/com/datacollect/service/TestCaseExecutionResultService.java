package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.dto.TestCaseExecutionResult;

/**
 * 用例执行结果服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface TestCaseExecutionResultService extends IService<com.datacollect.entity.TestCaseExecutionResult> {
    
    /**
     * 保存用例执行结果
     * 
     * @param result 用例执行结果
     * @return 是否保存成功
     */
    boolean saveTestCaseExecutionResult(TestCaseExecutionResult result);
    
    /**
     * 根据任务ID查询执行结果列表
     * 
     * @param taskId 任务ID
     * @return 执行结果列表
     */
    java.util.List<com.datacollect.entity.TestCaseExecutionResult> getByTaskId(String taskId);
    
    /**
     * 根据用例ID查询执行结果列表
     * 
     * @param testCaseId 用例ID
     * @return 执行结果列表
     */
    java.util.List<com.datacollect.entity.TestCaseExecutionResult> getByTestCaseId(Long testCaseId);
}
