package com.datacollect.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 用例执行日志请求DTO
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
public class TestCaseLogRequest {
    
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
     * 日志文件名
     */
    @NotBlank(message = "日志文件名不能为空")
    private String logFileName;
    
    /**
     * 日志内容
     */
    @NotBlank(message = "日志内容不能为空")
    private String logContent;
    
    /**
     * 执行机IP
     */
    private String executorIp;
    
    /**
     * 用例集ID
     */
    private Long testCaseSetId;
}
