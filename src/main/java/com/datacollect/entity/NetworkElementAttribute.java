package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 网元属性实体类
 *
 * @author system
 * @since 2025-01-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("network_element_attribute")
public class NetworkElementAttribute {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotNull(message = "网元ID不能为空")
    @TableField("network_element_id")
    private Long networkElementId;

    @NotBlank(message = "属性名称不能为空")
    @TableField("attribute_name")
    private String attributeName;

    @TableField("attribute_value")
    private String attributeValue;

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






