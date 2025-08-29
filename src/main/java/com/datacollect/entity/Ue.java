package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ue")
public class Ue {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "UE ID不能为空")
    @TableField("ue_id")
    private String ueId;

    @NotBlank(message = "UE名称不能为空")
    @TableField("name")
    private String name;

    @NotBlank(message = "UE用途不能为空")
    @TableField("purpose")
    private String purpose;

    @NotNull(message = "网络类型不能为空")
    @TableField("network_type_id")
    private Long networkTypeId;

    @TableField("vendor")
    private String vendor;

    @TableField("port")
    private String port;

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
