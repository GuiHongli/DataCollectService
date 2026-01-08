package com.datacollect.controller;

import com.datacollect.common.Result;
import com.datacollect.service.UeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * UE状态控制器
 * 用于执行机更新UE使用状态
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/ue-status")
public class UeStatusController {

    @Autowired
    private UeService ueService;

    /**
     * 标记UE为使用中
     * 
     * @param request 包含UE ID列表的请求
     * @return 操作结果
     */
    @PostMapping("/mark-in-use")
    public Result<Map<String, Object>> markUesInUse(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> ueIds = (List<Integer>) request.get("ueIds");
            
            if (ueIds == null || ueIds.isEmpty()) {
                return Result.error("UE ID列表不能为空");
            }
            
            boolean success = ueService.markUesInUse(ueIds);
            if (success) {
                log.info("UE已标记为使用中 - UE IDs: {}", ueIds);
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("ueIds", ueIds);
                data.put("status", "IN_USE");
                return Result.success(data);
            } else {
                return Result.error("标记UE为使用中失败");
            }
            
        } catch (Exception e) {
            log.error("标记UE为使用中失败 - 错误: {}", e.getMessage(), e);
            return Result.error("标记UE为使用中失败: " + e.getMessage());
        }
    }

    /**
     * 标记UE为可用（未使用）
     * 
     * @param request 包含UE ID列表的请求
     * @return 操作结果
     */
    @PostMapping("/mark-available")
    public Result<Map<String, Object>> markUesAvailable(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> ueIds = (List<Integer>) request.get("ueIds");
            
            if (ueIds == null || ueIds.isEmpty()) {
                return Result.error("UE ID列表不能为空");
            }
            
            boolean success = ueService.markUesAvailable(ueIds);
            if (success) {
                log.info("UE已标记为可用 - UE IDs: {}", ueIds);
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("ueIds", ueIds);
                data.put("status", "AVAILABLE");
                return Result.success(data);
            } else {
                return Result.error("标记UE为可用失败");
            }
            
        } catch (Exception e) {
            log.error("标记UE为可用失败 - 错误: {}", e.getMessage(), e);
            return Result.error("标记UE为可用失败: " + e.getMessage());
        }
    }
}

