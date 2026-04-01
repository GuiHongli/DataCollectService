package com.datacollect.service;

import java.util.Map;

/**
 * 用户活跃时间管理服务
 */
public interface UserActivityService {
    
    /**
     * 更新用户最后活跃时间
     */
    void updateLastActivityTime(String username);
    
    /**
     * 检查用户是否在活跃期内（1小时内）
     */
    boolean isUserActive(String username);
    
    /**
     * 清除用户活跃时间
     */
    void clearUserActivity(String username);
    
    /**
     * 获取所有用户活跃时间（用于调试）
     */
    Map<String, Long> getAllUserActivityTimes();
}






























