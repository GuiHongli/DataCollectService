package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.NetworkData;
import com.datacollect.mapper.NetworkDataMapper;
import com.datacollect.service.NetworkDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
            List<NetworkData> dataToSave = new ArrayList<>();
            int duplicateCount = 0;

            for (NetworkData networkData : networkDataList) {
                networkData.setFileName(fileName);
                networkData.setCreateTime(now);
                networkData.setUpdateTime(now);

                // 检查是否存在相同的 gpsi、time_stamp、start_time 和 sub_app_id
                if (isDuplicate(networkData)) {
                    duplicateCount++;
                    log.debug("Duplicate NetworkData found, skipping - gpsi: {}, timeStamp: {}, startTime: {}, subAppId: {}",
                            networkData.getGpsi(), networkData.getTimeStamp(), networkData.getStartTime(), networkData.getSubAppId());
                    continue;
                }

                dataToSave.add(networkData);
            }

            if (dataToSave.isEmpty()) {
                log.warn("All NetworkData records are duplicates, nothing to save - fileName: {}, total: {}, duplicates: {}",
                        fileName, networkDataList.size(), duplicateCount);
                return true;
            }

            // 批量保存去重后的数据
            boolean success = saveBatch(dataToSave);
            if (success) {
                log.info("NetworkData batch saved successfully - fileName: {}, total: {}, saved: {}, duplicates: {}",
                        fileName, networkDataList.size(), dataToSave.size(), duplicateCount);
            } else {
                log.error("Failed to batch save NetworkData - fileName: {}, count: {}", fileName, dataToSave.size());
            }
            return success;
        } catch (Exception e) {
            log.error("Error batch saving NetworkData - fileName: {}, error: {}", fileName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查是否存在相同的 gpsi、time_stamp、start_time 和 sub_app_id 的记录
     *
     * @param networkData 网络侧数据
     * @return true 如果存在重复记录，false 如果不存在
     */
    private boolean isDuplicate(NetworkData networkData) {
        LambdaQueryWrapper<NetworkData> queryWrapper = new LambdaQueryWrapper<>();
        
        // 检查 gpsi
        if (StringUtils.hasText(networkData.getGpsi())) {
            queryWrapper.eq(NetworkData::getGpsi, networkData.getGpsi());
        } else {
            queryWrapper.isNull(NetworkData::getGpsi);
        }
        
        // 检查 time_stamp
        if (StringUtils.hasText(networkData.getTimeStamp())) {
            queryWrapper.eq(NetworkData::getTimeStamp, networkData.getTimeStamp());
        } else {
            queryWrapper.isNull(NetworkData::getTimeStamp);
        }
        
        // 检查 start_time
        if (StringUtils.hasText(networkData.getStartTime())) {
            queryWrapper.eq(NetworkData::getStartTime, networkData.getStartTime());
        } else {
            queryWrapper.isNull(NetworkData::getStartTime);
        }
        
        // 检查 sub_app_id
        if (StringUtils.hasText(networkData.getSubAppId())) {
            queryWrapper.eq(NetworkData::getSubAppId, networkData.getSubAppId());
        } else {
            queryWrapper.isNull(NetworkData::getSubAppId);
        }

        // 查询是否存在记录
        long count = count(queryWrapper);
        return count > 0;
    }
}





