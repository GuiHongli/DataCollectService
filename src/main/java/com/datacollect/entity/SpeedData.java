package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 速率数据实体
 * 对应speed-10s.xlsx文件的数据结构
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("speed_data")
public class SpeedData {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID（关联client_task_info表的task_id）
     */
    @TableField("task_id")
    private String taskId;

    /**
     * 下行速率（bps）
     */
    @TableField("dl_speed")
    private String dlSpeed;

    /**
     * 上行速率（bps）
     */
    @TableField("ul_speed")
    private String ulSpeed;

    /**
     * 总体速率（bps）
     */
    @TableField("total")
    private String total;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}

