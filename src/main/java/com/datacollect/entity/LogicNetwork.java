package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("logic_network")
public class LogicNetwork {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "逻辑组网名称不能为空")
    @TableField("name")
    private String name;

    @TableField("description")
    private String description; // 描述字段不是必需的

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
