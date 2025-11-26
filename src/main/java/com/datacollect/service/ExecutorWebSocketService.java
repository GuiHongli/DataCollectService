package com.datacollect.service;

import com.datacollect.dto.TestCaseExecutionRequest;

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
     * @param executorMac 执行机MAC地址
     * @param sessionId 会话ID
     */
    void registerExecutor(String executorMac, String sessionId);
    
    /**
     * 注销执行机连接
     * 
     * @param executorMac 执行机MAC地址
     * @param sessionId 会话ID
     */
    void unregisterExecutor(String executorMac, String sessionId);
    
    /**
     * 检查执行机是否在线
     * 
     * @param executorMac 执行机MAC地址
     * @return 是否在线
     */
    boolean isExecutorOnline(String executorMac);
    
    /**
     * 通过WebSocket发送任务到执行机
     * 
     * @param executorMac 执行机MAC地址
     * @param request 任务请求
     * @return 是否发送成功
     */
    boolean sendTaskToExecutor(String executorMac, TestCaseExecutionRequest request);
    
    /**
     * 通过WebSocket发送停止命令到执行机
     * 
     * @param executorMac 执行机MAC地址
     * @param taskId 任务ID
     * @return 是否发送成功
     */
    boolean sendCancelCommand(String executorMac, String taskId);
    
    /**
     * 获取所有在线的执行机MAC地址列表
     * 
     * @return 执行机MAC地址列表
     */
    java.util.Set<String> getOnlineExecutors();
    
    /**
     * 获取执行机连接数
     * 
     * @return 连接数
     */
    int getConnectionCount();
}



import com.datacollect.dto.TestCaseExecutionRequest;

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
     * @param executorMac 执行机MAC地址
     * @param sessionId 会话ID
     */
    void registerExecutor(String executorMac, String sessionId);
    
    /**
     * 注销执行机连接
     * 
     * @param executorMac 执行机MAC地址
     * @param sessionId 会话ID
     */
    void unregisterExecutor(String executorMac, String sessionId);
    
    /**
     * 检查执行机是否在线
     * 
     * @param executorMac 执行机MAC地址
     * @return 是否在线
     */
    boolean isExecutorOnline(String executorMac);
    
    /**
     * 通过WebSocket发送任务到执行机
     * 
     * @param executorMac 执行机MAC地址
     * @param request 任务请求
     * @return 是否发送成功
     */
    boolean sendTaskToExecutor(String executorMac, TestCaseExecutionRequest request);
    
    /**
     * 通过WebSocket发送停止命令到执行机
     * 
     * @param executorMac 执行机MAC地址
     * @param taskId 任务ID
     * @return 是否发送成功
     */
    boolean sendCancelCommand(String executorMac, String taskId);
    
    /**
     * 获取所有在线的执行机MAC地址列表
     * 
     * @return 执行机MAC地址列表
     */
    java.util.Set<String> getOnlineExecutors();
    
    /**
     * 获取执行机连接数
     * 
     * @return 连接数
     */
    int getConnectionCount();
}







