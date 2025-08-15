package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("test_case_set")
public class TestCaseSet {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "用例集名称不能为空")
    @TableField("name")
    private String name;

    @NotBlank(message = "用例集版本不能为空")
    @TableField("version")
    private String version;

    @NotBlank(message = "用例集文件路径不能为空")
    @TableField("file_path")
    private String filePath;

    @TableField("gohttpserver_url")
    private String gohttpserverUrl;

    @TableField("file_size")
    private Long fileSize;

    @TableField("description")
    private String description;

    @TableField("status")
    private Integer status; // 0: 禁用, 1: 启用

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
