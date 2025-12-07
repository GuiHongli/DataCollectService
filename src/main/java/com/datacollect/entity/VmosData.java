package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * vMOS数据实体
 * 对应vmos-10s.xlsx文件的数据结构
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vmos_data")
public class VmosData {

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
     * 序号
     */
    @TableField("sequence_number")
    private String sequenceNumber;

    /**
     * 速率
     */
    @TableField("speed")
    private String speed;

    /**
     * 分辨率
     */
    @TableField("resolution")
    private String resolution;

    /**
     * RTT
     */
    @TableField("rtt")
    private String rtt;

    /**
     * 丢包率
     */
    @TableField("packet_loss_rate")
    private String packetLossRate;

    /**
     * 卡顿占比
     */
    @TableField("stutter_ratio")
    private String stutterRatio;

    /**
     * 初缓时延
     */
    @TableField("initial_buffering_delay")
    private String initialBufferingDelay;

    /**
     * 码率
     */
    @TableField("bitrate")
    private String bitrate;

    /**
     * 视频体验
     */
    @TableField("video_experience")
    private String videoExperience;

    /**
     * 交互体验
     */
    @TableField("interaction_experience")
    private String interactionExperience;

    /**
     * 呈现体验
     */
    @TableField("presentation_experience")
    private String presentationExperience;

    /**
     * α
     */
    @TableField("alpha")
    private String alpha;

    /**
     * β
     */
    @TableField("beta")
    private String beta;

    /**
     * vMOS
     */
    @TableField("vmos")
    private String vmos;

    /**
     * avgQOE
     */
    @TableField("avg_qoe")
    private String avgQoe;

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

