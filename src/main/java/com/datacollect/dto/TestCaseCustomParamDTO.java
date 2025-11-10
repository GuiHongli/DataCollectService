package com.datacollect.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class TestCaseCustomParamDTO {

    private Long id;

    @NotBlank(message = "业务大类不能为空")
    private String businessCategory;

    @NotBlank(message = "APP不能为空")
    private String app;

    @NotBlank(message = "自定义参数名称不能为空")
    private String paramName;

    @NotEmpty(message = "自定义参数值不能为空")
    private List<String> paramValues;
}







