package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 端侧任务信息实体
 * 对应taskinfo.json文件的数据结构
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("client_task_info")
public class ClientTaskInfo {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    @TableField("task_id")
    private String taskId;

    /**
     * 国家信息
     */
    @TableField("nation")
    private String nation;

    /**
     * 运营商信息
     */
    @TableField("operator")
    private String operator;

    /**
     * PRB
     */
    @TableField("prb")
    private String prb;

    /**
     * RSRP
     */
    @TableField("rsrp")
    private String rsrp;

    /**
     * 业务大类
     */
    @TableField("service")
    private String service;

    /**
     * 应用名称
     */
    @TableField("app")
    private String app;

    /**
     * 开始时间（格式：年月日时分秒，24小时制）
     */
    @TableField("start_time")
    private String startTime;

    /**
     * 结束时间（格式：年月日时分秒，24小时制）
     */
    @TableField("end_time")
    private String endTime;

    /**
     * 用户类别
     */
    @TableField("user_category")
    private String userCategory;

    /**
     * 设备ID
     */
    @TableField("device_id")
    private String deviceId;

    /**
     * 数据报告（JSON格式）
     * 包含：stunNumber（卡顿次数）、stunRate（卡顿率）、
     * avgUplinkRtt（平均上行RTT）、avgDownlinkRtt（平均下行RTT）、
     * avgUplinkSpeed（平均上行速率）、avgDownlinkSpeed（平均下行速率）、
     * avgUplinkLost（平均上行丢包率）、avgDownlinkLost（平均下行丢包率）、
     * avgLost（平均丢包率）
     */
    @TableField("summary")
    private String summary;

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

