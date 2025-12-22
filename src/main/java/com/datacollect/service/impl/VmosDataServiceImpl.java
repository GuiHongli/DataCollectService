package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.VmosData;
import com.datacollect.mapper.VmosDataMapper;
import com.datacollect.service.VmosDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * vMOS数据服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class VmosDataServiceImpl extends ServiceImpl<VmosDataMapper, VmosData> implements VmosDataService {

    @Override
    public boolean batchSaveVmosData(List<VmosData> vmosDataList, String taskId) {
        if (vmosDataList == null || vmosDataList.isEmpty()) {
            log.warn("VmosData list is empty, cannot save");
            return false;
        }

        if (taskId == null || taskId.trim().isEmpty()) {
            log.warn("TaskId is null or empty, cannot save");
            return false;
        }

        try {
            // 检查该taskId是否已有数据
            QueryWrapper<VmosData> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("task_id", taskId);
            queryWrapper.last("LIMIT 1");
            VmosData existingData = getOne(queryWrapper);
            
            if (existingData != null) {
                log.info("VmosData already exists for taskId, skip saving - taskId: {}", taskId);
                return true;
            }

            // 设置任务ID和创建时间
            LocalDateTime now = LocalDateTime.now();
            for (VmosData vmosData : vmosDataList) {
                vmosData.setTaskId(taskId);
                vmosData.setCreateTime(now);
                vmosData.setUpdateTime(now);
            }

            // 批量保存
            boolean success = saveBatch(vmosDataList);
            if (success) {
                log.info("VmosData batch saved successfully - taskId: {}, count: {}", taskId, vmosDataList.size());
            } else {
                log.error("Failed to batch save VmosData - taskId: {}, count: {}", taskId, vmosDataList.size());
            }
            return success;
        } catch (Exception e) {
            log.error("Error batch saving VmosData - taskId: {}, error: {}", taskId, e.getMessage(), e);
            return false;
        }
    }
}








