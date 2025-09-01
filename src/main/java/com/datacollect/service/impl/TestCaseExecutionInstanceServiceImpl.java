package com.datacollect.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.TestCaseExecutionInstance;
import com.datacollect.mapper.TestCaseExecutionInstanceMapper;
import com.datacollect.service.TestCaseExecutionInstanceService;

import lombok.extern.slf4j.Slf4j;

/**
 * 用例执行例次服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class TestCaseExecutionInstanceServiceImpl extends ServiceImpl<TestCaseExecutionInstanceMapper, TestCaseExecutionInstance> implements TestCaseExecutionInstanceService {

    @Override
    public boolean batchSaveInstances(List<TestCaseExecutionInstance> instances) {
        log.info("Batch saving test case execution instances - Count: {}", instances.size());
        
        try {
            LocalDateTime now = LocalDateTime.now();
            for (TestCaseExecutionInstance instance : instances) {
                instance.setCreateTime(now);
                instance.setUpdateTime(now);
            }
            
            boolean success = saveBatch(instances);
            if (success) {
                log.info("Test case execution instances batch save successful - Count: {}", instances.size());
            } else {
                log.error("Test case execution instances batch save failed - Count: {}", instances.size());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("Exception occurred while batch saving test case execution instances - Count: {}, Error: {}", instances.size(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<TestCaseExecutionInstance> getByCollectTaskId(Long collectTaskId) {
        log.debug("Querying test case execution instances by collect task ID - Task ID: {}", collectTaskId);
        
        QueryWrapper<TestCaseExecutionInstance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("collect_task_id", collectTaskId);
        queryWrapper.orderByAsc("test_case_id", "round");
        
        List<TestCaseExecutionInstance> instances = list(queryWrapper);
        log.debug("Found {} test case execution instances - Task ID: {}", instances.size(), collectTaskId);
        
        return instances;
    }

    @Override
    public boolean updateExecutionStatus(Long id, String status, String executionTaskId) {
        log.info("Updating execution status - Instance ID: {}, Status: {}, Execution Task ID: {}", id, status, executionTaskId);
        
        UpdateWrapper<TestCaseExecutionInstance> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id);
        updateWrapper.set("status", status);
        updateWrapper.set("execution_task_id", executionTaskId);
        updateWrapper.set("update_time", LocalDateTime.now());
        
        boolean success = update(updateWrapper);
        if (success) {
            log.info("Execution status update successful - Instance ID: {}, Status: {}", id, status);
        } else {
            log.error("Execution status update failed - Instance ID: {}, Status: {}", id, status);
        }
        
        return success;
    }
    
    @Override
    public boolean updateExecutionStatusByTestCaseAndRound(Long collectTaskId, Long testCaseId, Integer round, String status) {
        log.info("Updating execution status by test case ID and round - Task ID: {}, Test Case ID: {}, Round: {}, Status: {}", 
                collectTaskId, testCaseId, round, status);
        
        UpdateWrapper<TestCaseExecutionInstance> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("collect_task_id", collectTaskId);
        updateWrapper.eq("test_case_id", testCaseId);
        updateWrapper.eq("round", round);
        updateWrapper.set("status", status);
        updateWrapper.set("update_time", LocalDateTime.now());
        
        boolean success = update(updateWrapper);
        if (success) {
            log.info("Execution status update successful - Task ID: {}, Test Case ID: {}, Round: {}, Status: {}", 
                    collectTaskId, testCaseId, round, status);
        } else {
            log.error("Execution status update failed - Task ID: {}, Test Case ID: {}, Round: {}, Status: {}", 
                    collectTaskId, testCaseId, round, status);
        }
        
        return success;
    }
    
    @Override
    public boolean updateExecutionStatusAndResultByTestCaseAndRound(Long collectTaskId, Long testCaseId, Integer round, String status, String result) {
        log.info("Updating execution status and result by test case ID and round - Task ID: {}, Test Case ID: {}, Round: {}, Status: {}, Result: {}", 
                collectTaskId, testCaseId, round, status, result);
        
        UpdateWrapper<TestCaseExecutionInstance> updateWrapper = createBaseUpdateWrapper(collectTaskId, testCaseId, round);
        updateWrapper.set("status", status);
        updateWrapper.set("result", result);
        
        return executeUpdate(updateWrapper, "Execution status and result", collectTaskId, testCaseId, round, status, result);
    }
    
    @Override
    public boolean updateExecutionStatusAndResultAndFailureReasonByTestCaseAndRound(Long collectTaskId, Long testCaseId, Integer round, String status, String result, String failureReason) {
        log.info("Updating execution status, result and failure reason by test case ID and round - Task ID: {}, Test Case ID: {}, Round: {}, Status: {}, Result: {}, Failure Reason: {}", 
                collectTaskId, testCaseId, round, status, result, failureReason);
        
        UpdateWrapper<TestCaseExecutionInstance> updateWrapper = createBaseUpdateWrapper(collectTaskId, testCaseId, round);
        updateWrapper.set("status", status);
        updateWrapper.set("result", result);
        updateWrapper.set("failure_reason", failureReason);
        
        return executeUpdate(updateWrapper, "Execution status, result and failure reason", collectTaskId, testCaseId, round, status, result, failureReason);
    }
    
    @Override
    public boolean updateExecutionStatusAndResultAndFailureReasonAndLogFilePathByTestCaseAndRound(Long collectTaskId, Long testCaseId, Integer round, String status, String result, String failureReason, String logFilePath) {
        log.info("Updating execution status, result, failure reason and log file path by test case ID and round - Task ID: {}, Test Case ID: {}, Round: {}, Status: {}, Result: {}, Failure Reason: {}, Log File: {}", 
                collectTaskId, testCaseId, round, status, result, failureReason, logFilePath);
        
        UpdateWrapper<TestCaseExecutionInstance> updateWrapper = createBaseUpdateWrapper(collectTaskId, testCaseId, round);
        updateWrapper.set("status", status);
        updateWrapper.set("result", result);
        updateWrapper.set("failure_reason", failureReason);
        updateWrapper.set("log_file_path", logFilePath);
        
        return executeUpdate(updateWrapper, "Execution status, result, failure reason and log file path", collectTaskId, testCaseId, round, status, result, failureReason, logFilePath);
    }
    
    @Override
    public List<TestCaseExecutionInstance> getByCollectTaskIdAndStatus(Long collectTaskId, String status) {
        log.debug("Querying test case execution instances by collect task ID and status - Task ID: {}, Status: {}", collectTaskId, status);
        
        QueryWrapper<TestCaseExecutionInstance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("collect_task_id", collectTaskId);
        queryWrapper.eq("status", status);
        queryWrapper.orderByAsc("test_case_id", "round");
        
        List<TestCaseExecutionInstance> instances = list(queryWrapper);
        log.debug("Found {} test case execution instances - Task ID: {}, Status: {}", instances.size(), collectTaskId, status);
        
        return instances;
    }

    private UpdateWrapper<TestCaseExecutionInstance> createBaseUpdateWrapper(Long collectTaskId, Long testCaseId, Integer round) {
        UpdateWrapper<TestCaseExecutionInstance> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("collect_task_id", collectTaskId);
        updateWrapper.eq("test_case_id", testCaseId);
        updateWrapper.eq("round", round);
        updateWrapper.set("update_time", LocalDateTime.now());
        return updateWrapper;
    }

    private boolean executeUpdate(UpdateWrapper<TestCaseExecutionInstance> updateWrapper, String operationType, Long collectTaskId, Long testCaseId, Integer round, Object... additionalParams) {
        boolean success = update(updateWrapper);
        if (success) {
            log.info("{} update successful - Task ID: {}, Test Case ID: {}, Round: {}", operationType, collectTaskId, testCaseId, round);
        } else {
            log.error("{} update failed - Task ID: {}, Test Case ID: {}, Round: {}", operationType, collectTaskId, testCaseId, round);
        }
        return success;
    }
}
