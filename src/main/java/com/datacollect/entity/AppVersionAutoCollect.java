package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * app版本变更自动采集配置实体
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@TableName("app_version_auto_collect")
public class AppVersionAutoCollect {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 应用名称
     */
    @TableField("app_name")
    private String appName;
    
    /**
     * 平台类型（0-安卓，1-iOS）
     */
    @TableField("platform_type")
    private Boolean platformType;
    
    /**
     * 是否自动采集（0-否，1-是）
     */
    @TableField("auto_collect")
    private Boolean autoCollect;
    
    /**
     * 采集任务模版ID
     */
    @TableField("template_id")
    private Long templateId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 是否删除（0-未删除，1-已删除）
     */
    @TableField("deleted")
    private Integer deleted;
}

