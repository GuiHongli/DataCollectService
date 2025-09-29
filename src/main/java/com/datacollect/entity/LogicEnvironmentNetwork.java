package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("logic_environment_network")
public class LogicEnvironmentNetwork {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotNull(message = "逻辑环境ID不能为空")
    @TableField("logic_environment_id")
    private Long logicEnvironmentId;

    @NotNull(message = "网络类型ID不能为空")
    @TableField("logic_network_id")
    private Long logicNetworkId; // 注意：数据库字段名保持不变，但实际引用network_type表的ID

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
