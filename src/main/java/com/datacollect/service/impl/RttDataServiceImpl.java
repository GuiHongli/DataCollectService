package com.datacollect.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.RttData;
import com.datacollect.mapper.RttDataMapper;
import com.datacollect.service.RttDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RTT数据服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class RttDataServiceImpl extends ServiceImpl<RttDataMapper, RttData> implements RttDataService {

    @Override
    public boolean batchSaveRttData(List<RttData> rttDataList, String taskId) {
        if (rttDataList == null || rttDataList.isEmpty()) {
            log.warn("RttData list is empty, cannot save");
            return false;
        }

        if (taskId == null || taskId.trim().isEmpty()) {
            log.warn("TaskId is null or empty, cannot save");
            return false;
        }

        try {
            // 设置任务ID和创建时间
            LocalDateTime now = LocalDateTime.now();
            for (RttData rttData : rttDataList) {
                rttData.setTaskId(taskId);
                rttData.setCreateTime(now);
                rttData.setUpdateTime(now);
            }

            // 批量保存
            boolean success = saveBatch(rttDataList);
            if (success) {
                log.info("RttData batch saved successfully - taskId: {}, count: {}", taskId, rttDataList.size());
            } else {
                log.error("Failed to batch save RttData - taskId: {}, count: {}", taskId, rttDataList.size());
            }
            return success;
        } catch (Exception e) {
            log.error("Error batch saving RttData - taskId: {}, error: {}", taskId, e.getMessage(), e);
            return false;
        }
    }
}

