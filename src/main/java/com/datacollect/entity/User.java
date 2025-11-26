package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user")
public class User {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @TableField("username")
    private String username;

    @NotBlank(message = "密码不能为空")
    @TableField("password")
    private String password;

    @TableField("role")
    private String role; // admin-管理员，user-普通用户

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




















