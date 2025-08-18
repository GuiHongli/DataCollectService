package com.datacollect.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 用例执行结果DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
public class TestCaseExecutionResult {
    
    /**
     * 任务ID
     */
    @NotBlank(message = "任务ID不能为空")
    private String taskId;
    
    /**
     * 用例ID
     */
    @NotNull(message = "用例ID不能为空")
    private Long testCaseId;
    
    /**
     * 轮次
     */
    @NotNull(message = "轮次不能为空")
    private Integer round;
    
    /**
     * 执行状态 (SUCCESS/FAILED/BLOCKED)
     */
    @NotBlank(message = "执行状态不能为空")
    private String status;
    
    /**
     * 执行结果描述
     */
    private String result;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long executionTime;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 失败原因（详细分析）
     */
    private String failureReason;
    
    /**
     * 日志文件路径或HTTP链接
     */
    private String logFilePath;
    
    /**
     * 执行机IP
     */
    @NotBlank(message = "执行机IP不能为空")
    private String executorIp;
    
    /**
     * 用例集ID
     */
    @NotNull(message = "用例集ID不能为空")
    private Long testCaseSetId;
}
