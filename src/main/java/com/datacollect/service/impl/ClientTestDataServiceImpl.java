package com.datacollect.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.ClientTestData;
import com.datacollect.mapper.ClientTestDataMapper;
import com.datacollect.service.ClientTestDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 端侧测试数据服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class ClientTestDataServiceImpl extends ServiceImpl<ClientTestDataMapper, ClientTestData> implements ClientTestDataService {

    @Override
    public boolean batchSaveClientTestData(List<ClientTestData> clientTestDataList, String taskId) {
        if (clientTestDataList == null || clientTestDataList.isEmpty()) {
            log.warn("ClientTestData list is empty, cannot save");
            return false;
        }

        if (taskId == null || taskId.trim().isEmpty()) {
            log.warn("TaskId is null or empty, cannot save");
            return false;
        }

        try {
            // 设置任务ID和创建时间
            LocalDateTime now = LocalDateTime.now();
            for (ClientTestData clientTestData : clientTestDataList) {
                clientTestData.setTaskId(taskId);
                clientTestData.setCreateTime(now);
                clientTestData.setUpdateTime(now);
            }

            // 批量保存
            boolean success = saveBatch(clientTestDataList);
            if (success) {
                log.info("ClientTestData batch saved successfully - taskId: {}, count: {}", taskId, clientTestDataList.size());
            } else {
                log.error("Failed to batch save ClientTestData - taskId: {}, count: {}", taskId, clientTestDataList.size());
            }
            return success;
        } catch (Exception e) {
            log.error("Error batch saving ClientTestData - taskId: {}, error: {}", taskId, e.getMessage(), e);
            return false;
        }
    }
}

