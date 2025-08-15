package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.TestCase;

import java.util.List;

public interface TestCaseService extends IService<TestCase> {
    
    /**
     * 根据用例集ID获取测试用例列表
     */
    List<TestCase> getByTestCaseSetId(Long testCaseSetId);
    
    /**
     * 批量保存测试用例
     */
    void saveBatch(List<TestCase> testCases);
}
