package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * vMOS计算参数配置实体
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vmos_params_config")
public class VmosParamsConfig {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  /**
   * 应用大类（shortvideo, voip, watch_live, live_streaming, vod_streaming, meeting, mobile_game, mobile_game_cloud）
   */
  @TableField("service")
  private String service;

  /**
   * a1参数（bitrate参数）
   */
  @TableField("a1")
  private BigDecimal a1;

  /**
   * a2参数（resolution参数）
   */
  @TableField("a2")
  private BigDecimal a2;

  /**
   * w1参数（sQuality权重1）
   */
  @TableField("w1")
  private BigDecimal w1;

  /**
   * w2参数（sQuality权重2）
   */
  @TableField("w2")
  private BigDecimal w2;

  /**
   * a3参数（RTT参数）
   */
  @TableField("a3")
  private BigDecimal a3;

  /**
   * a4参数（lost_packet_rate参数）
   */
  @TableField("a4")
  private BigDecimal a4;

  /**
   * a5参数（stall_rate参数）
   */
  @TableField("a5")
  private BigDecimal a5;

  /**
   * g1参数（sView权重1）
   */
  @TableField("g1")
  private BigDecimal g1;

  /**
   * g2参数（sView权重2）
   */
  @TableField("g2")
  private BigDecimal g2;

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
