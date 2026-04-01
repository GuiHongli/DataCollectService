package com.datacollect.service.impl;

import com.alibaba.fastjson.JSON;
import com.datacollect.dto.TestCaseExecutionRequest;
import com.datacollect.dto.WebSocketMessage;
import com.datacollect.service.ExecutorWebSocketService;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行机WebSocket连接管理服务实现
 * 
 * @author system
 * @since 2024-01-01
 */
@Service
public class ExecutorWebSocketServiceImpl implements ExecutorWebSocketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorWebSocketServiceImpl.class);
    
    /**
     * 存储执行机MAC地址到会话ID的映射（一个执行机可能有多个会话，但通常只有一个）
     * key: executorMac, value: Set<sessionId>
     */
    private final Map<String, Set<String>> executorSessions = new ConcurrentHashMap<>();
    
    /**
     * 存储会话ID到执行机MAC地址的映射（用于反向查找）
     * key: sessionId, value: executorMac
     */
    private final Map<String, String> sessionToExecutor = new ConcurrentHashMap<>();
    
    /**
     * 存储会话ID到WebSocketSession的映射
     * key: sessionId, value: WebSocketSession
     */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void registerExecutor(String executorMac, String sessionId) {
        LOGGER.info("注册执行机连接 - 执行机MAC地址: {}, 会话ID: {}", executorMac, sessionId);
        
        executorSessions.computeIfAbsent(executorMac, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionToExecutor.put(sessionId, executorMac);
        
        LOGGER.info("执行机注册成功 - 执行机MAC地址: {}, 当前在线执行机数: {}", executorMac, executorSessions.size());
    }
    
    /**
     * 注册WebSocket会话
     * 
     * @param session WebSocket会话
     */
    public void registerSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }
    
    /**
     * 注销WebSocket会话
     * 
     * @param sessionId 会话ID
     */
    public void unregisterSession(String sessionId) {
        sessions.remove(sessionId);
    }
    
    @Override
    public void unregisterExecutor(String executorMac, String sessionId) {
        LOGGER.info("注销执行机连接 - 执行机MAC地址: {}, 会话ID: {}", executorMac, sessionId);
        
        Set<String> sessions = executorSessions.get(executorMac);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                executorSessions.remove(executorMac);
            }
        }
        sessionToExecutor.remove(sessionId);
        this.sessions.remove(sessionId);
        
        LOGGER.info("执行机注销success - 执行机MAC地址: {}, 当前在线执行机数: {}", executorMac, executorSessions.size());
    }
    
    @Override
    public boolean isExecutorOnline(String executorMac) {
        Set<String> sessions = executorSessions.get(executorMac);
        return sessions != null && !sessions.isEmpty();
    }
    
    @Override
    public boolean sendTaskToExecutor(String executorMac, TestCaseExecutionRequest request) {
        Set<String> sessionIds = executorSessions.get(executorMac);
        if (sessionIds == null || sessionIds.isEmpty()) {
            LOGGER.warn("执行机不在线，无法send任务 - 执行机MAC地址: {}, 任务ID: {}", executorMac, request.getTaskId());
            return false;
        }
        
        try {
            WebSocketMessage message = new WebSocketMessage();
            message.setType("TASK");
            message.setData(request);
            message.setTimestamp(System.currentTimeMillis());
            // 保留executorIp字段用于兼容，但实际使用MAC地址作为key
            message.setExecutorIp(executorMac);
            
            String messageJson = JSON.toJSONString(message);
            
            // 发送到所有该执行机的会话
            boolean sent = false;
            for (String sessionId : sessionIds) {
                WebSocketSession session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(messageJson));
                        sent = true;
                        LOGGER.info("任务已通过WebSocketsend到执行机 - 执行机MAC地址: {}, 任务ID: {}, 会话ID: {}", 
                                executorMac, request.getTaskId(), sessionId);
                    } catch (IOException e) {
                        LOGGER.error("发送消息到会话失败 - 会话ID: {}, 错误: {}", sessionId, e.getMessage(), e);
                        // 如果sendfailed，移除该会话
                        unregisterSession(sessionId);
                        unregisterExecutor(executorMac, sessionId);
                    }
                } else {
                    LOGGER.warn("会话不存在或已关闭 - 会话ID: {}", sessionId);
                    unregisterExecutor(executorMac, sessionId);
                }
            }
            
            return sent;
            
        } catch (Exception e) {
            LOGGER.error("通过WebSocketsend任务failed - 执行机MAC地址: {}, 任务ID: {}, error: {}", 
                    executorMac, request.getTaskId(), e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean sendCancelCommand(String executorMac, String taskId) {
        Set<String> sessionIds = executorSessions.get(executorMac);
        if (sessionIds == null || sessionIds.isEmpty()) {
            LOGGER.warn("执行机不在线，无法sendstop命令 - 执行机MAC地址: {}, 任务ID: {}", executorMac, taskId);
            return false;
        }
        
        try {
            WebSocketMessage message = new WebSocketMessage();
            message.setType("CANCEL");
            Map<String, Object> cancelData = new java.util.HashMap<>();
            cancelData.put("taskId", taskId);
            message.setData(cancelData);
            message.setTimestamp(System.currentTimeMillis());
            // 保留executorIp字段用于兼容，但实际使用MAC地址作为key
            message.setExecutorIp(executorMac);
            
            String messageJson = JSON.toJSONString(message);
            
            // 发送到所有该执行机的会话
            boolean sent = false;
            for (String sessionId : sessionIds) {
                WebSocketSession session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(messageJson));
                        sent = true;
                        LOGGER.info("stop命令已通过WebSocketsend到执行机 - 执行机MAC地址: {}, 任务ID: {}, 会话ID: {}", 
                                executorMac, taskId, sessionId);
                    } catch (IOException e) {
                        LOGGER.error("发送停止命令到会话失败 - 会话ID: {}, 错误: {}", sessionId, e.getMessage(), e);
                        // 如果sendfailed，移除该会话
                        unregisterSession(sessionId);
                        unregisterExecutor(executorMac, sessionId);
                    }
                } else {
                    LOGGER.warn("会话不存在或已关闭 - 会话ID: {}", sessionId);
                    unregisterExecutor(executorMac, sessionId);
                }
            }
            
            return sent;
            
        } catch (Exception e) {
            LOGGER.error("通过WebSocket发送停止命令失败 - 执行机MAC地址: {}, 任务ID: {}, 错误: {}", 
                    executorMac, taskId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Set<String> getOnlineExecutors() {
        return executorSessions.keySet();
    }
    
    @Override
    public int getConnectionCount() {
        return executorSessions.size();
    }
    
    /**
     * 根据会话IDget执行机MAC地址
     * 
     * @param sessionId 会话ID
     * @return 执行机MAC地址
     */
    public String getExecutorMacBySession(String sessionId) {
        return sessionToExecutor.get(sessionId);
    }
    
    /**
     * 根据会话ID注销连接
     * 
     * @param sessionId 会话ID
     */
    public void unregisterBySession(String sessionId) {
        String executorMac = sessionToExecutor.get(sessionId);
        if (executorMac != null) {
            unregisterExecutor(executorMac, sessionId);
        } else {
            unregisterSession(sessionId);
        }
    }
}


import com.alibaba.fastjson.JSON;
import com.datacollect.dto.TestCaseExecutionRequest;
import com.datacollect.dto.WebSocketMessage;
import com.datacollect.service.ExecutorWebSocketService;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 执行机WebSocket连接管理服务实现
 * 
 * @author system
 * @since 2024-01-01
 */
@Service
public class ExecutorWebSocketServiceImpl implements ExecutorWebSocketService {
    
    /**
     * 存储执行机MAC地址到会话ID的映射（一个执行机可能有多个会话，但通常只有一个）
     * key: executorMac, value: Set<sessionId>
     */
    private final Map<String, Set<String>> executorSessions = new ConcurrentHashMap<>();
    
    /**
     * 存储会话ID到执行机MAC地址的映射（用于反向查找）
     * key: sessionId, value: executorMac
     */
    private final Map<String, String> sessionToExecutor = new ConcurrentHashMap<>();
    
    /**
     * 存储会话ID到WebSocketSession的映射
     * key: sessionId, value: WebSocketSession
     */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void registerExecutor(String executorMac, String sessionId) {
        LOGGER.info("注册执行机连接 - 执行机MAC地址: {}, 会话ID: {}", executorMac, sessionId);
        
        executorSessions.computeIfAbsent(executorMac, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionToExecutor.put(sessionId, executorMac);
        
        LOGGER.info("执行机注册成功 - 执行机MAC地址: {}, 当前在线执行机数: {}", executorMac, executorSessions.size());
    }
    
    /**
     * 注册WebSocket会话
     * 
     * @param session WebSocket会话
     */
    public void registerSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }
    
    /**
     * 注销WebSocket会话
     * 
     * @param sessionId 会话ID
     */
    public void unregisterSession(String sessionId) {
        sessions.remove(sessionId);
    }
    
    @Override
    public void unregisterExecutor(String executorMac, String sessionId) {
        LOGGER.info("注销执行机连接 - 执行机MAC地址: {}, 会话ID: {}", executorMac, sessionId);
        
        Set<String> sessions = executorSessions.get(executorMac);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                executorSessions.remove(executorMac);
            }
        }
        sessionToExecutor.remove(sessionId);
        this.sessions.remove(sessionId);
        
        LOGGER.info("执行机注销success - 执行机MAC地址: {}, 当前在线执行机数: {}", executorMac, executorSessions.size());
    }
    
    @Override
    public boolean isExecutorOnline(String executorMac) {
        Set<String> sessions = executorSessions.get(executorMac);
        return sessions != null && !sessions.isEmpty();
    }
    
    @Override
    public boolean sendTaskToExecutor(String executorMac, TestCaseExecutionRequest request) {
        Set<String> sessionIds = executorSessions.get(executorMac);
        if (sessionIds == null || sessionIds.isEmpty()) {
            LOGGER.warn("执行机不在线，无法send任务 - 执行机MAC地址: {}, 任务ID: {}", executorMac, request.getTaskId());
            return false;
        }
        
        try {
            WebSocketMessage message = new WebSocketMessage();
            message.setType("TASK");
            message.setData(request);
            message.setTimestamp(System.currentTimeMillis());
            // 保留executorIp字段用于兼容，但实际使用MAC地址作为key
            message.setExecutorIp(executorMac);
            
            String messageJson = JSON.toJSONString(message);
            
            // 发送到所有该执行机的会话
            boolean sent = false;
            for (String sessionId : sessionIds) {
                WebSocketSession session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(messageJson));
                        sent = true;
                        LOGGER.info("任务已通过WebSocketsend到执行机 - 执行机MAC地址: {}, 任务ID: {}, 会话ID: {}", 
                                executorMac, request.getTaskId(), sessionId);
                    } catch (IOException e) {
                        LOGGER.error("发送消息到会话失败 - 会话ID: {}, 错误: {}", sessionId, e.getMessage(), e);
                        // 如果sendfailed，移除该会话
                        unregisterSession(sessionId);
                        unregisterExecutor(executorMac, sessionId);
                    }
                } else {
                    LOGGER.warn("会话不存在或已关闭 - 会话ID: {}", sessionId);
                    unregisterExecutor(executorMac, sessionId);
                }
            }
            
            return sent;
            
        } catch (Exception e) {
            LOGGER.error("通过WebSocketsend任务failed - 执行机MAC地址: {}, 任务ID: {}, error: {}", 
                    executorMac, request.getTaskId(), e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean sendCancelCommand(String executorMac, String taskId) {
        Set<String> sessionIds = executorSessions.get(executorMac);
        if (sessionIds == null || sessionIds.isEmpty()) {
            LOGGER.warn("执行机不在线，无法sendstop命令 - 执行机MAC地址: {}, 任务ID: {}", executorMac, taskId);
            return false;
        }
        
        try {
            WebSocketMessage message = new WebSocketMessage();
            message.setType("CANCEL");
            Map<String, Object> cancelData = new java.util.HashMap<>();
            cancelData.put("taskId", taskId);
            message.setData(cancelData);
            message.setTimestamp(System.currentTimeMillis());
            // 保留executorIp字段用于兼容，但实际使用MAC地址作为key
            message.setExecutorIp(executorMac);
            
            String messageJson = JSON.toJSONString(message);
            
            // 发送到所有该执行机的会话
            boolean sent = false;
            for (String sessionId : sessionIds) {
                WebSocketSession session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(messageJson));
                        sent = true;
                        LOGGER.info("stop命令已通过WebSocketsend到执行机 - 执行机MAC地址: {}, 任务ID: {}, 会话ID: {}", 
                                executorMac, taskId, sessionId);
                    } catch (IOException e) {
                        LOGGER.error("发送停止命令到会话失败 - 会话ID: {}, 错误: {}", sessionId, e.getMessage(), e);
                        // 如果sendfailed，移除该会话
                        unregisterSession(sessionId);
                        unregisterExecutor(executorMac, sessionId);
                    }
                } else {
                    LOGGER.warn("会话不存在或已关闭 - 会话ID: {}", sessionId);
                    unregisterExecutor(executorMac, sessionId);
                }
            }
            
            return sent;
            
        } catch (Exception e) {
            LOGGER.error("通过WebSocketsendstop命令failed - 执行机MAC地址: {}, 任务ID: {}, error: {}", 
                    executorMac, taskId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Set<String> getOnlineExecutors() {
        return executorSessions.keySet();
    }
    
    @Override
    public int getConnectionCount() {
        return executorSessions.size();
    }
    
    /**
     * 根据会话ID获取执行机MAC地址
     * 
     * @param sessionId 会话ID
     * @return 执行机MAC地址
     */
    public String getExecutorMacBySession(String sessionId) {
        return sessionToExecutor.get(sessionId);
    }
    
    /**
     * 根据会话ID注销连接
     * 
     * @param sessionId 会话ID
     */
    public void unregisterBySession(String sessionId) {
        String executorMac = sessionToExecutor.get(sessionId);
        if (executorMac != null) {
            unregisterExecutor(executorMac, sessionId);
        } else {
            unregisterSession(sessionId);
        }
    }
}






