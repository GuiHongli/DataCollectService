package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("remote_login_log")
public class RemoteLoginLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "执行机IP不能为空")
    @TableField("executor_ip")
    private String executorIp;

    @TableField("logic_environment_name")
    private String logicEnvironmentName;

    @NotBlank(message = "操作系统类型不能为空")
    @TableField("os_type")
    private String osType; // linux, windows

    @NotBlank(message = "连接方式不能为空")
    @TableField("connection_type")
    private String connectionType; // ssh, rdp, vnc

    @NotBlank(message = "用户名不能为空")
    @TableField("username")
    private String username;

    @TableField("port")
    private Integer port;

    @TableField("operation_note")
    private String operationNote;

    @TableField("connect_time")
    private LocalDateTime connectTime;

    @TableField("disconnect_time")
    private LocalDateTime disconnectTime;

    @TableField("status")
    private String status; // CONNECTING, CONNECTED, DISCONNECTED, FAILED

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
