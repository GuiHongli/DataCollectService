package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("logic_environment")
public class LogicEnvironment {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "逻辑环境名称不能为空")
    @TableField("name")
    private String name;

    @NotNull(message = "执行机ID不能为空")
    @TableField("executor_id")
    private Long executorId;

    @TableField("description")
    private String description;

    @TableField("status")
    private Integer status; // 0: 不可用, 1: 可用

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
