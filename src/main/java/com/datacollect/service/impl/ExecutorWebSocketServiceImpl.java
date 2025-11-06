package com.datacollect.service.impl;

import com.alibaba.fastjson.JSON;
import com.datacollect.dto.TestCaseExecutionRequest;
import com.datacollect.dto.WebSocketMessage;
import com.datacollect.service.ExecutorWebSocketService;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
public class ExecutorWebSocketServiceImpl implements ExecutorWebSocketService {
    
    /**
     * 存储执行机IP到会话ID的映射（一个执行机可能有多个会话，但通常只有一个）
     * key: executorIp, value: Set<sessionId>
     */
    private final Map<String, Set<String>> executorSessions = new ConcurrentHashMap<>();
    
    /**
     * 存储会话ID到执行机IP的映射（用于反向查找）
     * key: sessionId, value: executorIp
     */
    private final Map<String, String> sessionToExecutor = new ConcurrentHashMap<>();
    
    /**
     * 存储会话ID到WebSocketSession的映射
     * key: sessionId, value: WebSocketSession
     */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void registerExecutor(String executorIp, String sessionId) {
        log.info("注册执行机连接 - 执行机IP: {}, 会话ID: {}", executorIp, sessionId);
        
        executorSessions.computeIfAbsent(executorIp, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionToExecutor.put(sessionId, executorIp);
        
        log.info("执行机注册成功 - 执行机IP: {}, 当前在线执行机数: {}", executorIp, executorSessions.size());
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
    public void unregisterExecutor(String executorIp, String sessionId) {
        log.info("注销执行机连接 - 执行机IP: {}, 会话ID: {}", executorIp, sessionId);
        
        Set<String> sessions = executorSessions.get(executorIp);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                executorSessions.remove(executorIp);
            }
        }
        sessionToExecutor.remove(sessionId);
        this.sessions.remove(sessionId);
        
        log.info("执行机注销成功 - 执行机IP: {}, 当前在线执行机数: {}", executorIp, executorSessions.size());
    }
    
    @Override
    public boolean isExecutorOnline(String executorIp) {
        Set<String> sessions = executorSessions.get(executorIp);
        return sessions != null && !sessions.isEmpty();
    }
    
    @Override
    public boolean sendTaskToExecutor(String executorIp, TestCaseExecutionRequest request) {
        Set<String> sessionIds = executorSessions.get(executorIp);
        if (sessionIds == null || sessionIds.isEmpty()) {
            log.warn("执行机不在线，无法发送任务 - 执行机IP: {}, 任务ID: {}", executorIp, request.getTaskId());
            return false;
        }
        
        try {
            WebSocketMessage message = new WebSocketMessage();
            message.setType("TASK");
            message.setData(request);
            message.setTimestamp(System.currentTimeMillis());
            message.setExecutorIp(executorIp);
            
            String messageJson = JSON.toJSONString(message);
            
            // 发送到所有该执行机的会话
            boolean sent = false;
            for (String sessionId : sessionIds) {
                WebSocketSession session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(messageJson));
                        sent = true;
                        log.info("任务已通过WebSocket发送到执行机 - 执行机IP: {}, 任务ID: {}, 会话ID: {}", 
                                executorIp, request.getTaskId(), sessionId);
                    } catch (IOException e) {
                        log.error("发送消息到会话失败 - 会话ID: {}, 错误: {}", sessionId, e.getMessage(), e);
                        // 如果发送失败，移除该会话
                        unregisterSession(sessionId);
                        unregisterExecutor(executorIp, sessionId);
                    }
                } else {
                    log.warn("会话不存在或已关闭 - 会话ID: {}", sessionId);
                    unregisterExecutor(executorIp, sessionId);
                }
            }
            
            return sent;
            
        } catch (Exception e) {
            log.error("通过WebSocket发送任务失败 - 执行机IP: {}, 任务ID: {}, 错误: {}", 
                    executorIp, request.getTaskId(), e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean sendCancelCommand(String executorIp, String taskId) {
        Set<String> sessionIds = executorSessions.get(executorIp);
        if (sessionIds == null || sessionIds.isEmpty()) {
            log.warn("执行机不在线，无法发送停止命令 - 执行机IP: {}, 任务ID: {}", executorIp, taskId);
            return false;
        }
        
        try {
            WebSocketMessage message = new WebSocketMessage();
            message.setType("CANCEL");
            Map<String, Object> cancelData = new java.util.HashMap<>();
            cancelData.put("taskId", taskId);
            message.setData(cancelData);
            message.setTimestamp(System.currentTimeMillis());
            message.setExecutorIp(executorIp);
            
            String messageJson = JSON.toJSONString(message);
            
            // 发送到所有该执行机的会话
            boolean sent = false;
            for (String sessionId : sessionIds) {
                WebSocketSession session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(messageJson));
                        sent = true;
                        log.info("停止命令已通过WebSocket发送到执行机 - 执行机IP: {}, 任务ID: {}, 会话ID: {}", 
                                executorIp, taskId, sessionId);
                    } catch (IOException e) {
                        log.error("发送停止命令到会话失败 - 会话ID: {}, 错误: {}", sessionId, e.getMessage(), e);
                        // 如果发送失败，移除该会话
                        unregisterSession(sessionId);
                        unregisterExecutor(executorIp, sessionId);
                    }
                } else {
                    log.warn("会话不存在或已关闭 - 会话ID: {}", sessionId);
                    unregisterExecutor(executorIp, sessionId);
                }
            }
            
            return sent;
            
        } catch (Exception e) {
            log.error("通过WebSocket发送停止命令失败 - 执行机IP: {}, 任务ID: {}, 错误: {}", 
                    executorIp, taskId, e.getMessage(), e);
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
     * 根据会话ID获取执行机IP
     * 
     * @param sessionId 会话ID
     * @return 执行机IP
     */
    public String getExecutorIpBySession(String sessionId) {
        return sessionToExecutor.get(sessionId);
    }
    
    /**
     * 根据会话ID注销连接
     * 
     * @param sessionId 会话ID
     */
    public void unregisterBySession(String sessionId) {
        String executorIp = sessionToExecutor.get(sessionId);
        if (executorIp != null) {
            unregisterExecutor(executorIp, sessionId);
        } else {
            unregisterSession(sessionId);
        }
    }
}

