package com.datacollect.controller;

import com.datacollect.common.Result;
import com.datacollect.dto.TestCaseExecutionResult;
import com.datacollect.service.TestCaseExecutionResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 用例执行结果控制器
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/test-result")
@Validated
public class TestCaseExecutionResultController {

    @Autowired
    private TestCaseExecutionResultService testCaseExecutionResultService;

    /**
     * 接收用例执行结果
     * 
     * @param result 用例执行结果
     * @return 接收结果
     */
    @PostMapping("/report")
    public Result<Map<String, Object>> reportTestCaseResult(@Valid @RequestBody TestCaseExecutionResult result) {
        log.info("接收到用例执行结果 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}", 
                result.getTaskId(), result.getTestCaseId(), result.getRound(), result.getStatus());
        
        try {
            // 保存执行结果
            boolean success = testCaseExecutionResultService.saveTestCaseExecutionResult(result);
            
            if (success) {
                Map<String, Object> data = new HashMap<>();
                data.put("taskId", result.getTaskId());
                data.put("testCaseId", result.getTestCaseId());
                data.put("round", result.getRound());
                data.put("status", result.getStatus());
                data.put("message", "用例执行结果接收成功");
                data.put("timestamp", System.currentTimeMillis());
                
                log.info("用例执行结果接收成功 - 任务ID: {}, 用例ID: {}, 轮次: {}", 
                        result.getTaskId(), result.getTestCaseId(), result.getRound());
                
                return Result.success(data);
            } else {
                log.error("用例执行结果保存失败 - 任务ID: {}, 用例ID: {}, 轮次: {}", 
                        result.getTaskId(), result.getTestCaseId(), result.getRound());
                return Result.error("用例执行结果保存失败");
            }
            
        } catch (Exception e) {
            log.error("接收用例执行结果异常 - 任务ID: {}, 用例ID: {}, 轮次: {}, 错误: {}", 
                    result.getTaskId(), result.getTestCaseId(), result.getRound(), e.getMessage(), e);
            return Result.error("接收用例执行结果失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据任务ID查询执行结果列表
     * 
     * @param taskId 任务ID
     * @return 执行结果列表
     */
    @GetMapping("/task/{taskId}")
    public Result<java.util.List<com.datacollect.entity.TestCaseExecutionResult>> getResultsByTaskId(@PathVariable String taskId) {
        log.info("查询任务执行结果 - 任务ID: {}", taskId);
        
        try {
            java.util.List<com.datacollect.entity.TestCaseExecutionResult> results = testCaseExecutionResultService.getByTaskId(taskId);
            
            log.info("查询到任务执行结果数量: {} - 任务ID: {}", results.size(), taskId);
            return Result.success(results);
            
        } catch (Exception e) {
            log.error("查询任务执行结果失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
            return Result.error("查询任务执行结果失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据用例ID查询执行结果列表
     * 
     * @param testCaseId 用例ID
     * @return 执行结果列表
     */
    @GetMapping("/testcase/{testCaseId}")
    public Result<java.util.List<com.datacollect.entity.TestCaseExecutionResult>> getResultsByTestCaseId(@PathVariable Long testCaseId) {
        log.info("查询用例执行结果 - 用例ID: {}", testCaseId);
        
        try {
            java.util.List<com.datacollect.entity.TestCaseExecutionResult> results = testCaseExecutionResultService.getByTestCaseId(testCaseId);
            
            log.info("查询到用例执行结果数量: {} - 用例ID: {}", results.size(), testCaseId);
            return Result.success(results);
            
        } catch (Exception e) {
            log.error("查询用例执行结果失败 - 用例ID: {}, 错误: {}", testCaseId, e.getMessage(), e);
            return Result.error("查询用例执行结果失败: " + e.getMessage());
        }
    }
}
