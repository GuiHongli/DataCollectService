package com.datacollect.entity.dto;

import com.datacollect.entity.LogicEnvironment;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class CreateLogicEnvironmentRequest {
    
    @Valid
    private LogicEnvironment logicEnvironment;
    
    @NotEmpty(message = "UE列表不能为空")
    private List<Long> ueIds;
    
    private List<Long> networkIds;
}
