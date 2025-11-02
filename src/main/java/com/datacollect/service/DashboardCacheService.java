package com.datacollect.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datacollect.entity.DashboardCache;
import com.datacollect.mapper.DashboardCacheMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 仪表盘数据缓存服务（数据库存储）
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class DashboardCacheService {
    
    @Autowired
    private DashboardCacheMapper dashboardCacheMapper;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 获取仪表盘总体统计数据
     * 
     * @return 统计数据
     */
    public Map<String, Object> getStats() {
        try {
            QueryWrapper<DashboardCache> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("cache_key", DashboardCache.OVERALL_STATS_KEY);
            queryWrapper.eq("cache_type", DashboardCache.CacheType.OVERALL);
            
            DashboardCache cache = dashboardCacheMapper.selectOne(queryWrapper);
            
            if (cache == null || cache.getCacheValue() == null) {
                log.warn("Dashboard overall stats cache not found in database");
                return new HashMap<>();
            }
            
            // 解析JSON字符串为Map
            Map<String, Object> stats = objectMapper.readValue(
                    cache.getCacheValue(), 
                    new TypeReference<Map<String, Object>>() {}
            );
            
            log.debug("Dashboard overall stats retrieved from database cache");
            return stats;
            
        } catch (Exception e) {
            log.error("Failed to get dashboard stats from database cache - error: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * 更新仪表盘总体统计数据
     * 
     * @param stats 统计数据
     */
    @Transactional
    public void updateStats(Map<String, Object> stats) {
        try {
            // 将Map转换为JSON字符串
            String cacheValue = objectMapper.writeValueAsString(stats);
            
            QueryWrapper<DashboardCache> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("cache_key", DashboardCache.OVERALL_STATS_KEY);
            queryWrapper.eq("cache_type", DashboardCache.CacheType.OVERALL);
            
            DashboardCache cache = dashboardCacheMapper.selectOne(queryWrapper);
            
            if (cache == null) {
                // 插入新记录
                cache = new DashboardCache();
                cache.setCacheKey(DashboardCache.OVERALL_STATS_KEY);
                cache.setCacheType(DashboardCache.CacheType.OVERALL);
                cache.setCacheValue(cacheValue);
                dashboardCacheMapper.insert(cache);
                log.info("Dashboard overall stats cache inserted into database");
            } else {
                // 更新现有记录
                cache.setCacheValue(cacheValue);
                dashboardCacheMapper.updateById(cache);
                log.info("Dashboard overall stats cache updated in database");
            }
            
        } catch (Exception e) {
            log.error("Failed to update dashboard stats in database cache - error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update dashboard stats cache", e);
        }
    }
    
    /**
     * 获取地域统计数据
     * 
     * @param regionId 地域ID
     * @return 统计数据，如果不存在返回空Map
     */
    public Map<String, Object> getRegionStats(Long regionId) {
        try {
            QueryWrapper<DashboardCache> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("cache_key", String.valueOf(regionId));
            queryWrapper.eq("cache_type", DashboardCache.CacheType.REGION);
            
            DashboardCache cache = dashboardCacheMapper.selectOne(queryWrapper);
            
            if (cache == null || cache.getCacheValue() == null) {
                log.debug("Region stats cache not found in database for region ID: {}", regionId);
                return new HashMap<>();
            }
            
            // 解析JSON字符串为Map
            Map<String, Object> stats = objectMapper.readValue(
                    cache.getCacheValue(), 
                    new TypeReference<Map<String, Object>>() {}
            );
            
            log.debug("Region stats retrieved from database cache for region ID: {}", regionId);
            return stats;
            
        } catch (Exception e) {
            log.error("Failed to get region stats from database cache - region ID: {}, error: {}", 
                    regionId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * 更新地域统计数据
     * 
     * @param regionId 地域ID
     * @param stats 统计数据
     */
    @Transactional
    public void updateRegionStats(Long regionId, Map<String, Object> stats) {
        try {
            // 将Map转换为JSON字符串
            String cacheValue = objectMapper.writeValueAsString(stats);
            
            QueryWrapper<DashboardCache> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("cache_key", String.valueOf(regionId));
            queryWrapper.eq("cache_type", DashboardCache.CacheType.REGION);
            
            DashboardCache cache = dashboardCacheMapper.selectOne(queryWrapper);
            
            if (cache == null) {
                // 插入新记录
                cache = new DashboardCache();
                cache.setCacheKey(String.valueOf(regionId));
                cache.setCacheType(DashboardCache.CacheType.REGION);
                cache.setCacheValue(cacheValue);
                dashboardCacheMapper.insert(cache);
                log.debug("Region stats cache inserted into database for region ID: {}", regionId);
            } else {
                // 更新现有记录
                cache.setCacheValue(cacheValue);
                dashboardCacheMapper.updateById(cache);
                log.debug("Region stats cache updated in database for region ID: {}", regionId);
            }
            
        } catch (Exception e) {
            log.error("Failed to update region stats in database cache - region ID: {}, error: {}", 
                    regionId, e.getMessage(), e);
            throw new RuntimeException("Failed to update region stats cache for region: " + regionId, e);
        }
    }
    
    /**
     * 清除所有地域统计数据缓存
     */
    @Transactional
    public void clearRegionStatsCache() {
        try {
            QueryWrapper<DashboardCache> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("cache_type", DashboardCache.CacheType.REGION);
            dashboardCacheMapper.delete(queryWrapper);
            log.info("All region stats cache cleared from database");
        } catch (Exception e) {
            log.error("Failed to clear region stats cache from database - error: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取统计缓存更新时间
     * 
     * @return 更新时间戳，如果不存在返回0
     */
    public long getStatsCacheUpdateTime() {
        try {
            QueryWrapper<DashboardCache> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("cache_key", DashboardCache.OVERALL_STATS_KEY);
            queryWrapper.eq("cache_type", DashboardCache.CacheType.OVERALL);
            
            DashboardCache cache = dashboardCacheMapper.selectOne(queryWrapper);
            
            if (cache != null && cache.getUpdateTime() != null) {
                return cache.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
        } catch (Exception e) {
            log.warn("Failed to get stats cache update time - error: {}", e.getMessage());
        }
        return 0;
    }
    
    /**
     * 获取地域统计缓存更新时间
     * 
     * @return 更新时间戳，如果不存在返回0
     */
    public long getRegionStatsCacheUpdateTime() {
        try {
            QueryWrapper<DashboardCache> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("cache_type", DashboardCache.CacheType.REGION);
            queryWrapper.orderByDesc("update_time");
            queryWrapper.last("LIMIT 1");
            
            DashboardCache cache = dashboardCacheMapper.selectOne(queryWrapper);
            
            if (cache != null && cache.getUpdateTime() != null) {
                return cache.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
        } catch (Exception e) {
            log.warn("Failed to get region stats cache update time - error: {}", e.getMessage());
        }
        return 0;
    }
}
