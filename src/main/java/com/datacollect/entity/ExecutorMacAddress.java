package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("executor_mac_address")
public class ExecutorMacAddress {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "MAC地址不能为空")
    @TableField("mac_address")
    private String macAddress;

    @NotBlank(message = "IP地址不能为空")
    @TableField("ip_address")
    private String ipAddress;

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

