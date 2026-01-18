package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.VmosData;
import com.datacollect.mapper.VmosDataMapper;
import com.datacollect.service.VmosDataService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * vMOS数据服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Service
public class VmosDataServiceImpl extends ServiceImpl<VmosDataMapper, VmosData> implements VmosDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VmosDataServiceImpl.class);

    @Override
    public boolean batchSaveVmosData(List<VmosData> vmosDataList, String taskId) {
        if (vmosDataList == null || vmosDataList.isEmpty()) {
            LOGGER.warn("VmosData list is empty, cannot save");
            return false;
        }

        if (taskId == null || taskId.trim().isEmpty()) {
            LOGGER.warn("TaskId is null or empty, cannot save");
            return false;
        }

        try {
            // check该taskId是否已有数据
            QueryWrapper<VmosData> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("task_id", taskId);
            queryWrapper.last("LIMIT 1");
            VmosData existingData = getOne(queryWrapper);
            
            if (existingData != null) {
                LOGGER.info("VmosData already exists for taskId, skip saving - taskId: {}", taskId);
                return true;
            }

            // set任务ID和create时间
            LocalDateTime now = LocalDateTime.now();
            for (VmosData vmosData : vmosDataList) {
                vmosData.setTaskId(taskId);
                vmosData.setCreateTime(now);
                vmosData.setUpdateTime(now);
            }

            // 批量save
            boolean success = saveBatch(vmosDataList);
            if (success) {
                LOGGER.info("VmosData batch saved successfully - taskId: {}, count: {}", taskId, vmosDataList.size());
            } else {
                LOGGER.error("Failed to batch save VmosData - taskId: {}, count: {}", taskId, vmosDataList.size());
            }
            return success;
        } catch (Exception e) {
            LOGGER.error("Error batch saving VmosData - taskId: {}, error: {}", taskId, e.getMessage(), e);
            return false;
        }
    }
}








