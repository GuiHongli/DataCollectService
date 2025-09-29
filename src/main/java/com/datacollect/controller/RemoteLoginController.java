package com.datacollect.controller;

import com.datacollect.common.Result;
import com.datacollect.entity.RemoteLoginLog;
import com.datacollect.service.RemoteLoginLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/remote-login")
@Validated
public class RemoteLoginController {

    @Autowired
    private RemoteLoginLogService remoteLoginLogService;

    /**
     * 记录远程登录操作
     */
    @PostMapping("/log")
    public Result<Map<String, Object>> logRemoteLogin(@Valid @RequestBody RemoteLoginLogRequest request) {
        try {
            log.info("Recording remote login operation - Executor IP: {}, OS: {}, Connection Type: {}", 
                    request.getExecutorIp(), request.getOsType(), request.getConnectionType());
            
            // 创建登录日志记录
            RemoteLoginLog loginLog = new RemoteLoginLog();
            loginLog.setExecutorIp(request.getExecutorIp());
            loginLog.setLogicEnvironmentName(request.getLogicEnvironmentName());
            loginLog.setOsType(request.getOsType());
            loginLog.setConnectionType(request.getConnectionType());
            loginLog.setUsername(request.getUsername());
            loginLog.setPort(request.getPort());
            loginLog.setOperationNote(request.getOperationNote());
            loginLog.setConnectTime(LocalDateTime.now());
            loginLog.setStatus("CONNECTING");
            
            // 保存记录
            remoteLoginLogService.save(loginLog);
            
            // 生成连接信息
            Map<String, Object> connectionInfo = generateConnectionInfo(request);
            
            log.info("Remote login log recorded successfully - Log ID: {}", loginLog.getId());
            
            return Result.success(connectionInfo);
            
        } catch (Exception e) {
            log.error("Failed to record remote login operation", e);
            return Result.error("记录远程登录操作失败: " + e.getMessage());
        }
    }

    /**
     * 更新登录状态
     */
    @PutMapping("/{id}/status")
    public Result<Boolean> updateLoginStatus(
            @PathVariable @NotNull Long id,
            @RequestParam String status) {
        try {
            RemoteLoginLog loginLog = remoteLoginLogService.getById(id);
            if (loginLog == null) {
                return Result.error("登录记录不存在");
            }
            
            loginLog.setStatus(status);
            if ("DISCONNECTED".equals(status)) {
                loginLog.setDisconnectTime(LocalDateTime.now());
            }
            
            remoteLoginLogService.updateById(loginLog);
            
            log.info("Updated remote login status - Log ID: {}, Status: {}", id, status);
            return Result.success(true);
            
        } catch (Exception e) {
            log.error("Failed to update login status", e);
            return Result.error("更新登录状态失败: " + e.getMessage());
        }
    }

    /**
     * 生成连接信息
     */
    private Map<String, Object> generateConnectionInfo(RemoteLoginLogRequest request) {
        Map<String, Object> connectionInfo = new HashMap<>();
        
        if ("ssh".equals(request.getConnectionType())) {
            connectionInfo.put("command", String.format("ssh %s@%s -p %d", 
                    request.getUsername(), request.getExecutorIp(), request.getPort()));
            connectionInfo.put("type", "ssh");
            connectionInfo.put("description", "SSH连接命令");
        } else if ("rdp".equals(request.getConnectionType())) {
            connectionInfo.put("url", String.format("rdp://%s:%d", 
                    request.getExecutorIp(), request.getPort()));
            connectionInfo.put("type", "rdp");
            connectionInfo.put("description", "RDP连接URL");
        } else if ("vnc".equals(request.getConnectionType())) {
            connectionInfo.put("url", String.format("vnc://%s:%d", 
                    request.getExecutorIp(), request.getPort()));
            connectionInfo.put("type", "vnc");
            connectionInfo.put("description", "VNC连接URL");
        }
        
        connectionInfo.put("executorIp", request.getExecutorIp());
        connectionInfo.put("username", request.getUsername());
        connectionInfo.put("port", request.getPort());
        connectionInfo.put("osType", request.getOsType());
        
        return connectionInfo;
    }

    /**
     * 远程登录请求DTO
     */
    public static class RemoteLoginLogRequest {
        private String executorIp;
        private String logicEnvironmentName;
        private String osType;
        private String connectionType;
        private String username;
        private Integer port;
        private String operationNote;

        // Getters and Setters
        public String getExecutorIp() { return executorIp; }
        public void setExecutorIp(String executorIp) { this.executorIp = executorIp; }

        public String getLogicEnvironmentName() { return logicEnvironmentName; }
        public void setLogicEnvironmentName(String logicEnvironmentName) { this.logicEnvironmentName = logicEnvironmentName; }

        public String getOsType() { return osType; }
        public void setOsType(String osType) { this.osType = osType; }

        public String getConnectionType() { return connectionType; }
        public void setConnectionType(String connectionType) { this.connectionType = connectionType; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public Integer getPort() { return port; }
        public void setPort(Integer port) { this.port = port; }

        public String getOperationNote() { return operationNote; }
        public void setOperationNote(String operationNote) { this.operationNote = operationNote; }
    }
}
