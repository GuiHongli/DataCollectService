package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 端侧和网络侧时间配置实体
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("test_settings_time_config")
public class TestSettingsTimeConfig {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  /**
   * 端侧匹配网络侧时间差（最大10，最小0）
   */
  @NotNull(message = "时间差不能为空")
  @Min(value = 0, message = "时间差最小值为0")
  @Max(value = 10, message = "时间差最大值为10")
  @TableField("time_diff")
  private Integer timeDiff;

  /**
   * 端侧采集时间间隔（秒）
   * 可选值：10或30
   */
  @NotNull(message = "采集时间间隔不能为空")
  @TableField("collect_interval")
  private Integer collectInterval;

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


