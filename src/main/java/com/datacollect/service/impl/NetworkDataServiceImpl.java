package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.dto.NetworkDataGroupDTO;
import com.datacollect.entity.NetworkData;
import com.datacollect.mapper.NetworkDataMapper;
import com.datacollect.service.NetworkDataService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 网络侧数据服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Service
public class NetworkDataServiceImpl extends ServiceImpl<NetworkDataMapper, NetworkData> implements NetworkDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkDataServiceImpl.class);

    @Override
    public boolean batchSaveNetworkData(List<NetworkData> networkDataList, String fileName) {
        if (networkDataList == null || networkDataList.isEmpty()) {
            LOGGER.warn("NetworkData list is empty, cannot save");
            return false;
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            LOGGER.warn("FileName is null or empty, cannot save");
            return false;
        }

        try {
            // set文件名和create时间
            LocalDateTime now = LocalDateTime.now();
            List<NetworkData> dataToSave = new ArrayList<>();
            int duplicateCount = 0;

            for (NetworkData networkData : networkDataList) {
                networkData.setFileName(fileName);
                networkData.setCreateTime(now);
                networkData.setUpdateTime(now);

                // check是否存在相同的 gpsi、time_stamp、start_time 和 sub_app_id
                if (isDuplicate(networkData)) {
                    duplicateCount++;
                    LOGGER.debug("Duplicate NetworkData found, skipping - gpsi: {}, timeStamp: {}, startTime: {}, subAppId: {}",
                            networkData.getGpsi(), networkData.getTimeStamp(), networkData.getStartTime(), networkData.getSubAppId());
                    continue;
                }

                dataToSave.add(networkData);
            }

            if (dataToSave.isEmpty()) {
                LOGGER.warn("All NetworkData records are duplicates, nothing to save - fileName: {}, total: {}, duplicates: {}",
                        fileName, networkDataList.size(), duplicateCount);
                return true;
            }

            // 批量save去重后的数据
            boolean success = saveBatch(dataToSave);
            if (success) {
                LOGGER.info("NetworkData batch saved successfully - fileName: {}, total: {}, saved: {}, duplicates: {}",
                        fileName, networkDataList.size(), dataToSave.size(), duplicateCount);
            } else {
                LOGGER.error("Failed to batch save NetworkData - fileName: {}, count: {}", fileName, dataToSave.size());
            }
            return success;
        } catch (Exception e) {
            LOGGER.error("Error batch saving NetworkData - fileName: {}, error: {}", fileName, e.getMessage(), e);
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

    @Override
    public Page<NetworkDataGroupDTO> getGroupedNetworkDataPage(Integer current, Integer size, String gpsi, String date, String subAppId) {
        Page<NetworkDataGroupDTO> page = new Page<>(current, size);
        return baseMapper.selectGroupedNetworkDataPage(page, gpsi, date, subAppId);
    }

    @Override
    public boolean deleteByDate(String gpsi, String date, String subAppId) {
        try {
            QueryWrapper<NetworkData> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("gpsi", gpsi);
            queryWrapper.eq("sub_app_id", subAppId);
            // 使用LEFT函数提取日期部分进行匹配
            queryWrapper.apply("LEFT(start_time, 10) = {0}", date);
            
            boolean success = remove(queryWrapper);
            if (success) {
                LOGGER.info("Network data deleted by date - gpsi: {}, date: {}, subAppId: {}", gpsi, date, subAppId);
            } else {
                LOGGER.warn("No network data found to delete - gpsi: {}, date: {}, subAppId: {}", gpsi, date, subAppId);
            }
            return success;
        } catch (Exception e) {
            LOGGER.error("Failed to delete network data by date - gpsi: {}, date: {}, subAppId: {}, error: {}", 
                    gpsi, date, subAppId, e.getMessage(), e);
            return false;
        }
    }
}





