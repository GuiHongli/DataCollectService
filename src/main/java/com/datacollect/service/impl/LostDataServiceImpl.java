package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.LostData;
import com.datacollect.mapper.LostDataMapper;
import com.datacollect.service.LostDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 丢包率数据服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class LostDataServiceImpl extends ServiceImpl<LostDataMapper, LostData> implements LostDataService {

    @Override
    public boolean batchSaveLostData(List<LostData> lostDataList, String taskId) {
        if (lostDataList == null || lostDataList.isEmpty()) {
            log.warn("LostData list is empty, cannot save");
            return false;
        }

        if (taskId == null || taskId.trim().isEmpty()) {
            log.warn("TaskId is null or empty, cannot save");
            return false;
        }

        try {
            // 设置任务ID和创建时间
            LocalDateTime now = LocalDateTime.now();
            for (LostData lostData : lostDataList) {
                lostData.setTaskId(taskId);
                lostData.setCreateTime(now);
                lostData.setUpdateTime(now);
            }

            // 批量保存
            boolean success = saveBatch(lostDataList);
            if (success) {
                log.info("LostData batch saved successfully - taskId: {}, count: {}", taskId, lostDataList.size());
            } else {
                log.error("Failed to batch save LostData - taskId: {}, count: {}", taskId, lostDataList.size());
            }
            return success;
        } catch (Exception e) {
            log.error("Error batch saving LostData - taskId: {}, error: {}", taskId, e.getMessage(), e);
            return false;
        }
    }
}




