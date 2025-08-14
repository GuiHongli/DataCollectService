package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("executor")
public class Executor {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "执行机IP不能为空")
    @TableField("ip_address")
    private String ipAddress;

    @NotBlank(message = "执行机名称不能为空")
    @TableField("name")
    private String name;

    @NotNull(message = "执行机所属地域不能为空")
    @TableField("region_id")
    private Long regionId;

    @TableField("description")
    private String description;

    @TableField("status")
    private Integer status; // 0: 离线, 1: 在线, 2: 故障

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
