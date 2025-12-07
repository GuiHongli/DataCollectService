package com.datacollect.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.dto.TaskInfoDTO;
import com.datacollect.entity.ClientTaskInfo;
import com.datacollect.mapper.ClientTaskInfoMapper;
import com.datacollect.service.ClientTaskInfoService;
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
public class ClientTaskInfoServiceImpl extends ServiceImpl<ClientTaskInfoMapper, ClientTaskInfo> implements ClientTaskInfoService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean saveTaskInfo(TaskInfoDTO taskInfoDTO) {
        if (taskInfoDTO == null) {
            log.warn("TaskInfoDTO is null, cannot save");
            return false;
        }

        try {
            ClientTaskInfo clientTaskInfo = convertToEntity(taskInfoDTO);
            LocalDateTime now = LocalDateTime.now();
            clientTaskInfo.setCreateTime(now);
            clientTaskInfo.setUpdateTime(now);

            boolean success = save(clientTaskInfo);
            if (success) {
                log.info("ClientTaskInfo saved successfully - taskId: {}, id: {}", taskInfoDTO.getTaskId(), clientTaskInfo.getId());
            } else {
                log.error("Failed to save ClientTaskInfo - taskId: {}", taskInfoDTO.getTaskId());
            }
            return success;
        } catch (Exception e) {
            log.error("Error saving ClientTaskInfo - taskId: {}, error: {}", taskInfoDTO.getTaskId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 将TaskInfoDTO转换为ClientTaskInfo实体
     * 
     * @param dto TaskInfoDTO
     * @return ClientTaskInfo实体
     */
    private ClientTaskInfo convertToEntity(TaskInfoDTO dto) {
        ClientTaskInfo clientTaskInfo = new ClientTaskInfo();
        clientTaskInfo.setTaskId(dto.getTaskId());
        clientTaskInfo.setNation(dto.getNation());
        clientTaskInfo.setOperator(dto.getOperator());
        clientTaskInfo.setPrb(dto.getPrb());
        clientTaskInfo.setRsrp(dto.getRsrp());
        clientTaskInfo.setService(dto.getService());
        clientTaskInfo.setApp(dto.getApp());
        clientTaskInfo.setStartTime(dto.getStartTime());
        clientTaskInfo.setEndTime(dto.getEndTime());
        clientTaskInfo.setUserCategory(dto.getUserCategory());
        clientTaskInfo.setDeviceId(dto.getDeviceId());

        // 将summary Map转换为JSON字符串
        if (dto.getSummary() != null && !dto.getSummary().isEmpty()) {
            try {
                String summaryJson = objectMapper.writeValueAsString(dto.getSummary());
                clientTaskInfo.setSummary(summaryJson);
            } catch (JsonProcessingException e) {
                log.warn("Failed to convert summary to JSON - taskId: {}, error: {}", dto.getTaskId(), e.getMessage());
                clientTaskInfo.setSummary(null);
            }
        }

        return clientTaskInfo;
    }
}

