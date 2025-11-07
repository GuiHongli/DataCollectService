package com.datacollect.handler;

import com.alibaba.fastjson.JSON;
import com.datacollect.dto.ExecutorRegisterMessage;
import com.datacollect.dto.WebSocketMessage;
import com.datacollect.entity.Executor;
import com.datacollect.service.ExecutorService;
import com.datacollect.service.ExecutorWebSocketService;
import com.datacollect.service.impl.ExecutorWebSocketServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 执行机WebSocket消息处理器
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Component
public class ExecutorWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private ExecutorWebSocketService executorWebSocketService;
    
    @Autowired
    private ExecutorService executorService;
    
    /**
     * 心跳检查定时任务
     */
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(1);
    
    /**
     * 存储会话的最后活跃时间
     */
    private final java.util.Map<String, Long> sessionLastActiveTime = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * 心跳间隔（秒）
     */
    private static final long HEARTBEAT_INTERVAL = 30;
    
    /**
     * 连接超时时间（秒）
     */
    private static final long CONNECTION_TIMEOUT = 120;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("执行机WebSocket连接建立 - 会话ID: {}, 远程地址: {}", 
                session.getId(), getRemoteAddress(session));
        
        // 注册会话
        if (executorWebSocketService instanceof ExecutorWebSocketServiceImpl) {
            ((ExecutorWebSocketServiceImpl) executorWebSocketService).registerSession(session);
        }
        
        // 启动心跳检查
        startHeartbeatCheck(session);
        
        super.afterConnectionEstablished(session);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();
        
        log.debug("收到执行机WebSocket消息 - 会话ID: {}, 消息内容: {}", sessionId, payload);
        
        // 更新最后活跃时间
        sessionLastActiveTime.put(sessionId, System.currentTimeMillis());
        
        try {
            WebSocketMessage wsMessage = JSON.parseObject(payload, WebSocketMessage.class);
            
            if (wsMessage == null || wsMessage.getType() == null) {
                log.warn("收到无效的WebSocket消息 - 会话ID: {}, 消息: {}", sessionId, payload);
                sendErrorResponse(session, "无效的消息格式");
                return;
            }
            
            String messageType = wsMessage.getType();
            
            switch (messageType) {
                case "REGISTER":
                    handleRegisterMessage(session, wsMessage);
                    break;
                case "HEARTBEAT":
                    handleHeartbeatMessage(session, wsMessage);
                    break;
                case "TASK_RESPONSE":
                    handleTaskResponseMessage(session, wsMessage);
                    break;
                default:
                    log.warn("未知的消息类型 - 会话ID: {}, 消息类型: {}", sessionId, messageType);
                    sendErrorResponse(session, "未知的消息类型: " + messageType);
            }
            
        } catch (Exception e) {
            log.error("处理WebSocket消息异常 - 会话ID: {}, 错误: {}", sessionId, e.getMessage(), e);
            sendErrorResponse(session, "处理消息异常: " + e.getMessage());
        }
        
        super.handleTextMessage(session, message);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误 - 会话ID: {}, 错误: {}", session.getId(), exception.getMessage(), exception);
        super.handleTransportError(session, exception);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        log.info("执行机WebSocket连接关闭 - 会话ID: {}, 关闭状态: {}", sessionId, status);
        
        // 清理会话信息
        sessionLastActiveTime.remove(sessionId);
        
        // 从服务中注销执行机
        if (executorWebSocketService instanceof ExecutorWebSocketServiceImpl) {
            ExecutorWebSocketServiceImpl service = (ExecutorWebSocketServiceImpl) executorWebSocketService;
            service.unregisterBySession(sessionId);
        }
        
        super.afterConnectionClosed(session, status);
    }
    
    /**
     * 处理执行机注册消息
     */
    private void handleRegisterMessage(WebSocketSession session, WebSocketMessage wsMessage) {
        String sessionId = session.getId();
        
        try {
            ExecutorRegisterMessage registerMsg = JSON.parseObject(
                    JSON.toJSONString(wsMessage.getData()), ExecutorRegisterMessage.class);
            
            if (registerMsg == null || registerMsg.getExecutorIp() == null) {
                log.warn("注册消息缺少执行机IP - 会话ID: {}", sessionId);
                sendErrorResponse(session, "注册消息缺少执行机IP");
                return;
            }
            
            String executorIp = registerMsg.getExecutorIp();
            
            // 验证执行机是否存在
            QueryWrapper<Executor> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("ip_address", executorIp);
            Executor executor = executorService.getOne(queryWrapper);
            
            if (executor == null) {
                // 执行机不存在，自动创建
                log.info("执行机不存在，自动创建 - 执行机IP: {}, 会话ID: {}", executorIp, sessionId);
                
                executor = new Executor();
                executor.setIpAddress(executorIp);
                
                // 设置执行机名称，如果注册消息中有则使用，否则使用默认值
                String executorName = registerMsg.getExecutorName();
                if (executorName == null || executorName.trim().isEmpty()) {
                    executorName = "执行机-" + executorIp;
                }
                executor.setName(executorName);
                
                // 设置默认地域ID为1（中国片区），如果后续需要可以配置化
                executor.setRegionId(1L);
                
                // 设置描述
                if (registerMsg.getDescription() != null && !registerMsg.getDescription().trim().isEmpty()) {
                    executor.setDescription(registerMsg.getDescription());
                } else {
                    executor.setDescription("通过WebSocket自动注册的执行机");
                }
                
                // 设置状态为在线
                executor.setStatus(registerMsg.getStatus() != null ? registerMsg.getStatus() : 1);
                
                // 保存执行机
                executorService.save(executor);
                
                log.info("执行机已自动创建 - 执行机IP: {}, 执行机名称: {}, 执行机ID: {}, 会话ID: {}", 
                        executorIp, executorName, executor.getId(), sessionId);
            } else {
                // 执行机已存在，更新状态为在线
                executor.setStatus(1);
                executorService.updateById(executor);
                
                log.info("执行机已存在，更新状态为在线 - 执行机IP: {}, 执行机名称: {}, 会话ID: {}", 
                        executorIp, executor.getName(), sessionId);
            }
            
            // 注册执行机连接
            executorWebSocketService.registerExecutor(executorIp, sessionId);
            
            // 发送注册成功响应
            WebSocketMessage response = new WebSocketMessage();
            response.setType("REGISTER_RESPONSE");
            response.setTimestamp(System.currentTimeMillis());
            response.setMessageId(wsMessage.getMessageId());
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("status", "SUCCESS");
            data.put("message", "执行机注册成功");
            data.put("executorIp", executorIp);
            data.put("executorId", executor.getId());
            data.put("executorName", executor.getName());
            response.setData(data);
            
            sendMessage(session, response);
            
            log.info("执行机注册成功 - 执行机IP: {}, 执行机名称: {}, 执行机ID: {}, 会话ID: {}", 
                    executorIp, executor.getName(), executor.getId(), sessionId);
            
        } catch (Exception e) {
            log.error("处理注册消息异常 - 会话ID: {}, 错误: {}", sessionId, e.getMessage(), e);
            sendErrorResponse(session, "处理注册消息异常: " + e.getMessage());
        }
    }
    
    /**
     * 处理心跳消息
     */
    private void handleHeartbeatMessage(WebSocketSession session, WebSocketMessage wsMessage) {
        // 只需更新最后活跃时间，已经在上层处理
        String sessionId = session.getId();
        log.debug("收到心跳消息 - 会话ID: {}", sessionId);
        
        // 发送心跳响应
        WebSocketMessage response = new WebSocketMessage();
        response.setType("HEARTBEAT_RESPONSE");
        response.setTimestamp(System.currentTimeMillis());
        response.setMessageId(wsMessage.getMessageId());
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("status", "OK");
        response.setData(data);
        
        sendMessage(session, response);
    }
    
    /**
     * 处理任务响应消息
     */
    private void handleTaskResponseMessage(WebSocketSession session, WebSocketMessage wsMessage) {
        String sessionId = session.getId();
        log.info("收到任务响应消息 - 会话ID: {}, 响应: {}", sessionId, wsMessage.getData());
        // 这里可以根据需要处理任务执行结果
    }
    
    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String messageJson = JSON.toJSONString(message);
            session.sendMessage(new TextMessage(messageJson));
        } catch (IOException e) {
            log.error("发送WebSocket消息失败 - 会话ID: {}, 错误: {}", session.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 发送错误响应
     */
    private void sendErrorResponse(WebSocketSession session, String errorMessage) {
        WebSocketMessage response = new WebSocketMessage();
        response.setType("ERROR");
        response.setTimestamp(System.currentTimeMillis());
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("error", errorMessage);
        response.setData(data);
        
        sendMessage(session, response);
    }
    
    /**
     * 获取远程地址
     */
    private String getRemoteAddress(WebSocketSession session) {
        InetSocketAddress remoteAddress = session.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.toString() : "unknown";
    }
    
    /**
     * 启动心跳检查
     */
    private void startHeartbeatCheck(WebSocketSession session) {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            String sessionId = session.getId();
            Long lastActiveTime = sessionLastActiveTime.get(sessionId);
            
            if (lastActiveTime == null) {
                lastActiveTime = System.currentTimeMillis();
                sessionLastActiveTime.put(sessionId, lastActiveTime);
            }
            
            long inactiveTime = System.currentTimeMillis() - lastActiveTime;
            
            if (inactiveTime > CONNECTION_TIMEOUT * 1000) {
                log.warn("执行机连接超时，关闭连接 - 会话ID: {}, 空闲时间: {}秒", 
                        sessionId, inactiveTime / 1000);
                try {
                    session.close(CloseStatus.SESSION_NOT_RELIABLE);
                } catch (IOException e) {
                    log.error("关闭超时连接失败 - 会话ID: {}", sessionId, e);
                }
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }
}

