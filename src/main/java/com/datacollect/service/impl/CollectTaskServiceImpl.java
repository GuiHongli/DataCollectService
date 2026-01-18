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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 采集任务服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Service
public class CollectTaskServiceImpl extends ServiceImpl<CollectTaskMapper, CollectTask> implements CollectTaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectTaskServiceImpl.class);

    @Autowired
    private CollectStrategyService collectStrategyService;

    @Override
    public Long createCollectTask(CollectTaskRequest request, String createBy) {
        LOGGER.info("Create collect task - task name: {}, collect strategy ID: {}, collect count: {}, createBy: {}", 
                request.getName(), request.getCollectStrategyId(), request.getCollectCount(), createBy);
        
        // get采集策略信息
        CollectStrategy strategy = collectStrategyService.getById(request.getCollectStrategyId());
        if (strategy == null) {
            LOGGER.error("Collect strategy not found - strategy ID: {}", request.getCollectStrategyId());
            throw new CollectTaskException("COLLECT_STRATEGY_NOT_FOUND", "采集策略不存在");
        }
        
        CollectTask collectTask = new CollectTask();
        collectTask.setName(request.getName());
        collectTask.setDescription(request.getDescription());
        // 将网元ID列表转换为JSON字符串存储
        if (request.getNetworkElementIds() != null && !request.getNetworkElementIds().isEmpty()) {
            collectTask.setNetworkElementIds(com.alibaba.fastjson.JSON.toJSONString(request.getNetworkElementIds()));
        }
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
        
        // 设置任务级别自定义参数
        collectTask.setCustomParams(request.getTaskCustomParams());
        
        // 设置创建人（下发人）
        collectTask.setCreateBy(createBy);
        
        LocalDateTime now = LocalDateTime.now();
        collectTask.setCreateTime(now);
        collectTask.setUpdateTime(now);
        
        boolean success = save(collectTask);
        if (success) {
            LOGGER.info("Collect task created successfully - task ID: {}, createBy: {}", collectTask.getId(), createBy);
            return collectTask.getId();
        } else {
            LOGGER.error("Failed to create collect task - task name: {}", request.getName());
            throw new CollectTaskException("COLLECT_TASK_SAVE_FAILED", "采集任务创建失败");
        }
    }

    @Override
    public CollectTask getCollectTaskById(Long id) {
        LOGGER.debug("Get collect task by ID - task ID: {}", id);
        return getById(id);
    }

    @Override
    public boolean updateTaskStatus(Long id, String status) {
        LOGGER.info("Update task status - task ID: {}, status: {}", id, status);
        
        if (!isValidStatus(status)) {
            LOGGER.error("Invalid task status: {} - task ID: {}", status, id);
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
            LOGGER.info("Task status updated successfully - task ID: {}, status: {}", id, status);
        } else {
            LOGGER.error("Failed to update task status - task ID: {}, status: {}", id, status);
        }
        return success;
    }
    
    /**
     * validate任务状态是否有效
     * 
     * @param status 状态值
     * @return 是否有效
     */
    private boolean isValidStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return false;
        }
        
        String[] validStatuses = {"PENDING", "WAITING", "RUNNING", "COMPLETED", "STOPPED", "PAUSED"};
        for (String validStatus : validStatuses) {
            if (validStatus.equals(status)) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean updateTaskProgress(Long id, Integer totalCount, Integer successCount, Integer failedCount) {
        LOGGER.debug("Update task progress - task ID: {}, total test case count: {}, success: {}, failed: {}", 
                id, totalCount, successCount, failedCount);
        
        UpdateWrapper<CollectTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id);
        updateWrapper.set("total_test_case_count", totalCount);
        updateWrapper.set("success_test_case_count", successCount);
        updateWrapper.set("failed_test_case_count", failedCount);
        updateWrapper.set("update_time", LocalDateTime.now());
        
        boolean success = update(updateWrapper);
        if (success) {
            LOGGER.debug("Task progress updated successfully - task ID: {}", id);
        } else {
            LOGGER.error("Failed to update task progress - task ID: {}", id);
        }
        
        return success;
    }
    
    @Override
    public boolean updateTaskFailureReason(Long id, String failureReason) {
        LOGGER.info("Update task failure reason - task ID: {}, failure reason: {}", id, failureReason);
        
        UpdateWrapper<CollectTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id);
        updateWrapper.set("failure_reason", failureReason);
        updateWrapper.set("update_time", LocalDateTime.now());
        
        boolean success = update(updateWrapper);
        if (success) {
            LOGGER.info("Task failure reason updated successfully - task ID: {}", id);
        } else {
            LOGGER.error("Failed to update task failure reason - task ID: {}", id);
        }
        
        return success;
    }
}
