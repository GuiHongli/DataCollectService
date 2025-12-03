package com.datacollect.handler;

import com.alibaba.fastjson.JSON;
import com.datacollect.dto.ExecutorRegisterMessage;
import com.datacollect.dto.WebSocketMessage;
import com.datacollect.entity.Executor;
import com.datacollect.entity.ExecutorMacAddress;
import com.datacollect.service.ExecutorService;
import com.datacollect.service.ExecutorMacAddressService;
import com.datacollect.service.ExecutorWebSocketService;
import com.datacollect.service.RegionService;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;

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
    
    @Autowired
    private ExecutorMacAddressService executorMacAddressService;
    
    @Autowired
    private RegionService regionService;
    
    /**
     * 心跳检查定时任务
     */
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(1);
    
    /**
     * 存储会话的最后活跃时间
     */
    private final java.util.Map<String, Long> sessionLastActiveTime = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * 存储每个会话的心跳检查定时任务，用于取消任务
     */
    private final java.util.Map<String, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    
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
        
        // 取消心跳检查定时任务
        ScheduledFuture<?> heartbeatTask = heartbeatTasks.remove(sessionId);
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            log.debug("已取消心跳检查定时任务 - 会话ID: {}", sessionId);
        }
        
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
            
            // MAC地址是必需的
            if (registerMsg == null || registerMsg.getExecutorMac() == null || registerMsg.getExecutorMac().trim().isEmpty()) {
                log.warn("注册消息缺少MAC地址 - 会话ID: {}", sessionId);
                sendErrorResponse(session, "注册消息缺少MAC地址，MAC地址是唯一标识");
                return;
            }
            
            String executorIp = registerMsg.getExecutorIp();
            String executorMac = registerMsg.getExecutorMac().trim();
            
            log.info("收到执行机注册消息 - 执行机IP: {}, MAC地址: {}, 会话ID: {}", 
                    executorIp != null ? executorIp : "未提供", executorMac, sessionId);
            
            // 使用MAC地址查找执行机（优先通过mac_address字段查找）
            QueryWrapper<Executor> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("mac_address", executorMac);
            Executor executor = executorService.getOne(queryWrapper);
            
            // 如果通过mac_address字段没找到，尝试通过ExecutorMacAddress表查找
            if (executor == null) {
                ExecutorMacAddress macAddress = executorMacAddressService.getByMacAddress(executorMac);
                if (macAddress != null && macAddress.getIpAddress() != null) {
                    // 通过IP地址查找执行机
                    QueryWrapper<Executor> ipQueryWrapper = new QueryWrapper<>();
                    ipQueryWrapper.eq("ip_address", macAddress.getIpAddress());
                    executor = executorService.getOne(ipQueryWrapper);
                }
            }
            
            if (executor == null) {
                // 执行机不存在，先检查IP地址是否已存在
                boolean ipExists = false;
                if (executorIp != null && !executorIp.trim().isEmpty()) {
                    QueryWrapper<Executor> ipQueryWrapper = new QueryWrapper<>();
                    ipQueryWrapper.eq("ip_address", executorIp);
                    Executor existingExecutor = executorService.getOne(ipQueryWrapper);
                    
                    if (existingExecutor != null) {
                        // IP地址已存在，更新mac_address_id
                        log.info("IP地址已存在，更新mac_address_id - IP地址: {}, MAC地址: {}, 执行机ID: {}, 会话ID: {}", 
                                executorIp, executorMac, existingExecutor.getId(), sessionId);
                        
                        executor = existingExecutor;
                        ipExists = true;
                        
                        // 更新MAC地址
                        executor.setMacAddress(executorMac);
                        
                        // 更新状态为在线
                        executor.setStatus(registerMsg.getStatus() != null ? registerMsg.getStatus() : 1);
                        
                        executorService.updateById(executor);
                        
                        log.info("已更新执行机的mac_address - IP地址: {}, MAC地址: {}, 执行机ID: {}, 会话ID: {}", 
                                executorIp, executorMac, executor.getId(), sessionId);
                    }
                }
                
                if (!ipExists) {
                    // IP地址不存在，自动创建新执行机
                    log.info("执行机不存在，自动创建 - MAC地址: {}, 执行机IP: {}, 会话ID: {}", executorMac, executorIp, sessionId);
                    
                    executor = new Executor();
                    // 设置MAC地址（必需）
                    executor.setMacAddress(executorMac);
                    
                    // 设置IP地址（如果提供）
                    if (executorIp != null && !executorIp.trim().isEmpty()) {
                        executor.setIpAddress(executorIp);
                    } else {
                        // 如果没有提供IP，尝试从MAC地址表中获取
                        ExecutorMacAddress macAddress = executorMacAddressService.getByMacAddress(executorMac);
                        if (macAddress != null && macAddress.getIpAddress() != null) {
                            executor.setIpAddress(macAddress.getIpAddress());
                        }
                    }
                    
                    // 设置执行机名称，如果注册消息中有则使用，否则使用默认值
                    String executorName = registerMsg.getExecutorName();
                    if (executorName == null || executorName.trim().isEmpty()) {
                        executorName = "执行机-" + executorMac;
                    }
                    executor.setName(executorName);
                    
                    // 处理地域信息：判断是否存在，不存在则创建，并绑定到执行机
                    Long regionId = regionService.findOrCreateRegionHierarchy(
                        registerMsg.getRegionName(),
                        registerMsg.getCountryName(),
                        registerMsg.getProvinceName(),
                        registerMsg.getCityName(),
                        "通过执行机注册自动创建"
                    );
                    
                    if (regionId != null) {
                        executor.setRegionId(regionId);
                        log.info("执行机地域已绑定 - MAC地址: {}, 地域ID: {}, 地域信息: 地域={}, 国家={}, 省份={}, 城市={}, 会话ID: {}", 
                                executorMac, regionId, 
                                registerMsg.getRegionName(), registerMsg.getCountryName(), 
                                registerMsg.getProvinceName(), registerMsg.getCityName(), sessionId);
                    } else {
                        // 如果没有提供地域信息，设置默认地域ID为1（中国片区）
                        executor.setRegionId(1L);
                        log.info("执行机未提供地域信息，使用默认地域ID=1 - MAC地址: {}, 会话ID: {}", executorMac, sessionId);
                    }
                    
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
                    
                    log.info("执行机已自动创建 - MAC地址: {}, 执行机IP: {}, 执行机名称: {}, 执行机ID: {}, 会话ID: {}", 
                            executorMac, executor.getIpAddress(), executorName, executor.getId(), sessionId);
                }
            } else {
                // 执行机已存在，更新状态为在线
                executor.setStatus(1);
                
                // 确保MAC地址已设置
                if (executor.getMacAddress() == null || !executorMac.equals(executor.getMacAddress())) {
                    executor.setMacAddress(executorMac);
                }
                
                // 如果提供了IP地址且与现有不同，则更新IP地址
                if (executorIp != null && !executorIp.trim().isEmpty() && 
                    (executor.getIpAddress() == null || !executorIp.equals(executor.getIpAddress()))) {
                    executor.setIpAddress(executorIp);
                }
                
                executorService.updateById(executor);
                
                log.info("执行机已存在，更新状态为在线 - MAC地址: {}, 执行机IP: {}, 执行机名称: {}, 会话ID: {}", 
                        executorMac, executor.getIpAddress(), executor.getName(), sessionId);
            }
            
            // 确保executor不为null（理论上不应该为null，但为了代码健壮性添加检查）
            if (executor == null) {
                log.error("执行机处理异常，executor为null - MAC地址: {}, 执行机IP: {}, 会话ID: {}", executorMac, executorIp, sessionId);
                sendErrorResponse(session, "处理执行机信息异常");
                return;
            }
            
            // 注册到MAC地址表
            ExecutorMacAddress macAddress = executorMacAddressService.registerOrUpdateMacAddress(executorMac, executor.getIpAddress());
            if (macAddress != null) {
                // 更新executor的macAddressId
                executor.setMacAddressId(macAddress.getId());
                executorService.updateById(executor);
            }
            
            // 使用MAC地址注册执行机连接
            executorWebSocketService.registerExecutor(executorMac, sessionId);
            
            // 发送注册成功响应
            WebSocketMessage response = new WebSocketMessage();
            response.setType("REGISTER_RESPONSE");
            response.setTimestamp(System.currentTimeMillis());
            response.setMessageId(wsMessage.getMessageId());
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("status", "SUCCESS");
            data.put("message", "执行机注册成功");
            data.put("executorMac", executorMac);
            data.put("executorIp", executor.getIpAddress());
            data.put("executorId", executor.getId());
            data.put("executorName", executor.getName());
            response.setData(data);
            
            sendMessage(session, response);
            
            log.info("执行机注册成功 - MAC地址: {}, 执行机IP: {}, 执行机名称: {}, 执行机ID: {}, 会话ID: {}", 
                    executorMac, executor.getIpAddress(), executor.getName(), executor.getId(), sessionId);
            
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
        String sessionId = session.getId();
        
        // 如果已存在该会话的心跳任务，先取消
        ScheduledFuture<?> existingTask = heartbeatTasks.get(sessionId);
        if (existingTask != null) {
            existingTask.cancel(false);
        }
        
        // 创建新的心跳检查任务并保存引用
        ScheduledFuture<?> heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(() -> {
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
        
        // 保存任务引用，以便后续取消
        heartbeatTasks.put(sessionId, heartbeatTask);
    }
    
    /**
     * 组件销毁时关闭线程池
     */
    @PreDestroy
    public void destroy() {
        log.info("正在关闭ExecutorWebSocketHandler，清理所有心跳检查任务...");
        
        // 取消所有心跳任务
        for (ScheduledFuture<?> task : heartbeatTasks.values()) {
            if (task != null) {
                task.cancel(false);
            }
        }
        heartbeatTasks.clear();
        
        // 关闭线程池
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdown();
            try {
                if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    heartbeatScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                heartbeatScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("心跳检查线程池已关闭");
        }
    }
}


import com.alibaba.fastjson.JSON;
import com.datacollect.dto.ExecutorRegisterMessage;
import com.datacollect.dto.WebSocketMessage;
import com.datacollect.entity.Executor;
import com.datacollect.entity.ExecutorMacAddress;
import com.datacollect.service.ExecutorService;
import com.datacollect.service.ExecutorMacAddressService;
import com.datacollect.service.ExecutorWebSocketService;
import com.datacollect.service.RegionService;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;

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
    
    @Autowired
    private ExecutorMacAddressService executorMacAddressService;
    
    @Autowired
    private RegionService regionService;
    
    /**
     * 心跳检查定时任务
     */
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(1);
    
    /**
     * 存储会话的最后活跃时间
     */
    private final java.util.Map<String, Long> sessionLastActiveTime = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * 存储每个会话的心跳检查定时任务，用于取消任务
     */
    private final java.util.Map<String, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    
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
        
        // 取消心跳检查定时任务
        ScheduledFuture<?> heartbeatTask = heartbeatTasks.remove(sessionId);
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            log.debug("已取消心跳检查定时任务 - 会话ID: {}", sessionId);
        }
        
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
            
            // MAC地址是必需的
            if (registerMsg == null || registerMsg.getExecutorMac() == null || registerMsg.getExecutorMac().trim().isEmpty()) {
                log.warn("注册消息缺少MAC地址 - 会话ID: {}", sessionId);
                sendErrorResponse(session, "注册消息缺少MAC地址，MAC地址是唯一标识");
                return;
            }
            
            String executorIp = registerMsg.getExecutorIp();
            String executorMac = registerMsg.getExecutorMac().trim();
            
            log.info("收到执行机注册消息 - 执行机IP: {}, MAC地址: {}, 会话ID: {}", 
                    executorIp != null ? executorIp : "未提供", executorMac, sessionId);
            
            // 使用MAC地址查找执行机（优先通过mac_address字段查找）
            QueryWrapper<Executor> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("mac_address", executorMac);
            Executor executor = executorService.getOne(queryWrapper);
            
            // 如果通过mac_address字段没找到，尝试通过ExecutorMacAddress表查找
            if (executor == null) {
                ExecutorMacAddress macAddress = executorMacAddressService.getByMacAddress(executorMac);
                if (macAddress != null && macAddress.getIpAddress() != null) {
                    // 通过IP地址查找执行机
                    QueryWrapper<Executor> ipQueryWrapper = new QueryWrapper<>();
                    ipQueryWrapper.eq("ip_address", macAddress.getIpAddress());
                    executor = executorService.getOne(ipQueryWrapper);
                }
            }
            
            if (executor == null) {
                // 执行机不存在，先检查IP地址是否已存在
                boolean ipExists = false;
                if (executorIp != null && !executorIp.trim().isEmpty()) {
                    QueryWrapper<Executor> ipQueryWrapper = new QueryWrapper<>();
                    ipQueryWrapper.eq("ip_address", executorIp);
                    Executor existingExecutor = executorService.getOne(ipQueryWrapper);
                    
                    if (existingExecutor != null) {
                        // IP地址已存在，更新mac_address_id
                        log.info("IP地址已存在，更新mac_address_id - IP地址: {}, MAC地址: {}, 执行机ID: {}, 会话ID: {}", 
                                executorIp, executorMac, existingExecutor.getId(), sessionId);
                        
                        executor = existingExecutor;
                        ipExists = true;
                        
                        // 更新MAC地址
                        executor.setMacAddress(executorMac);
                        
                        // 更新状态为在线
                        executor.setStatus(registerMsg.getStatus() != null ? registerMsg.getStatus() : 1);
                        
                        executorService.updateById(executor);
                        
                        log.info("已更新执行机的mac_address - IP地址: {}, MAC地址: {}, 执行机ID: {}, 会话ID: {}", 
                                executorIp, executorMac, executor.getId(), sessionId);
                    }
                }
                
                if (!ipExists) {
                    // IP地址不存在，自动创建新执行机
                    log.info("执行机不存在，自动创建 - MAC地址: {}, 执行机IP: {}, 会话ID: {}", executorMac, executorIp, sessionId);
                    
                    executor = new Executor();
                    // 设置MAC地址（必需）
                    executor.setMacAddress(executorMac);
                    
                    // 设置IP地址（如果提供）
                    if (executorIp != null && !executorIp.trim().isEmpty()) {
                        executor.setIpAddress(executorIp);
                    } else {
                        // 如果没有提供IP，尝试从MAC地址表中获取
                        ExecutorMacAddress macAddress = executorMacAddressService.getByMacAddress(executorMac);
                        if (macAddress != null && macAddress.getIpAddress() != null) {
                            executor.setIpAddress(macAddress.getIpAddress());
                        }
                    }
                    
                    // 设置执行机名称，如果注册消息中有则使用，否则使用默认值
                    String executorName = registerMsg.getExecutorName();
                    if (executorName == null || executorName.trim().isEmpty()) {
                        executorName = "执行机-" + executorMac;
                    }
                    executor.setName(executorName);
                    
                    // 处理地域信息：判断是否存在，不存在则创建，并绑定到执行机
                    Long regionId = regionService.findOrCreateRegionHierarchy(
                        registerMsg.getRegionName(),
                        registerMsg.getCountryName(),
                        registerMsg.getProvinceName(),
                        registerMsg.getCityName(),
                        "通过执行机注册自动创建"
                    );
                    
                    if (regionId != null) {
                        executor.setRegionId(regionId);
                        log.info("执行机地域已绑定 - MAC地址: {}, 地域ID: {}, 地域信息: 地域={}, 国家={}, 省份={}, 城市={}, 会话ID: {}", 
                                executorMac, regionId, 
                                registerMsg.getRegionName(), registerMsg.getCountryName(), 
                                registerMsg.getProvinceName(), registerMsg.getCityName(), sessionId);
                    } else {
                        // 如果没有提供地域信息，设置默认地域ID为1（中国片区）
                        executor.setRegionId(1L);
                        log.info("执行机未提供地域信息，使用默认地域ID=1 - MAC地址: {}, 会话ID: {}", executorMac, sessionId);
                    }
                    
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
                    
                    log.info("执行机已自动创建 - MAC地址: {}, 执行机IP: {}, 执行机名称: {}, 执行机ID: {}, 会话ID: {}", 
                            executorMac, executor.getIpAddress(), executorName, executor.getId(), sessionId);
                }
            } else {
                // 执行机已存在，更新状态为在线
                executor.setStatus(1);
                
                // 确保MAC地址已设置
                if (executor.getMacAddress() == null || !executorMac.equals(executor.getMacAddress())) {
                    executor.setMacAddress(executorMac);
                }
                
                // 如果提供了IP地址且与现有不同，则更新IP地址
                if (executorIp != null && !executorIp.trim().isEmpty() && 
                    (executor.getIpAddress() == null || !executorIp.equals(executor.getIpAddress()))) {
                    executor.setIpAddress(executorIp);
                }
                
                executorService.updateById(executor);
                
                log.info("执行机已存在，更新状态为在线 - MAC地址: {}, 执行机IP: {}, 执行机名称: {}, 会话ID: {}", 
                        executorMac, executor.getIpAddress(), executor.getName(), sessionId);
            }
            
            // 确保executor不为null（理论上不应该为null，但为了代码健壮性添加检查）
            if (executor == null) {
                log.error("执行机处理异常，executor为null - MAC地址: {}, 执行机IP: {}, 会话ID: {}", executorMac, executorIp, sessionId);
                sendErrorResponse(session, "处理执行机信息异常");
                return;
            }
            
            // 注册到MAC地址表
            ExecutorMacAddress macAddress = executorMacAddressService.registerOrUpdateMacAddress(executorMac, executor.getIpAddress());
            if (macAddress != null) {
                // 更新executor的macAddressId
                executor.setMacAddressId(macAddress.getId());
                executorService.updateById(executor);
            }
            
            // 使用MAC地址注册执行机连接
            executorWebSocketService.registerExecutor(executorMac, sessionId);
            
            // 发送注册成功响应
            WebSocketMessage response = new WebSocketMessage();
            response.setType("REGISTER_RESPONSE");
            response.setTimestamp(System.currentTimeMillis());
            response.setMessageId(wsMessage.getMessageId());
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("status", "SUCCESS");
            data.put("message", "执行机注册成功");
            data.put("executorMac", executorMac);
            data.put("executorIp", executor.getIpAddress());
            data.put("executorId", executor.getId());
            data.put("executorName", executor.getName());
            response.setData(data);
            
            sendMessage(session, response);
            
            log.info("执行机注册成功 - MAC地址: {}, 执行机IP: {}, 执行机名称: {}, 执行机ID: {}, 会话ID: {}", 
                    executorMac, executor.getIpAddress(), executor.getName(), executor.getId(), sessionId);
            
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
        String sessionId = session.getId();
        
        // 如果已存在该会话的心跳任务，先取消
        ScheduledFuture<?> existingTask = heartbeatTasks.get(sessionId);
        if (existingTask != null) {
            existingTask.cancel(false);
        }
        
        // 创建新的心跳检查任务并保存引用
        ScheduledFuture<?> heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(() -> {
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
        
        // 保存任务引用，以便后续取消
        heartbeatTasks.put(sessionId, heartbeatTask);
    }
    
    /**
     * 组件销毁时关闭线程池
     */
    @PreDestroy
    public void destroy() {
        log.info("正在关闭ExecutorWebSocketHandler，清理所有心跳检查任务...");
        
        // 取消所有心跳任务
        for (ScheduledFuture<?> task : heartbeatTasks.values()) {
            if (task != null) {
                task.cancel(false);
            }
        }
        heartbeatTasks.clear();
        
        // 关闭线程池
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdown();
            try {
                if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    heartbeatScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                heartbeatScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("心跳检查线程池已关闭");
        }
    }
}






