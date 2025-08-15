package com.datacollect.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("test_case")
public class TestCase {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotNull(message = "用例集ID不能为空")
    @TableField("test_case_set_id")
    private Long testCaseSetId;

    @NotBlank(message = "用例名称不能为空")
    @TableField("name")
    private String name;

    @NotBlank(message = "用例编号不能为空")
    @TableField("code")
    private String code;

    @TableField("logic_network")
    private String logicNetwork;

    @TableField("test_steps")
    private String testSteps;

    @TableField("expected_result")
    private String expectedResult;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
