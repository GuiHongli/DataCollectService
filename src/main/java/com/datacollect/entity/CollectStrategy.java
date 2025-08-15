package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("collect_strategy")
public class CollectStrategy {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "策略名称不能为空")
    @TableField("name")
    private String name;

    @NotNull(message = "采集次数不能为空")
    @TableField("collect_count")
    private Integer collectCount;

    @NotNull(message = "用例集ID不能为空")
    @TableField("test_case_set_id")
    private Long testCaseSetId;

    @TableField("description")
    private String description;

    @TableField("status")
    private Integer status; // 0: 禁用, 1: 启用

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
