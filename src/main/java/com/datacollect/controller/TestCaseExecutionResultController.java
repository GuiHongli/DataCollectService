package com.datacollect.controller;

import com.datacollect.common.Result;
import com.datacollect.dto.TestCaseExecutionResult;
import com.datacollect.dto.TestCaseLogRequest;
import com.datacollect.service.TestCaseExecutionResultService;
import org.springframework.web.multipart.MultipartFile;
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
        log.info("Received test case execution result - task ID: {}, test case ID: {}, round: {}, status: {}", 
                result.getTaskId(), result.getTestCaseId(), result.getRound(), result.getStatus());
        
        // 记录日志文件信息
        if (result.getLogFilePath() != null && !result.getLogFilePath().trim().isEmpty()) {
            log.info("Test case execution result contains log file - task ID: {}, test case ID: {}, round: {}, log file: {}", 
                    result.getTaskId(), result.getTestCaseId(), result.getRound(), result.getLogFilePath());
        }
        
        try {
            // 保存执行结果
            boolean success = testCaseExecutionResultService.saveTestCaseExecutionResult(result);
            
            if (success) {
                Map<String, Object> data = new HashMap<>();
                data.put("taskId", result.getTaskId());
                data.put("testCaseId", result.getTestCaseId());
                data.put("round", result.getRound());
                data.put("status", result.getStatus());
                data.put("logFilePath", result.getLogFilePath());
                data.put("message", "用例执行结果接收成功");
                data.put("timestamp", System.currentTimeMillis());
                
                log.info("Test case execution result received successfully - task ID: {}, test case ID: {}, round: {}, log file: {}", 
                        result.getTaskId(), result.getTestCaseId(), result.getRound(), result.getLogFilePath());
                
                return Result.success(data);
            } else {
                log.error("Failed to save test case execution result - task ID: {}, test case ID: {}, round: {}", 
                        result.getTaskId(), result.getTestCaseId(), result.getRound());
                return Result.error("用例执行结果保存失败");
            }
            
        } catch (Exception e) {
            log.error("Exception receiving test case execution result - task ID: {}, test case ID: {}, round: {}, error: {}", 
                    result.getTaskId(), result.getTestCaseId(), result.getRound(), e.getMessage(), e);
            return Result.error("接收用例执行结果失败: " + e.getMessage());
        }
    }
    
   
    /**
     * 接收用例执行日志文件
     * 
     * @param taskId 任务ID
     * @param testCaseId 用例ID
     * @param round 轮次
     * @param logFile 日志文件
     * @return 接收结果
     */
    @PostMapping("/log/upload")
    public Result<Map<String, Object>> uploadTestCaseLog(@RequestParam("taskId") String taskId,
                                                        @RequestParam("testCaseId") Long testCaseId,
                                                        @RequestParam("round") Integer round,
                                                        @RequestParam("logFile") MultipartFile logFile) {
        log.info("Received test case execution log file - task ID: {}, test case ID: {}, round: {}, filename: {}, file size: {} bytes", 
                taskId, testCaseId, round, logFile.getOriginalFilename(), logFile.getSize());
        
        try {
            // 创建日志文件存储目录并保存文件
            java.nio.file.Path filePath = createLogFileAndSave(taskId, testCaseId, round, logFile);
            
            // 构建响应数据
            Map<String, Object> data = buildUploadResponseData(taskId, testCaseId, round, logFile, filePath);
            
            log.info("Test case execution log file uploaded successfully - task ID: {}, test case ID: {}, round: {}, file path: {}", 
                    taskId, testCaseId, round, filePath);
            
            return Result.success(data);
            
        } catch (Exception e) {
            log.error("Exception receiving test case execution log file - task ID: {}, test case ID: {}, round: {}, error: {}", 
                    taskId, testCaseId, round, e.getMessage(), e);
            return Result.error("接收用例执行日志文件失败: " + e.getMessage());
        }
    }

    /**
     * 创建日志文件并保存
     */
    private java.nio.file.Path createLogFileAndSave(String taskId, Long testCaseId, Integer round, MultipartFile logFile) throws Exception {
        // 创建日志文件存储目录
        String logDir = "logs/testcase/" + taskId + "/" + testCaseId + "/" + round;
        java.nio.file.Path logPath = java.nio.file.Paths.get(logDir);
        java.nio.file.Files.createDirectories(logPath);
        
        // 保存日志文件
        String fileName = getLogFileName(logFile, testCaseId, round);
        java.nio.file.Path filePath = logPath.resolve(fileName);
        logFile.transferTo(filePath.toFile());
        
        return filePath;
    }

    /**
     * 获取日志文件名
     */
    private String getLogFileName(MultipartFile logFile, Long testCaseId, Integer round) {
        String fileName = logFile.getOriginalFilename();
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "testcase_" + testCaseId + "_" + round + ".log";
        }
        return fileName;
    }

    /**
     * 构建上传响应数据
     */
    private Map<String, Object> buildUploadResponseData(String taskId, Long testCaseId, Integer round, 
                                                      MultipartFile logFile, java.nio.file.Path filePath) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("testCaseId", testCaseId);
        data.put("round", round);
        data.put("logFileName", filePath.getFileName().toString());
        data.put("fileSize", logFile.getSize());
        data.put("filePath", filePath.toString());
        data.put("message", "用例执行日志文件上传成功");
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }

    /**
     * 根据任务ID查询执行结果列表
     * 
     * @param taskId 任务ID
     * @return 执行结果列表
     */
    @GetMapping("/task/{taskId}")
    public Result<java.util.List<com.datacollect.entity.TestCaseExecutionResult>> getResultsByTaskId(@PathVariable String taskId) {
        log.info("Query task execution results - task ID: {}", taskId);
        
        try {
            java.util.List<com.datacollect.entity.TestCaseExecutionResult> results = testCaseExecutionResultService.getByTaskId(taskId);
            
            log.info("Found task execution result count: {} - task ID: {}", results.size(), taskId);
            return Result.success(results);
            
        } catch (Exception e) {
            log.error("Failed to query task execution results - task ID: {}, error: {}", taskId, e.getMessage(), e);
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
        log.info("Query test case execution results - test case ID: {}", testCaseId);
        
        try {
            java.util.List<com.datacollect.entity.TestCaseExecutionResult> results = testCaseExecutionResultService.getByTestCaseId(testCaseId);
            
            log.info("Found test case execution result count: {} - test case ID: {}", results.size(), testCaseId);
            return Result.success(results);
            
        } catch (Exception e) {
            log.error("Failed to query test case execution results - test case ID: {}, error: {}", testCaseId, e.getMessage(), e);
            return Result.error("查询用例执行结果失败: " + e.getMessage());
        }
    }
}
