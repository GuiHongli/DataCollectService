package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.dto.CollectTaskRequest;
import com.datacollect.entity.CollectTask;

/**
 * 采集任务服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface CollectTaskService extends IService<CollectTask> {
    
    /**
     * 创建采集任务
     * 
     * @param request 采集任务请求
     * @return 采集任务ID
     */
    Long createCollectTask(CollectTaskRequest request);
    
    /**
     * 根据ID获取采集任务
     * 
     * @param id 任务ID
     * @return 采集任务
     */
    CollectTask getCollectTaskById(Long id);
    
    /**
     * 更新任务状态
     * 
     * @param id 任务ID
     * @param status 状态
     * @return 是否更新成功
     */
    boolean updateTaskStatus(Long id, String status);
    
    /**
     * 更新任务进度
     * 
     * @param id 任务ID
     * @param totalCount 总用例数
     * @param successCount 成功数量
     * @param failedCount 失败数量
     * @return 是否更新成功
     */
    boolean updateTaskProgress(Long id, Integer totalCount, Integer successCount, Integer failedCount);
    
    /**
     * 更新任务失败原因
     * 
     * @param id 任务ID
     * @param failureReason 失败原因
     * @return 是否更新成功
     */
    boolean updateTaskFailureReason(Long id, String failureReason);
}
