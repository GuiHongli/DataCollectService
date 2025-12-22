package com.datacollect.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.NetworkData;
import com.datacollect.mapper.NetworkDataMapper;
import com.datacollect.service.NetworkDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 网络侧数据服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class NetworkDataServiceImpl extends ServiceImpl<NetworkDataMapper, NetworkData> implements NetworkDataService {

    @Override
    public boolean batchSaveNetworkData(List<NetworkData> networkDataList, String fileName) {
        if (networkDataList == null || networkDataList.isEmpty()) {
            log.warn("NetworkData list is empty, cannot save");
            return false;
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("FileName is null or empty, cannot save");
            return false;
        }

        try {
            // 设置文件名和创建时间
            LocalDateTime now = LocalDateTime.now();
            for (NetworkData networkData : networkDataList) {
                networkData.setFileName(fileName);
                networkData.setCreateTime(now);
                networkData.setUpdateTime(now);
            }

            // 批量保存
            boolean success = saveBatch(networkDataList);
            if (success) {
                log.info("NetworkData batch saved successfully - fileName: {}, count: {}", fileName, networkDataList.size());
            } else {
                log.error("Failed to batch save NetworkData - fileName: {}, count: {}", fileName, networkDataList.size());
            }
            return success;
        } catch (Exception e) {
            log.error("Error batch saving NetworkData - fileName: {}, error: {}", fileName, e.getMessage(), e);
            return false;
        }
    }
}




