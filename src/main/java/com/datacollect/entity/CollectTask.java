package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("collect_task")
public class CollectTask {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "任务名称不能为空")
    @TableField("name")
    private String name;

    @NotNull(message = "采集策略ID不能为空")
    @TableField("strategy_id")
    private Long strategyId;

    @TableField("schedule")
    private String schedule; // 定时表达式

    @TableField("description")
    private String description;

    @TableField("status")
    private Integer status; // 0: 停止, 1: 运行中, 2: 暂停

    @TableField("last_run_time")
    private LocalDateTime lastRunTime;

    @TableField("next_run_time")
    private LocalDateTime nextRunTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_by")
    private String updateBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
