package com.datacollect.service.schedule;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datacollect.entity.NetworkData;
import com.datacollect.mapper.NetworkDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网络侧数据清理定时任务服务
 * 每三个月执行一次，物理删除三个月之前的网络侧数据
 * 
 * @author system
 * @since 2024-01-01
 */
@Service
public class NetworkDataCleanupScheduleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkDataCleanupScheduleService.class);

    @Autowired
    private NetworkDataMapper networkDataMapper;

    /**
     * 每三个月执行一次数据清理
     * cron表达式: 0 0 0 1 1,4,7,10 * 表示每年1月、4月、7月、10月的第一天凌晨0点执行
     * 这样每3个月执行一次
     */
    @Scheduled(cron = "0 0 0 1 1,4,7,10 *")
    public void scheduledCleanupOldData() {
        LOGGER.info("Scheduled task: Starting network data cleanup (physical delete)...");
        
        try {
            // 计算3个月前的日期时间
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            LOGGER.info("Cleaning up network data before: {}", threeMonthsAgo);
            
            // 查询3个月前的网络侧数据（只处理未逻辑删除的数据）
            // 使用Mapper直接查询，避免逻辑删除过滤
            QueryWrapper<NetworkData> queryWrapper = new QueryWrapper<>();
            queryWrapper.lt("create_time", threeMonthsAgo);
            queryWrapper.eq("deleted", 0); // 只处理未逻辑删除的数据
            List<NetworkData> oldNetworkData = networkDataMapper.selectList(queryWrapper);
            
            if (oldNetworkData == null || oldNetworkData.isEmpty()) {
                LOGGER.info("No old network data found to cleanup");
                return;
            }
            
            LOGGER.info("Found {} old network data records to cleanup", oldNetworkData.size());
            
            int deletedCount = 0;
            
            // 遍历每条数据，物理删除
            for (NetworkData networkData : oldNetworkData) {
                try {
                    // 物理删除
                    QueryWrapper<NetworkData> deleteWrapper = new QueryWrapper<>();
                    deleteWrapper.eq("id", networkData.getId());
                    networkDataMapper.delete(deleteWrapper);
                    deletedCount++;
                } catch (Exception e) {
                    LOGGER.error("Failed to cleanup network data with id: {}, error: {}", 
                            networkData.getId(), e.getMessage(), e);
                    // 继续处理下一条数据
                }
            }
            
            LOGGER.info("Scheduled task: Network data cleanup completed. Deleted {} records", deletedCount);
            
        } catch (Exception e) {
            LOGGER.error("Scheduled task: Failed to cleanup old network data - error: {}", e.getMessage(), e);
            // 不抛出异常，避免影响定时任务继续执行
        }
    }
}

