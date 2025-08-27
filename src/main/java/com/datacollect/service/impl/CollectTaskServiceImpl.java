package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.dto.CollectTaskRequest;
import com.datacollect.entity.CollectTask;
import com.datacollect.entity.CollectStrategy;
import com.datacollect.mapper.CollectTaskMapper;
import com.datacollect.service.CollectTaskService;
import com.datacollect.service.CollectStrategyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
        log.info("创建采集任务 - 任务名称: {}, 采集策略ID: {}, 采集次数: {}", 
                request.getName(), request.getCollectStrategyId(), request.getCollectCount());
        
        // 获取采集策略信息
        CollectStrategy strategy = collectStrategyService.getById(request.getCollectStrategyId());
        if (strategy == null) {
            log.error("采集策略不存在 - 策略ID: {}", request.getCollectStrategyId());
            throw new RuntimeException("采集策略不存在");
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
            log.info("采集任务创建成功 - 任务ID: {}", collectTask.getId());
            return collectTask.getId();
        } else {
            log.error("采集任务创建失败 - 任务名称: {}", request.getName());
            throw new RuntimeException("采集任务创建失败");
        }
    }

    @Override
    public CollectTask getCollectTaskById(Long id) {
        log.debug("根据ID获取采集任务 - 任务ID: {}", id);
        return getById(id);
    }

    @Override
    public boolean updateTaskStatus(Long id, String status) {
        log.info("更新任务状态 - 任务ID: {}, 状态: {}", id, status);
        
        // 验证状态值
        if (!isValidStatus(status)) {
            log.error("无效的任务状态: {} - 任务ID: {}", status, id);
            return false;
        }
        
        UpdateWrapper<CollectTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id);
        updateWrapper.set("status", status);
        updateWrapper.set("update_time", LocalDateTime.now());
        
        // 根据状态设置相应的时间字段
        if ("RUNNING".equals(status)) {
            updateWrapper.set("start_time", LocalDateTime.now());
        } else if ("COMPLETED".equals(status) || "STOPPED".equals(status) || "PAUSED".equals(status)) {
            updateWrapper.set("end_time", LocalDateTime.now());
        }
        
        boolean success = update(updateWrapper);
        if (success) {
            log.info("任务状态更新成功 - 任务ID: {}, 状态: {}", id, status);
        } else {
            log.error("任务状态更新失败 - 任务ID: {}, 状态: {}", id, status);
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
        log.debug("更新任务进度 - 任务ID: {}, 总用例数: {}, 成功: {}, 失败: {}", 
                id, totalCount, successCount, failedCount);
        
        UpdateWrapper<CollectTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id);
        updateWrapper.set("total_test_case_count", totalCount);
        updateWrapper.set("success_test_case_count", successCount);
        updateWrapper.set("failed_test_case_count", failedCount);
        updateWrapper.set("update_time", LocalDateTime.now());
        
        boolean success = update(updateWrapper);
        if (success) {
            log.debug("任务进度更新成功 - 任务ID: {}", id);
        } else {
            log.error("任务进度更新失败 - 任务ID: {}", id);
        }
        
        return success;
    }
    
    @Override
    public boolean updateTaskFailureReason(Long id, String failureReason) {
        log.info("更新任务失败原因 - 任务ID: {}, 失败原因: {}", id, failureReason);
        
        UpdateWrapper<CollectTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id);
        updateWrapper.set("failure_reason", failureReason);
        updateWrapper.set("update_time", LocalDateTime.now());
        
        boolean success = update(updateWrapper);
        if (success) {
            log.info("任务失败原因更新成功 - 任务ID: {}", id);
        } else {
            log.error("任务失败原因更新失败 - 任务ID: {}", id);
        }
        
        return success;
    }
}
