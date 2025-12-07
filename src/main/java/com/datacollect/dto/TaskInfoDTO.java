package com.datacollect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * 端侧任务信息DTO
 * 对应taskinfo.json文件的数据结构
 */
@Data
public class TaskInfoDTO {

    /**
     * 任务ID
     */
    @JsonProperty("taskId")
    private String taskId;

    /**
     * 国家信息
     */
    @JsonProperty("nation")
    private String nation;

    /**
     * 运营商信息
     */
    @JsonProperty("operator")
    private String operator;

    /**
     * PRB
     */
    @JsonProperty("prb")
    private String prb;

    /**
     * RSRP
     */
    @JsonProperty("rsrp")
    private String rsrp;

    /**
     * 业务大类
     */
    @JsonProperty("service")
    private String service;

    /**
     * 应用名称
     */
    @JsonProperty("app")
    private String app;

    /**
     * 开始时间（格式：年月日时分秒，24小时制）
     */
    @JsonProperty("startTime")
    private String startTime;

    /**
     * 结束时间（格式：年月日时分秒，24小时制）
     */
    @JsonProperty("endTime")
    private String endTime;

    /**
     * 用户类别
     */
    @JsonProperty("userCategory")
    private String userCategory;

    /**
     * 设备ID
     */
    @JsonProperty("deviceId")
    private String deviceId;

    /**
     * 数据报告（JSON格式）
     * 包含：stunNumber（卡顿次数）、stunRate（卡顿率）、
     * avgUplinkRtt（平均上行RTT）、avgDownlinkRtt（平均下行RTT）、
     * avgUplinkSpeed（平均上行速率）、avgDownlinkSpeed（平均下行速率）、
     * avgUplinkLost（平均上行丢包率）、avgDownlinkLost（平均下行丢包率）、
     * avgLost（平均丢包率）
     */
    @JsonProperty("summary")
    private Map<String, String> summary;
}



