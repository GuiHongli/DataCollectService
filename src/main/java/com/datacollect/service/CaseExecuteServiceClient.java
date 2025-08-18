package com.datacollect.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * CaseExecuteService客户端
 * 用于调用CaseExecuteService的相关接口
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class CaseExecuteServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * 取消任务执行
     * 
     * @param executorIp 执行机IP
     * @param taskId 任务ID
     * @return 是否取消成功
     */
    public boolean cancelTaskExecution(String executorIp, String taskId) {
        try {
            String url = String.format("http://%s:8081/api/test-case-execution/cancel/%s", executorIp, taskId);
            log.info("调用CaseExecuteService取消任务 - URL: {}, 任务ID: {}", url, taskId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                Integer code = (Integer) result.get("code");
                
                if (code != null && code == 200) {
                    log.info("CaseExecuteService取消任务成功 - 任务ID: {}, 执行机IP: {}", taskId, executorIp);
                    return true;
                } else {
                    String message = (String) result.get("message");
                    log.error("CaseExecuteService取消任务返回错误 - 任务ID: {}, 错误信息: {}", taskId, message);
                    return false;
                }
            } else {
                log.error("CaseExecuteService取消任务调用失败 - 任务ID: {}, HTTP状态: {}", taskId, response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            log.error("CaseExecuteService取消任务网络调用异常 - 任务ID: {}, 执行机IP: {}, 错误: {}", 
                    taskId, executorIp, e.getMessage(), e);
            return false;
        }
    }
}
