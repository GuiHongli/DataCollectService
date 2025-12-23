package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.dto.TaskInfoDTO;
import com.datacollect.entity.ClientTaskInfo;

/**
 * 端侧任务信息服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface TaskInfoService extends IService<ClientTaskInfo> {
    
    /**
     * 保存任务信息
     * 
     * @param taskInfoDTO 任务信息DTO
     * @return 是否保存成功
     */
    boolean saveTaskInfo(TaskInfoDTO taskInfoDTO);
}









