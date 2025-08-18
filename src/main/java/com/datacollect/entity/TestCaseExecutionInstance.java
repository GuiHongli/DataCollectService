package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用例执行例次实体
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@TableName("test_case_execution_instance")
public class TestCaseExecutionInstance {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 采集任务ID
     */
    private Long collectTaskId;
    
    /**
     * 用例ID
     */
    private Long testCaseId;
    
    /**
     * 轮次
     */
    private Integer round;
    
    /**
     * 逻辑环境ID
     */
    private Long logicEnvironmentId;
    
    /**
     * 执行机IP
     */
    private String executorIp;
    
    /**
     * 执行状态 (PENDING/RUNNING/COMPLETED)
     */
    private String status;
    
    /**
     * 执行结果 (SUCCESS/FAILED/BLOCKED)
     */
    private String result;
    
    /**
     * 失败原因
     */
    private String failureReason;
    
    /**
     * 日志文件路径或HTTP链接
     */
    private String logFilePath;
    
    /**
     * 执行任务ID（CaseExecuteService返回的任务ID）
     */
    private String executionTaskId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
