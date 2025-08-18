package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.TestCaseExecutionInstance;
import com.datacollect.mapper.TestCaseExecutionInstanceMapper;
import com.datacollect.service.TestCaseExecutionInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
        log.info("批量保存用例执行例次 - 数量: {}", instances.size());
        
        try {
            LocalDateTime now = LocalDateTime.now();
            for (TestCaseExecutionInstance instance : instances) {
                instance.setCreateTime(now);
                instance.setUpdateTime(now);
            }
            
            boolean success = saveBatch(instances);
            if (success) {
                log.info("用例执行例次批量保存成功 - 数量: {}", instances.size());
            } else {
                log.error("用例执行例次批量保存失败 - 数量: {}", instances.size());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("批量保存用例执行例次异常 - 数量: {}, 错误: {}", instances.size(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<TestCaseExecutionInstance> getByCollectTaskId(Long collectTaskId) {
        log.debug("根据采集任务ID查询用例执行例次 - 任务ID: {}", collectTaskId);
        
        QueryWrapper<TestCaseExecutionInstance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("collect_task_id", collectTaskId);
        queryWrapper.orderByAsc("test_case_id", "round");
        
        List<TestCaseExecutionInstance> instances = list(queryWrapper);
        log.debug("查询到用例执行例次数量: {} - 任务ID: {}", instances.size(), collectTaskId);
        
        return instances;
    }

    @Override
    public boolean updateExecutionStatus(Long id, String status, String executionTaskId) {
        log.info("更新执行状态 - 例次ID: {}, 状态: {}, 执行任务ID: {}", id, status, executionTaskId);
        
        UpdateWrapper<TestCaseExecutionInstance> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id);
        updateWrapper.set("status", status);
        updateWrapper.set("execution_task_id", executionTaskId);
        updateWrapper.set("update_time", LocalDateTime.now());
        
        boolean success = update(updateWrapper);
        if (success) {
            log.info("执行状态更新成功 - 例次ID: {}, 状态: {}", id, status);
        } else {
            log.error("执行状态更新失败 - 例次ID: {}, 状态: {}", id, status);
        }
        
        return success;
    }
    
    @Override
    public boolean updateExecutionStatusByTestCaseAndRound(Long collectTaskId, Long testCaseId, Integer round, String status) {
        log.info("根据用例ID和轮次更新执行状态 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}", 
                collectTaskId, testCaseId, round, status);
        
        UpdateWrapper<TestCaseExecutionInstance> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("collect_task_id", collectTaskId);
        updateWrapper.eq("test_case_id", testCaseId);
        updateWrapper.eq("round", round);
        updateWrapper.set("status", status);
        updateWrapper.set("update_time", LocalDateTime.now());
        
        boolean success = update(updateWrapper);
        if (success) {
            log.info("执行状态更新成功 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}", 
                    collectTaskId, testCaseId, round, status);
        } else {
            log.error("执行状态更新失败 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}", 
                    collectTaskId, testCaseId, round, status);
        }
        
        return success;
    }
    
    @Override
    public boolean updateExecutionStatusAndResultByTestCaseAndRound(Long collectTaskId, Long testCaseId, Integer round, String status, String result) {
        log.info("根据用例ID和轮次更新执行状态和结果 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}, 结果: {}", 
                collectTaskId, testCaseId, round, status, result);
        
        UpdateWrapper<TestCaseExecutionInstance> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("collect_task_id", collectTaskId);
        updateWrapper.eq("test_case_id", testCaseId);
        updateWrapper.eq("round", round);
        updateWrapper.set("status", status);
        updateWrapper.set("result", result);
        updateWrapper.set("update_time", LocalDateTime.now());
        
        boolean success = update(updateWrapper);
        if (success) {
            log.info("执行状态和结果更新成功 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}, 结果: {}", 
                    collectTaskId, testCaseId, round, status, result);
        } else {
            log.error("执行状态和结果更新失败 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}, 结果: {}", 
                    collectTaskId, testCaseId, round, status, result);
        }
        
        return success;
    }
    
    @Override
    public boolean updateExecutionStatusAndResultAndFailureReasonByTestCaseAndRound(Long collectTaskId, Long testCaseId, Integer round, String status, String result, String failureReason) {
        log.info("根据用例ID和轮次更新执行状态、结果和失败原因 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}, 结果: {}, 失败原因: {}", 
                collectTaskId, testCaseId, round, status, result, failureReason);
        
        UpdateWrapper<TestCaseExecutionInstance> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("collect_task_id", collectTaskId);
        updateWrapper.eq("test_case_id", testCaseId);
        updateWrapper.eq("round", round);
        updateWrapper.set("status", status);
        updateWrapper.set("result", result);
        updateWrapper.set("failure_reason", failureReason);
        updateWrapper.set("update_time", LocalDateTime.now());
        
        boolean success = update(updateWrapper);
        if (success) {
            log.info("执行状态、结果和失败原因更新成功 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}, 结果: {}, 失败原因: {}", 
                    collectTaskId, testCaseId, round, status, result, failureReason);
        } else {
            log.error("执行状态、结果和失败原因更新失败 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}, 结果: {}, 失败原因: {}", 
                    collectTaskId, testCaseId, round, status, result, failureReason);
        }
        
        return success;
    }
    
    @Override
    public boolean updateExecutionStatusAndResultAndFailureReasonAndLogFilePathByTestCaseAndRound(Long collectTaskId, Long testCaseId, Integer round, String status, String result, String failureReason, String logFilePath) {
        log.info("根据用例ID和轮次更新执行状态、结果、失败原因和日志文件路径 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}, 结果: {}, 失败原因: {}, 日志文件: {}", 
                collectTaskId, testCaseId, round, status, result, failureReason, logFilePath);
        
        UpdateWrapper<TestCaseExecutionInstance> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("collect_task_id", collectTaskId);
        updateWrapper.eq("test_case_id", testCaseId);
        updateWrapper.eq("round", round);
        updateWrapper.set("status", status);
        updateWrapper.set("result", result);
        updateWrapper.set("failure_reason", failureReason);
        updateWrapper.set("log_file_path", logFilePath);
        updateWrapper.set("update_time", LocalDateTime.now());
        
        boolean success = update(updateWrapper);
        if (success) {
            log.info("执行状态、结果、失败原因和日志文件路径更新成功 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}, 结果: {}, 失败原因: {}, 日志文件: {}", 
                    collectTaskId, testCaseId, round, status, result, failureReason, logFilePath);
        } else {
            log.error("执行状态、结果、失败原因和日志文件路径更新失败 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}, 结果: {}, 失败原因: {}, 日志文件: {}", 
                    collectTaskId, testCaseId, round, status, result, failureReason, logFilePath);
        }
        
        return success;
    }
    
    @Override
    public List<TestCaseExecutionInstance> getByCollectTaskIdAndStatus(Long collectTaskId, String status) {
        log.debug("根据采集任务ID和状态查询用例执行例次 - 任务ID: {}, 状态: {}", collectTaskId, status);
        
        QueryWrapper<TestCaseExecutionInstance> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("collect_task_id", collectTaskId);
        queryWrapper.eq("status", status);
        queryWrapper.orderByAsc("test_case_id", "round");
        
        List<TestCaseExecutionInstance> instances = list(queryWrapper);
        log.debug("查询到用例执行例次数量: {} - 任务ID: {}, 状态: {}", instances.size(), collectTaskId, status);
        
        return instances;
    }
}
