package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.dto.TaskInfoDTO;
import com.datacollect.entity.ClientTaskInfo;
import com.datacollect.mapper.TaskInfoMapper;
import com.datacollect.service.TaskInfoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 端侧任务信息服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class TaskInfoServiceImpl extends ServiceImpl<TaskInfoMapper, ClientTaskInfo> implements TaskInfoService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean saveTaskInfo(TaskInfoDTO taskInfoDTO) {
        if (taskInfoDTO == null) {
            log.warn("TaskInfoDTO is null, cannot save");
            return false;
        }

        try {
            // 检查taskId是否已存在
            QueryWrapper<ClientTaskInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("task_id", taskInfoDTO.getTaskId());
            ClientTaskInfo existingTaskInfo = getOne(queryWrapper);
            
            if (existingTaskInfo != null) {
                log.info("TaskInfo already exists, skip saving - taskId: {}", taskInfoDTO.getTaskId());
                return true;
            }

            ClientTaskInfo taskInfo = convertToEntity(taskInfoDTO);
            LocalDateTime now = LocalDateTime.now();
            taskInfo.setCreateTime(now);
            taskInfo.setUpdateTime(now);

            boolean success = save(taskInfo);
            if (success) {
                log.info("TaskInfo saved successfully - taskId: {}, id: {}", taskInfoDTO.getTaskId(), taskInfo.getId());
            } else {
                log.error("Failed to save TaskInfo - taskId: {}", taskInfoDTO.getTaskId());
            }
            return success;
        } catch (Exception e) {
            log.error("Error saving TaskInfo - taskId: {}, error: {}", taskInfoDTO.getTaskId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 将TaskInfoDTO转换为TaskInfo实体
     * 
     * @param dto TaskInfoDTO
     * @return TaskInfo实体
     */
    private ClientTaskInfo convertToEntity(TaskInfoDTO dto) {
        ClientTaskInfo taskInfo = new ClientTaskInfo();
        taskInfo.setTaskId(dto.getTaskId());
        taskInfo.setNation(dto.getNation());
        taskInfo.setOperator(dto.getOperator());
        taskInfo.setPrb(dto.getPrb());
        taskInfo.setRsrp(dto.getRsrp());
        taskInfo.setService(dto.getService());
        taskInfo.setApp(dto.getApp());
        taskInfo.setStartTime(dto.getStartTime());
        taskInfo.setEndTime(dto.getEndTime());
        taskInfo.setUserCategory(dto.getUserCategory());
        taskInfo.setDeviceId(dto.getDeviceId());

        // 将summary Map转换为JSON字符串
        if (dto.getSummary() != null && !dto.getSummary().isEmpty()) {
            try {
                String summaryJson = objectMapper.writeValueAsString(dto.getSummary());
                taskInfo.setSummary(summaryJson);
            } catch (JsonProcessingException e) {
                log.warn("Failed to convert summary to JSON - taskId: {}, error: {}", dto.getTaskId(), e.getMessage());
                taskInfo.setSummary(null);
            }
        }

        return taskInfo;
    }
}








