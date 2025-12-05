package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("test_settings_network_ftp")
public class TestSettingsNetworkFtp {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "服务器地址不能为空")
    @TableField("server_address")
    private String serverAddress;

    @NotBlank(message = "账户不能为空")
    @TableField("account")
    private String account;

    @NotBlank(message = "密码不能为空")
    @TableField("password")
    private String password;

    @TableField("check_md5")
    private Integer checkMd5; // 0: 否, 1: 是

    @TableField("directory")
    private String directory;

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




