package com.datacollect.controller;

import com.datacollect.dto.AppCheckRequest;
import com.datacollect.dto.AppCheckResponse;
import com.datacollect.service.ExternalApiService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * 外部接口调用控制器
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/external")
@CrossOrigin(origins = "*")
public class ExternalApiController {
    
    @Autowired
    private ExternalApiService externalApiService;
    
    /**
     * 检查应用是否为新应用
     * 
     * @param appCheckRequests 应用检查请求列表
     * @return 应用检查响应
     */
    @PostMapping("/check-app-is-new")
    public ResponseEntity<Map<String, Object>> checkAppIsNew(@RequestBody List<AppCheckRequest> appCheckRequests) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("接收到检查应用是否为新应用的请求 - 参数: {}", appCheckRequests);
            
            AppCheckResponse result = externalApiService.checkAppIsNew(appCheckRequests);
            
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("检查应用是否为新应用失败", e);
            
            response.put("code", 500);
            response.put("message", "检查应用是否为新应用失败: " + e.getMessage());
            response.put("data", null);
            
            return ResponseEntity.status(500).body(response);
        }
    }
}

