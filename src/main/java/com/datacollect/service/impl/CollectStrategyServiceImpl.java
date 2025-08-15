package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.dto.CollectStrategyDTO;
import com.datacollect.entity.CollectStrategy;
import com.datacollect.entity.TestCaseSet;
import com.datacollect.entity.TestCase;
import com.datacollect.mapper.CollectStrategyMapper;
import com.datacollect.service.CollectStrategyService;
import com.datacollect.service.TestCaseSetService;
import com.datacollect.service.TestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CollectStrategyServiceImpl extends ServiceImpl<CollectStrategyMapper, CollectStrategy> implements CollectStrategyService {

    @Autowired
    private TestCaseSetService testCaseSetService;

    @Autowired
    private TestCaseService testCaseService;

    @Override
    public Page<CollectStrategyDTO> pageWithTestCaseSet(Page<CollectStrategy> page, String name, Long testCaseSetId) {
        // 查询采集策略
        QueryWrapper<CollectStrategy> queryWrapper = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        if (testCaseSetId != null) {
            queryWrapper.eq("test_case_set_id", testCaseSetId);
        }
        queryWrapper.orderByDesc("create_time");
        
        Page<CollectStrategy> strategyPage = this.page(page, queryWrapper);
        
        // 转换为DTO
        Page<CollectStrategyDTO> dtoPage = new Page<>();
        dtoPage.setCurrent(strategyPage.getCurrent());
        dtoPage.setSize(strategyPage.getSize());
        dtoPage.setTotal(strategyPage.getTotal());
        
        List<CollectStrategyDTO> dtoList = strategyPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }

    @Override
    public List<CollectStrategyDTO> listWithTestCaseSet() {
        QueryWrapper<CollectStrategy> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("create_time");
        
        List<CollectStrategy> strategies = this.list(queryWrapper);
        return strategies.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为DTO
     */
    private CollectStrategyDTO convertToDTO(CollectStrategy strategy) {
        CollectStrategyDTO dto = new CollectStrategyDTO();
        
        // 复制基本属性
        dto.setId(strategy.getId());
        dto.setName(strategy.getName());
        dto.setCollectCount(strategy.getCollectCount());
        dto.setTestCaseSetId(strategy.getTestCaseSetId());
        dto.setDescription(strategy.getDescription());
        dto.setStatus(strategy.getStatus());
        dto.setCreateBy(strategy.getCreateBy());
        dto.setUpdateBy(strategy.getUpdateBy());
        dto.setCreateTime(strategy.getCreateTime());
        dto.setUpdateTime(strategy.getUpdateTime());
        dto.setDeleted(strategy.getDeleted());
        
        // 获取用例集详细信息
        if (strategy.getTestCaseSetId() != null) {
            TestCaseSet testCaseSet = testCaseSetService.getById(strategy.getTestCaseSetId());
            if (testCaseSet != null) {
                dto.setTestCaseSetName(testCaseSet.getName());
                dto.setTestCaseSetVersion(testCaseSet.getVersion());
                dto.setTestCaseSetDescription(testCaseSet.getDescription());
                dto.setTestCaseSetFileSize(testCaseSet.getFileSize());
                dto.setTestCaseSetGohttpserverUrl(testCaseSet.getGohttpserverUrl());
                
                // 获取测试用例信息
                List<TestCase> testCases = testCaseService.getByTestCaseSetId(testCaseSet.getId());
                List<CollectStrategyDTO.TestCaseInfo> testCaseInfoList = testCases.stream()
                    .map(testCase -> {
                        CollectStrategyDTO.TestCaseInfo testCaseInfo = new CollectStrategyDTO.TestCaseInfo();
                        testCaseInfo.setId(testCase.getId());
                        testCaseInfo.setName(testCase.getName());
                        testCaseInfo.setCode(testCase.getCode());
                        testCaseInfo.setLogicNetwork(testCase.getLogicNetwork());
                        testCaseInfo.setTestSteps(testCase.getTestSteps());
                        testCaseInfo.setExpectedResult(testCase.getExpectedResult());
                        return testCaseInfo;
                    })
                    .collect(Collectors.toList());
                dto.setTestCaseList(testCaseInfoList);
            }
        }
        
        return dto;
    }
}
