package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.TestCase;
import com.datacollect.mapper.TestCaseMapper;
import com.datacollect.service.TestCaseService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestCaseServiceImpl extends ServiceImpl<TestCaseMapper, TestCase> implements TestCaseService {
    
    @Override
    public List<TestCase> getByTestCaseSetId(Long testCaseSetId) {
        QueryWrapper<TestCase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("test_case_set_id", testCaseSetId);
        queryWrapper.orderByAsc("number");
        return this.list(queryWrapper);
    }
    
    @Override
    public void saveBatch(List<TestCase> testCases) {
        if (testCases != null && !testCases.isEmpty()) {
            super.saveBatch(testCases);
        }
    }
}
