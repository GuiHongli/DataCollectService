package com.datacollect.service;

import com.datacollect.dto.TestCaseExecutionRequest;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行机WebSocket连接管理服务
 * 
 * @author system
 * @since 2024-01-01
 */
public interface ExecutorWebSocketService {
    
    /**
     * 注册执行机连接
     * 
     * @param executorIp 执行机IP
     * @param sessionId 会话ID
     */
    void registerExecutor(String executorIp, String sessionId);
    
    /**
     * 注销执行机连接
     * 
     * @param executorIp 执行机IP
     * @param sessionId 会话ID
     */
    void unregisterExecutor(String executorIp, String sessionId);
    
    /**
     * 检查执行机是否在线
     * 
     * @param executorIp 执行机IP
     * @return 是否在线
     */
    boolean isExecutorOnline(String executorIp);
    
    /**
     * 通过WebSocket发送任务到执行机
     * 
     * @param executorIp 执行机IP
     * @param request 任务请求
     * @return 是否发送成功
     */
    boolean sendTaskToExecutor(String executorIp, TestCaseExecutionRequest request);
    
    /**
     * 获取所有在线的执行机IP列表
     * 
     * @return 执行机IP列表
     */
    java.util.Set<String> getOnlineExecutors();
    
    /**
     * 获取执行机连接数
     * 
     * @return 连接数
     */
    int getConnectionCount();
}


