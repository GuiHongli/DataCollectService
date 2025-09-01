package com.datacollect.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.common.exception.CollectTaskException;
import com.datacollect.dto.CollectTaskRequest;
import com.datacollect.entity.CollectStrategy;
import com.datacollect.entity.CollectTask;
import com.datacollect.mapper.CollectTaskMapper;
import com.datacollect.service.CollectStrategyService;
import com.datacollect.service.CollectTaskService;

import lombok.extern.slf4j.Slf4j;

/**
 * 采集任务服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class CollectTaskServiceImpl extends ServiceImpl<CollectTaskMapper, CollectTask> implements CollectTaskService {

    @Autowired
    private CollectStrategyService collectStrategyService;

    @Override
    public Long createCollectTask(CollectTaskRequest request) {
        log.info("Create collect task - task name: {}, collect strategy ID: {}, collect count: {}", 
                request.getName(), request.getCollectStrategyId(), request.getCollectCount());
        
        // 获取采集策略信息
        CollectStrategy strategy = collectStrategyService.getById(request.getCollectStrategyId());
        if (strategy == null) {
            log.error("Collect strategy not found - strategy ID: {}", request.getCollectStrategyId());
            throw new CollectTaskException("COLLECT_STRATEGY_NOT_FOUND", "采集策略不存在");
        }
        
        CollectTask collectTask = new CollectTask();
        collectTask.setName(request.getName());
        collectTask.setDescription(request.getDescription());
        collectTask.setCollectStrategyId(request.getCollectStrategyId());
        collectTask.setCollectStrategyName(strategy.getName());
        collectTask.setTestCaseSetId(strategy.getTestCaseSetId());
        collectTask.setCollectCount(request.getCollectCount());
        collectTask.setRegionId(request.getRegionId());
        collectTask.setCountryId(request.getCountryId());
        collectTask.setProvinceId(request.getProvinceId());
        collectTask.setCityId(request.getCityId());
        collectTask.setStatus("RUNNING");
        collectTask.setTotalTestCaseCount(0);
        collectTask.setCompletedTestCaseCount(0);
        collectTask.setSuccessTestCaseCount(0);
        collectTask.setFailedTestCaseCount(0);
        
        // 设置自定义参数
        collectTask.setCustomParams(request.getCustomParams());
        
        LocalDateTime now = LocalDateTime.now();
        collectTask.setCreateTime(now);
        collectTask.setUpdateTime(now);
        
        boolean success = save(collectTask);
        if (success) {
            log.info("Collect task created successfully - task ID: {}", collectTask.getId());
            return collectTask.getId();
        } else {
            log.error("Failed to create collect task - task name: {}", request.getName());
            throw new CollectTaskException("COLLECT_TASK_SAVE_FAILED", "采集任务创建失败");
        }
    }

    @Override
    public CollectTask getCollectTaskById(Long id) {
        log.debug("Get collect task by ID - task ID: {}", id);
        return getById(id);
    }

    @Override
    public boolean updateTaskStatus(Long id, String status) {
        log.info("Update task status - task ID: {}, status: {}", id, status);
        
        if (!isValidStatus(status)) {
            log.error("Invalid task status: {} - task ID: {}", status, id);
            return false;
        }
        
        UpdateWrapper<CollectTask> updateWrapper = createBaseUpdateWrapper(id);
        updateWrapper.set("status", status);
        setTimeFieldsByStatus(updateWrapper, status);
        
        return executeUpdate(updateWrapper, id, status);
    }

    private UpdateWrapper<CollectTask> createBaseUpdateWrapper(Long id) {
        UpdateWrapper<CollectTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id);
        updateWrapper.set("update_time", LocalDateTime.now());
        return updateWrapper;
    }

    private void setTimeFieldsByStatus(UpdateWrapper<CollectTask> updateWrapper, String status) {
        if ("RUNNING".equals(status)) {
            updateWrapper.set("start_time", LocalDateTime.now());
        } else if ("COMPLETED".equals(status) || "STOPPED".equals(status) || "PAUSED".equals(status)) {
            updateWrapper.set("end_time", LocalDateTime.now());
        }
    }

    private boolean executeUpdate(UpdateWrapper<CollectTask> updateWrapper, Long id, String status) {
        boolean success = update(updateWrapper);
        if (success) {
            log.info("Task status updated successfully - task ID: {}, status: {}", id, status);
        } else {
            log.error("Failed to update task status - task ID: {}, status: {}", id, status);
        }
        return success;
    }
    
    /**
     * 验证任务状态是否有效
     * 
     * @param status 状态值
     * @return 是否有效
     */
    private boolean isValidStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return false;
        }
        
        String[] validStatuses = {"PENDING", "RUNNING", "COMPLETED", "STOPPED", "PAUSED"};
        for (String validStatus : validStatuses) {
            if (validStatus.equals(status)) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean updateTaskProgress(Long id, Integer totalCount, Integer successCount, Integer failedCount) {
        log.debug("Update task progress - task ID: {}, total test case count: {}, success: {}, failed: {}", 
                id, totalCount, successCount, failedCount);
        
        UpdateWrapper<CollectTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id);
        updateWrapper.set("total_test_case_count", totalCount);
        updateWrapper.set("success_test_case_count", successCount);
        updateWrapper.set("failed_test_case_count", failedCount);
        updateWrapper.set("update_time", LocalDateTime.now());
        
        boolean success = update(updateWrapper);
        if (success) {
            log.debug("Task progress updated successfully - task ID: {}", id);
        } else {
            log.error("Failed to update task progress - task ID: {}", id);
        }
        
        return success;
    }
    
    @Override
    public boolean updateTaskFailureReason(Long id, String failureReason) {
        log.info("Update task failure reason - task ID: {}, failure reason: {}", id, failureReason);
        
        UpdateWrapper<CollectTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id);
        updateWrapper.set("failure_reason", failureReason);
        updateWrapper.set("update_time", LocalDateTime.now());
        
        boolean success = update(updateWrapper);
        if (success) {
            log.info("Task failure reason updated successfully - task ID: {}", id);
        } else {
            log.error("Failed to update task failure reason - task ID: {}", id);
        }
        
        return success;
    }
}
