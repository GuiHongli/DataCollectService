package com.datacollect.controller;

import com.datacollect.dto.AppCheckRequest;
import com.datacollect.dto.AppCheckResponse;
import com.datacollect.dto.GetDailyRankRequest;
import com.datacollect.dto.GetDailyRankResponse;
import com.datacollect.dto.GetVersionHistoryRequest;
import com.datacollect.dto.GetVersionHistoryResponse;
import com.datacollect.dto.UpdateProbedStatusResponse;
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
@RequestMapping("/external")
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
            log.info("Received request to check if app is new - parameters: {}", appCheckRequests);
            
            AppCheckResponse result = externalApiService.checkAppIsNew(appCheckRequests);
            
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to check if app is new", e);
            
            response.put("code", 500);
            response.put("message", "Failed to check if app is new: " + e.getMessage());
            response.put("data", null);
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 更新探测状态
     * 
     * @param appNames 应用名称列表
     * @return 更新探测状态响应
     */
    @PostMapping("/update-probed-status")
    public ResponseEntity<Map<String, Object>> updateProbedStatus(@RequestBody List<String> appNames) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Received request to update probed status - parameters: {}", appNames);
            
            UpdateProbedStatusResponse result = externalApiService.updateProbedStatus(appNames);
            
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to update probed status", e);
            
            response.put("code", 500);
            response.put("message", "Failed to update probed status: " + e.getMessage());
            response.put("data", null);
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取每日排名
     * 
     * @param request 获取每日排名请求
     * @return 获取每日排名响应
     */
    @PostMapping("/apps/get-daily-rank")
    public ResponseEntity<Map<String, Object>> getDailyRank(@RequestBody GetDailyRankRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Received request to get daily rank - parameters: {}", request);
            
            GetDailyRankResponse result = externalApiService.getDailyRank(request);
            
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get daily rank", e);
            
            response.put("code", 500);
            response.put("message", "Failed to get daily rank: " + e.getMessage());
            response.put("data", null);
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取版本历史
     * 
     * @param request 获取版本历史请求
     * @return 获取版本历史响应
     */
    @PostMapping("/apps/get_version_history")
    public ResponseEntity<Map<String, Object>> getVersionHistory(@RequestBody GetVersionHistoryRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Received request to get version history - parameters: {}", request);
            
            GetVersionHistoryResponse result = externalApiService.getVersionHistory(request);
            
            response.put("message", "success");
            response.put("data", result.getData());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get version history", e);
            
            response.put("message", "Failed to get version history: " + e.getMessage());
            response.put("data", null);
            
            return ResponseEntity.status(500).body(response);
        }
    }
}

