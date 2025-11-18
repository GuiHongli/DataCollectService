package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("test_case_custom_param")
public class TestCaseCustomParam {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "业务大类不能为空")
    @TableField("business_category")
    private String businessCategory;

    @NotBlank(message = "APP不能为空")
    @TableField("app")
    private String app;

    @NotBlank(message = "自定义参数名称不能为空")
    @TableField("param_name")
    private String paramName;

    /**
     * 自定义参数值，JSON数组格式存储，如：["value1", "value2", "value3"]
     */
    @NotBlank(message = "自定义参数值不能为空")
    @TableField("param_values")
    private String paramValues;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}















