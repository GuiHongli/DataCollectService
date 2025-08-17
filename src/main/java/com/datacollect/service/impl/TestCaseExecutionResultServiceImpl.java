package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.dto.TestCaseExecutionResult;
import com.datacollect.mapper.TestCaseExecutionResultMapper;
import com.datacollect.service.TestCaseExecutionResultService;
import com.datacollect.service.CollectTaskService;
import com.datacollect.service.TestCaseExecutionInstanceService;
import com.datacollect.entity.CollectTask;
import com.datacollect.entity.TestCaseExecutionInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用例执行结果服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class TestCaseExecutionResultServiceImpl extends ServiceImpl<TestCaseExecutionResultMapper, com.datacollect.entity.TestCaseExecutionResult> implements TestCaseExecutionResultService {

    @Autowired
    private CollectTaskService collectTaskService;
    
    @Autowired
    private TestCaseExecutionInstanceService testCaseExecutionInstanceService;

    @Override
    public boolean saveTestCaseExecutionResult(TestCaseExecutionResult result) {
        log.info("保存用例执行结果 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}", 
                result.getTaskId(), result.getTestCaseId(), result.getRound(), result.getStatus());
        
        try {
            com.datacollect.entity.TestCaseExecutionResult entity = new com.datacollect.entity.TestCaseExecutionResult();
            BeanUtils.copyProperties(result, entity);
            
            // 设置创建和更新时间
            LocalDateTime now = LocalDateTime.now();
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            
            boolean success = save(entity);
            if (success) {
                log.info("用例执行结果保存成功 - 任务ID: {}, 用例ID: {}, 轮次: {}", 
                        result.getTaskId(), result.getTestCaseId(), result.getRound());
                
                // 更新例次状态
                updateExecutionInstanceStatus(result);
                
                // 检查任务是否完成
                checkAndUpdateTaskCompletion(result.getTaskId());
            } else {
                log.error("用例执行结果保存失败 - 任务ID: {}, 用例ID: {}, 轮次: {}", 
                        result.getTaskId(), result.getTestCaseId(), result.getRound());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("保存用例执行结果异常 - 任务ID: {}, 用例ID: {}, 轮次: {}, 错误: {}", 
                    result.getTaskId(), result.getTestCaseId(), result.getRound(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<com.datacollect.entity.TestCaseExecutionResult> getByTaskId(String taskId) {
        log.debug("根据任务ID查询执行结果 - 任务ID: {}", taskId);
        
        QueryWrapper<com.datacollect.entity.TestCaseExecutionResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        queryWrapper.orderByDesc("create_time");
        
        List<com.datacollect.entity.TestCaseExecutionResult> results = list(queryWrapper);
        log.debug("查询到执行结果数量: {}", results.size());
        
        return results;
    }

    @Override
    public List<com.datacollect.entity.TestCaseExecutionResult> getByTestCaseId(Long testCaseId) {
        log.debug("根据用例ID查询执行结果 - 用例ID: {}", testCaseId);
        
        QueryWrapper<com.datacollect.entity.TestCaseExecutionResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("test_case_id", testCaseId);
        queryWrapper.orderByDesc("create_time");
        
        List<com.datacollect.entity.TestCaseExecutionResult> results = list(queryWrapper);
        log.debug("查询到执行结果数量: {}", results.size());
        
        return results;
    }
    
    /**
     * 更新例次状态和结果
     * 
     * @param result 用例执行结果
     */
    private void updateExecutionInstanceStatus(TestCaseExecutionResult result) {
        try {
            // 根据taskId查找对应的采集任务ID
            Long collectTaskId = getCollectTaskIdFromTaskId(result.getTaskId());
            if (collectTaskId == null) {
                log.warn("无法找到对应的采集任务ID: {}", result.getTaskId());
                return;
            }
            
            // 根据执行结果状态确定例次状态和结果
            String instanceStatus = "COMPLETED";  // 执行状态：已完成
            String instanceResult = result.getStatus();  // 执行结果：SUCCESS/FAILED/BLOCKED
            
            // 获取失败原因
            String failureReason = null;
            if ("FAILED".equals(result.getStatus()) || "BLOCKED".equals(result.getStatus())) {
                failureReason = result.getErrorMessage() != null ? result.getErrorMessage() : result.getResult();
            }
            
            // 更新例次状态、结果和失败原因
            boolean success = testCaseExecutionInstanceService.updateExecutionStatusAndResultAndFailureReasonByTestCaseAndRound(
                    collectTaskId, result.getTestCaseId(), result.getRound(), instanceStatus, instanceResult, failureReason);
            
            if (success) {
                log.debug("例次状态和结果更新成功 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}, 结果: {}", 
                        result.getTaskId(), result.getTestCaseId(), result.getRound(), instanceStatus, instanceResult);
            } else {
                log.warn("例次状态和结果更新失败 - 任务ID: {}, 用例ID: {}, 轮次: {}", 
                        result.getTaskId(), result.getTestCaseId(), result.getRound());
            }
            
        } catch (Exception e) {
            log.error("更新例次状态和结果异常 - 任务ID: {}, 用例ID: {}, 轮次: {}, 错误: {}", 
                    result.getTaskId(), result.getTestCaseId(), result.getRound(), e.getMessage(), e);
        }
    }
    
    /**
     * 根据taskId获取采集任务ID
     * 
     * @param taskId 任务ID (可能是数字格式或execution_task_id格式)
     * @return 采集任务ID
     */
    private Long getCollectTaskIdFromTaskId(String taskId) {
        try {
            // 1. 尝试直接解析为数字
            return Long.parseLong(taskId);
        } catch (NumberFormatException e) {
            // 2. 如果不是数字，则通过execution_task_id查找对应的collect_task_id
            log.debug("taskId不是数字格式，尝试通过execution_task_id查找: {}", taskId);
            
            try {
                // 查询test_case_execution_instance表，根据execution_task_id找到collect_task_id
                QueryWrapper<TestCaseExecutionInstance> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("execution_task_id", taskId);
                queryWrapper.select("collect_task_id");
                queryWrapper.last("LIMIT 1");
                
                TestCaseExecutionInstance instance = testCaseExecutionInstanceService.getOne(queryWrapper);
                if (instance != null) {
                    log.debug("通过execution_task_id找到collect_task_id: {} -> {}", taskId, instance.getCollectTaskId());
                    return instance.getCollectTaskId();
                } else {
                    log.warn("未找到execution_task_id对应的collect_task_id: {}", taskId);
                    return null;
                }
            } catch (Exception ex) {
                log.error("通过execution_task_id查找collect_task_id异常: {}", ex.getMessage(), ex);
                return null;
            }
        }
    }
    
    /**
     * 检查并更新任务完成状态
     * 
     * @param taskId 任务ID
     */
    private void checkAndUpdateTaskCompletion(String taskId) {
        try {
            log.debug("检查任务完成状态 - 任务ID: {}", taskId);
            
            // 1. 根据taskId查找对应的采集任务ID
            Long collectTaskId = getCollectTaskIdFromTaskId(taskId);
            if (collectTaskId == null) {
                log.warn("无法找到对应的采集任务ID: {}", taskId);
                return;
            }
            
            // 2. 查询该任务的所有用例执行例次
            List<TestCaseExecutionInstance> instances = testCaseExecutionInstanceService.getByCollectTaskId(collectTaskId);
            if (instances.isEmpty()) {
                log.warn("未找到任务相关的用例执行例次 - 任务ID: {}", taskId);
                return;
            }
            
            // 3. 统计各种状态和结果的数量
            int totalCount = instances.size();
            int completedCount = 0;
            int successCount = 0;
            int failedCount = 0;
            int blockedCount = 0;
            
            for (TestCaseExecutionInstance instance : instances) {
                String status = instance.getStatus();
                String result = instance.getResult();
                
                // 检查执行状态是否完成
                if ("COMPLETED".equals(status)) {
                    completedCount++;
                    
                    // 根据执行结果统计
                    if ("SUCCESS".equals(result)) {
                        successCount++;
                    } else if ("FAILED".equals(result)) {
                        failedCount++;
                    } else if ("BLOCKED".equals(result)) {
                        blockedCount++;
                    }
                }
            }
            
            log.debug("任务状态统计 - 任务ID: {}, 总数: {}, 已完成: {}, 成功: {}, 失败: {}, 阻塞: {}", 
                    taskId, totalCount, completedCount, successCount, failedCount, blockedCount);
            
            // 4. 检查是否所有用例例次都已完成
            if (completedCount == totalCount) {
                log.info("所有用例例次已完成，更新任务状态为完成 - 任务ID: {}", taskId);
                
                // 5. 更新任务状态和进度
                String finalStatus = "COMPLETED";
                if (failedCount > 0 || blockedCount > 0) {
                    finalStatus = "FAILED";
                }
                
                boolean statusUpdated = collectTaskService.updateTaskStatus(collectTaskId, finalStatus);
                boolean progressUpdated = collectTaskService.updateTaskProgress(collectTaskId, totalCount, successCount, failedCount);
                
                if (statusUpdated && progressUpdated) {
                    log.info("任务状态更新成功 - 任务ID: {}, 最终状态: {}, 总数: {}, 成功: {}, 失败: {}", 
                            taskId, finalStatus, totalCount, successCount, failedCount);
                } else {
                    log.error("任务状态更新失败 - 任务ID: {}", taskId);
                }
            } else {
                log.debug("任务尚未完成，继续等待 - 任务ID: {}, 已完成: {}/{}", taskId, completedCount, totalCount);
            }
            
        } catch (Exception e) {
            log.error("检查任务完成状态异常 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
        }
    }
}
