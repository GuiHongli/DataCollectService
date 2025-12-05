package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("test_settings_device_imsi_mapping")
public class TestSettingsDeviceImsiMapping {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "deviceid不能为空")
    @TableField("device_id")
    private String deviceId;

    @NotBlank(message = "GPSI不能为空")
    @TableField("gpsi")
    private String gpsi;

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




