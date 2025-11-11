package com.datacollect.service.impl;

import com.datacollect.service.UserActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户活跃时间管理服务实现
 */
@Slf4j
@Service
public class UserActivityServiceImpl implements UserActivityService {
    
    /**
     * 存储用户最后活跃时间
     * key: username, value: 最后活跃时间戳（毫秒）
     */
    private final Map<String, Long> userActivityMap = new ConcurrentHashMap<>();
    
    /**
     * 用户活跃时间（毫秒）- 1小时
     */
    private static final long ACTIVITY_TIMEOUT = 60 * 60 * 1000;
    
    @Override
    public void updateLastActivityTime(String username) {
        if (username != null && !username.trim().isEmpty()) {
            long currentTime = System.currentTimeMillis();
            userActivityMap.put(username, currentTime);
            log.debug("更新用户活跃时间 - 用户名: {}, 时间: {}", username, currentTime);
        }
    }
    
    @Override
    public boolean isUserActive(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        Long lastActivityTime = userActivityMap.get(username);
        if (lastActivityTime == null) {
            log.debug("用户活跃时间不存在 - 用户名: {}", username);
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long inactiveTime = currentTime - lastActivityTime;
        
        if (inactiveTime > ACTIVITY_TIMEOUT) {
            log.info("用户活跃时间已过期 - 用户名: {}, 空闲时间: {} 分钟", 
                    username, inactiveTime / (60 * 1000));
            // 清除过期记录
            userActivityMap.remove(username);
            return false;
        }
        
        return true;
    }
    
    @Override
    public void clearUserActivity(String username) {
        if (username != null) {
            userActivityMap.remove(username);
            log.debug("清除用户活跃时间 - 用户名: {}", username);
        }
    }
    
    @Override
    public Map<String, Long> getAllUserActivityTimes() {
        return new ConcurrentHashMap<>(userActivityMap);
    }
}





