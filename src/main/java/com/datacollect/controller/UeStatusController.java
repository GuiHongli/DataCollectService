package com.datacollect.controller;

import com.datacollect.common.Result;
import com.datacollect.service.UeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * UE状态控制器
 * 用于执行机更新UE使用状态
 * 
 * @author system
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/ue-status")
public class UeStatusController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UeStatusController.class);

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
                LOGGER.info("UE已mark为in use - UE IDs: {}", ueIds);
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("ueIds", ueIds);
                data.put("status", "IN_USE");
                return Result.success(data);
            } else {
                return Result.error("标记UE为使用中失败");
            }
            
        } catch (Exception e) {
            LOGGER.error("markUE为in usefailed - error: {}", e.getMessage(), e);
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
                LOGGER.info("UE已mark为available - UE IDs: {}", ueIds);
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("ueIds", ueIds);
                data.put("status", "AVAILABLE");
                return Result.success(data);
            } else {
                return Result.error("标记UE为可用失败");
            }
            
        } catch (Exception e) {
            LOGGER.error("markUE为availablefailed - error: {}", e.getMessage(), e);
            return Result.error("标记UE为可用失败: " + e.getMessage());
        }
    }
}

