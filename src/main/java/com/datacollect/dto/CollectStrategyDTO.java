package com.datacollect.dto;

import com.datacollect.entity.CollectStrategy;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class CollectStrategyDTO extends CollectStrategy {

    // 用例集信息
    private String testCaseSetName;
    private String testCaseSetVersion;
    private String testCaseSetDescription;
    private Long testCaseSetFileSize;
    private String testCaseSetGohttpserverUrl;
    
    // 测试用例信息列表
    private List<TestCaseInfo> testCaseList;

    @Data
    public static class TestCaseInfo {
        private Long id;
        private String name;
        private String number;
        private String logicNetwork;
        private String testSteps;
        private String expectedResult;
    }
}
