package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 丢包率数据实体
 * 对应lost-10s.csv文件的数据结构
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("lost_data")
public class LostData {

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
     * 时间索引
     */
    @TableField("index_time")
    private String indexTime;

    /**
     * 下行丢包率（%）
     */
    @TableField("dl_loss")
    private String dlLoss;

    /**
     * 上行丢包率（%）
     */
    @TableField("ul_loss")
    private String ulLoss;

    /**
     * 总丢包率（%）
     */
    @TableField("total_loss")
    private String totalLoss;

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



