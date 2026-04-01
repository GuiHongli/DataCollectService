package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 网络侧数据实体
 * 对应网络侧压缩包中CSV文件的数据结构
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("network_data")
public class NetworkData {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 序列号
     */
    @TableField("serial_no")
    private String serialNo;

    /**
     * 时间戳
     */
    @TableField("time_stamp")
    private String timeStamp;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private String startTime;

    /**
     * GPSI
     */
    @TableField("gpsi")
    private String gpsi;

    /**
     * DNN
     */
    @TableField("dnn")
    private String dnn;

    /**
     * S-NSSAI
     */
    @TableField("s_nssai")
    private String sNssai;

    /**
     * RAT类型
     */
    @TableField("rat_type")
    private String ratType;

    /**
     * QCI
     */
    @TableField("qci")
    private String qci;

    /**
     * 体验优化标志
     */
    @TableField("exp_opt_flag")
    private String expOptFlag;

    /**
     * 体验优化开始时间
     */
    @TableField("exp_opt_start_time")
    private String expOptStartTime;

    /**
     * 应用ID
     */
    @TableField("app_id")
    private String appId;

    /**
     * 子应用ID
     */
    @TableField("sub_app_id")
    private String subAppId;

    /**
     * 应用状态
     */
    @TableField("app_status")
    private String appStatus;

    /**
     * 应用质量
     */
    @TableField("app_quality")
    private String appQuality;

    /**
     * TAI
     */
    @TableField("tai")
    private String tai;

    /**
     * 小区ID
     */
    @TableField("cell_id")
    private String cellId;

    /**
     * 下行时延
     */
    @TableField("delay_an")
    private String delayAn;

    /**
     * 上行时延
     */
    @TableField("delay_dn")
    private String delayDn;

    /**
     * 上行带宽
     */
    @TableField("uplink_bandwidth")
    private String uplinkBandwidth;

    /**
     * 下行带宽
     */
    @TableField("downlink_bandwidth")
    private String downlinkBandwidth;

    /**
     * 上行包数
     */
    @TableField("uplink_pkg")
    private String uplinkPkg;

    /**
     * 下行包数
     */
    @TableField("downlink_pkg")
    private String downlinkPkg;

    /**
     * 丢失上行包数
     */
    @TableField("lost_uplink_pkg")
    private String lostUplinkPkg;

    /**
     * 丢失下行包数
     */
    @TableField("lost_downlink_pkg")
    private String lostDownlinkPkg;

    /**
     * 子应用开始时间
     */
    @TableField("sub_app_start_time")
    private String subAppStartTime;

    /**
     * 信息指示
     */
    @TableField("info_indicate")
    private String infoIndicate;

    /**
     * UPFLD
     */
    @TableField("upfld")
    private String upfld;

    /**
     * PEI
     */
    @TableField("pei")
    private String pei;

    /**
     * 子应用有效时长
     */
    @TableField("sub_app_eff_duration")
    private String subAppEffDuration;

    /**
     * 最大下行时延
     */
    @TableField("max_delay_an")
    private String maxDelayAn;

    /**
     * 最大上行时延
     */
    @TableField("max_delay_dn")
    private String maxDelayDn;

    /**
     * 平均上行带宽
     */
    @TableField("avg_bandwidth_ui")
    private String avgBandwidthUI;

    /**
     * 平均下行带宽
     */
    @TableField("avg_bandwidth_di")
    private String avgBandwidthDI;

    /**
     * 最大上行带宽
     */
    @TableField("max_bandwidth_ui")
    private String maxBandwidthUI;

    /**
     * 最大下行带宽
     */
    @TableField("max_bandwidth_di")
    private String maxBandwidthDI;

    /**
     * 上行流量
     */
    @TableField("volume_ui")
    private String volumeUI;

    /**
     * 下行流量
     */
    @TableField("volume_di")
    private String volumeDI;

    /**
     * 体验优化结束时间
     */
    @TableField("exp_opt_end_time")
    private String expOptEndTime;

    /**
     * 子应用EDR开始时间
     */
    @TableField("sub_app_edr_start_time")
    private String subAppEdrStartTime;

    /**
     * 用户服务负载上行ECN
     */
    @TableField("user_service_load_ui_ecn")
    private String userServiceLoadUIEcn;

    /**
     * 用户服务负载下行ECN
     */
    @TableField("user_service_load_di_ecn")
    private String userServiceLoadDIEcn;

    /**
     * 保障失败原因
     */
    @TableField("assurance_failed_reason")
    private String assuranceFailedReason;

    /**
     * gNB QNC通知类型
     */
    @TableField("gnb_qnc_notif_type")
    private String gnbQncNotifType;

    /**
     * 平均QoE
     */
    @TableField("avg_qoe")
    private String avgQoe;

    /**
     * 最大分辨率
     */
    @TableField("max_resolution")
    private String maxResolution;

    /**
     * 最常见分辨率
     */
    @TableField("most_resolution")
    private String mostResolution;

    /**
     * 最大上行码率
     */
    @TableField("max_bit_rate_ui")
    private String maxBitRateUI;

    /**
     * 平均上行码率
     */
    @TableField("avg_bit_rate_ui")
    private String avgBitRateUI;

    /**
     * 最大下行码率
     */
    @TableField("max_bit_rate_di")
    private String maxBitRateDI;

    /**
     * 平均下行码率
     */
    @TableField("avg_bit_rate_di")
    private String avgBitRateDI;

    /**
     * 卡顿时长
     */
    @TableField("stalling_duration")
    private String stallingDuration;

    /**
     * 卡顿次数
     */
    @TableField("stalling_number")
    private String stallingNumber;

    /**
     * 服务时延
     */
    @TableField("service_delay")
    private String serviceDelay;

    /**
     * 服务初始时长
     */
    @TableField("service_initial_duration")
    private String serviceInitialDuration;

    /**
     * 文件名（用于标识来源）
     */
    @TableField("file_name")
    private String fileName;

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





