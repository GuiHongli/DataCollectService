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
            com.datacollect.entity.TestCaseExecutionResult entity = createResultEntity(result);
            
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

    /**
     * 创建结果实体
     */
    private com.datacollect.entity.TestCaseExecutionResult createResultEntity(TestCaseExecutionResult result) {
        com.datacollect.entity.TestCaseExecutionResult entity = new com.datacollect.entity.TestCaseExecutionResult();
        BeanUtils.copyProperties(result, entity);
        
        // 设置创建和更新时间
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        
        return entity;
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
            InstanceStatusInfo statusInfo = determineInstanceStatus(result);
            
            // 更新例次状态、结果、失败原因和日志文件路径
            boolean success = testCaseExecutionInstanceService.updateExecutionStatusAndResultAndFailureReasonAndLogFilePathByTestCaseAndRound(
                    collectTaskId, result.getTestCaseId(), result.getRound(), statusInfo.getStatus(), statusInfo.getResult(), statusInfo.getFailureReason(), result.getLogFilePath());
            
            if (success) {
                log.debug("例次状态和结果更新成功 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}, 结果: {}", 
                        result.getTaskId(), result.getTestCaseId(), result.getRound(), statusInfo.getStatus(), statusInfo.getResult());
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
     * 实例状态信息
     */
    private static class InstanceStatusInfo {
        private String status;
        private String result;
        private String failureReason;
        
        public InstanceStatusInfo(String status, String result, String failureReason) {
            this.status = status;
            this.result = result;
            this.failureReason = failureReason;
        }
        
        public String getStatus() { return status; }
        public String getResult() { return result; }
        public String getFailureReason() { return failureReason; }
    }

    /**
     * 确定实例状态信息
     */
    private InstanceStatusInfo determineInstanceStatus(TestCaseExecutionResult result) {
        String instanceStatus = "COMPLETED";  // 执行状态：已完成
        String instanceResult = result.getStatus();  // 执行结果：SUCCESS/FAILED/BLOCKED
        
        // 获取失败原因
        String failureReason = null;
        if ("FAILED".equals(result.getStatus()) || "BLOCKED".equals(result.getStatus())) {
            failureReason = result.getFailureReason() != null ? result.getFailureReason() : result.getResult();
        }
        
        return new InstanceStatusInfo(instanceStatus, instanceResult, failureReason);
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
            return findCollectTaskIdByExecutionTaskId(taskId);
        }
    }

    /**
     * 通过execution_task_id查找collect_task_id
     */
    private Long findCollectTaskIdByExecutionTaskId(String taskId) {
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
            
            // 3. 检查是否所有例次都已完成
            boolean allCompleted = checkAllInstancesCompleted(instances);
            
            if (allCompleted) {
                // 4. 更新任务状态为已完成
                updateTaskToCompleted(collectTaskId);
                
                // 5. 更新任务执行进度
                updateTaskExecutionProgress(collectTaskId, instances);
            }
            
        } catch (Exception e) {
            log.error("检查任务完成状态异常 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 检查所有实例是否已完成
     */
    private boolean checkAllInstancesCompleted(List<TestCaseExecutionInstance> instances) {
        for (TestCaseExecutionInstance instance : instances) {
            if (!"COMPLETED".equals(instance.getStatus()) && !"STOPPED".equals(instance.getStatus())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 更新任务为已完成状态
     */
    private void updateTaskToCompleted(Long collectTaskId) {
        boolean success = collectTaskService.updateTaskStatus(collectTaskId, "COMPLETED");
        if (success) {
            log.info("任务状态更新为已完成 - 任务ID: {}", collectTaskId);
        } else {
            log.error("任务状态更新失败 - 任务ID: {}", collectTaskId);
        }
    }

    /**
     * 更新任务执行进度
     */
    private void updateTaskExecutionProgress(Long collectTaskId, List<TestCaseExecutionInstance> instances) {
        try {
            // 统计各种状态的例次数量
            TaskProgressInfo progressInfo = calculateTaskProgress(instances);
            
            // 更新任务进度
            boolean progressUpdated = collectTaskService.updateTaskProgress(collectTaskId, progressInfo.getTotalCount(), progressInfo.getSuccessCount(), progressInfo.getFailedCount());
            if (progressUpdated) {
                log.info("任务执行进度更新成功 - 任务ID: {}, 总数: {}, 成功: {}, 失败: {}", 
                        collectTaskId, progressInfo.getTotalCount(), progressInfo.getSuccessCount(), progressInfo.getFailedCount());
            } else {
                log.error("任务执行进度更新失败 - 任务ID: {}", collectTaskId);
            }
            
        } catch (Exception e) {
            log.error("更新任务执行进度异常 - 任务ID: {}, 错误: {}", collectTaskId, e.getMessage(), e);
        }
    }

    /**
     * 任务进度信息
     */
    private static class TaskProgressInfo {
        private int totalCount;
        private int successCount;
        private int failedCount;
        
        public TaskProgressInfo(int totalCount, int successCount, int failedCount) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
        }
        
        public int getTotalCount() { return totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailedCount() { return failedCount; }
    }

    /**
     * 计算任务进度
     */
    private TaskProgressInfo calculateTaskProgress(List<TestCaseExecutionInstance> instances) {
        int totalCount = instances.size();
        int successCount = 0;
        int failedCount = 0;
        
        for (TestCaseExecutionInstance instance : instances) {
            if ("COMPLETED".equals(instance.getStatus())) {
                if ("SUCCESS".equals(instance.getResult())) {
                    successCount++;
                } else if ("FAILED".equals(instance.getResult()) || "BLOCKED".equals(instance.getResult())) {
                    failedCount++;
                }
            }
        }
        
        return new TaskProgressInfo(totalCount, successCount, failedCount);
    }
}
