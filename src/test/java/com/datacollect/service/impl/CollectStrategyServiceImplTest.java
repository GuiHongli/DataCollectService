package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.entity.CollectStrategy;
import com.datacollect.entity.TestCase;
import com.datacollect.entity.TestCaseSet;
import com.datacollect.dto.CollectStrategyDTO;
import com.datacollect.dto.CustomParamDTO;
import com.datacollect.mapper.CollectStrategyMapper;
import com.datacollect.service.TestCaseSetService;
import com.datacollect.service.TestCaseService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;
import java.time.LocalDateTime;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Page.class, QueryWrapper.class})
public class CollectStrategyServiceImplTest {

    @InjectMocks
    private CollectStrategyServiceImpl collectStrategyService;

    @Mock
    private CollectStrategyMapper collectStrategyMapper;

    @Mock
    private TestCaseSetService testCaseSetService;

    @Mock
    private TestCaseService testCaseService;

    private List<CollectStrategy> mockStrategies;
    private List<TestCaseSet> mockTestCaseSets;
    private List<TestCase> mockTestCases;

    @Before
    public void setUp() {
        // 准备测试数据
        mockStrategies = Arrays.asList(
            createMockCollectStrategy(1L, "策略1", "INTENT_1", "{\"key\":\"param1\",\"value\":\"value1\"}", 1L),
            createMockCollectStrategy(2L, "策略2", "INTENT_2", "{\"key\":\"param2\",\"value\":\"value2\"}", 2L),
            createMockCollectStrategy(3L, "策略3", "INTENT_3", null, 3L)
        );

        mockTestCaseSets = Arrays.asList(
            createMockTestCaseSet(1L, "用例集1", "v1.0", "描述1", 1024L, "http://localhost:8080/file1"),
            createMockTestCaseSet(2L, "用例集2", "v1.0", "描述2", 2048L, "http://localhost:8080/file2"),
            createMockTestCaseSet(3L, "用例集3", "v1.0", "描述3", 3072L, "http://localhost:8080/file3")
        );

        mockTestCases = Arrays.asList(
            createMockTestCase(1L, "用例1", "TC001", "逻辑组网1", "业务大类1", "App1", "测试步骤1", "预期结果1", 1L),
            createMockTestCase(2L, "用例2", "TC002", "逻辑组网2", "业务大类2", "App2", "测试步骤2", "预期结果2", 1L),
            createMockTestCase(3L, "用例3", "TC003", "逻辑组网3", "业务大类3", "App3", "测试步骤3", "预期结果3", 2L)
        );
    }

    @Test
    public void testPageWithTestCaseSet_Success() {
        // 准备测试数据
        Page<CollectStrategy> page = new Page<>(1, 10);
        page.setTotal(3);
        page.setRecords(mockStrategies);

        // Mock 依赖服务
        when(testCaseSetService.getById(any())).thenReturn(mockTestCaseSets.get(0));
        when(testCaseService.getByTestCaseSetId(any())).thenReturn(Arrays.asList(mockTestCases.get(0), mockTestCases.get(1)));

        // 执行测试
        Page<CollectStrategyDTO> result = collectStrategyService.pageWithTestCaseSet(page, "策略", 1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(3, result.getTotal());
        assertEquals(3, result.getRecords().size());

        // 验证第一个DTO
        CollectStrategyDTO firstDto = result.getRecords().get(0);
        assertEquals(Long.valueOf(1L), firstDto.getId());
        assertEquals("策略1", firstDto.getName());
        assertEquals("{\"key\":\"param1\",\"value\":\"value1\"}", firstDto.getCustomParams());
        assertEquals("用例集1", firstDto.getTestCaseSetName());
        assertEquals("v1.0", firstDto.getTestCaseSetVersion());
        assertEquals("描述1", firstDto.getTestCaseSetDescription());
        assertEquals(Long.valueOf(1024L), firstDto.getTestCaseSetFileSize());
        assertEquals("http://localhost:8080/file1", firstDto.getTestCaseSetGohttpserverUrl());
        assertEquals(2, firstDto.getTestCaseList().size());
    }

    @Test
    public void testPageWithTestCaseSet_WithNameFilter() {
        // 准备测试数据
        Page<CollectStrategy> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(Arrays.asList(mockStrategies.get(0)));

        // Mock 依赖服务
        when(testCaseSetService.getById(any())).thenReturn(mockTestCaseSets.get(0));
        when(testCaseService.getByTestCaseSetId(any())).thenReturn(Arrays.asList(mockTestCases.get(0)));

        // 执行测试
        Page<CollectStrategyDTO> result = collectStrategyService.pageWithTestCaseSet(page, "策略1", null);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals("策略1", result.getRecords().get(0).getName());
    }

    @Test
    public void testPageWithTestCaseSet_WithTestCaseSetIdFilter() {
        // 准备测试数据
        Page<CollectStrategy> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(Arrays.asList(mockStrategies.get(0)));

        // Mock 依赖服务
        when(testCaseSetService.getById(1L)).thenReturn(mockTestCaseSets.get(0));
        when(testCaseService.getByTestCaseSetId(1L)).thenReturn(Arrays.asList(mockTestCases.get(0)));

        // 执行测试
        Page<CollectStrategyDTO> result = collectStrategyService.pageWithTestCaseSet(page, null, 1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(Long.valueOf(1L), result.getRecords().get(0).getTestCaseSetId());
    }

    @Test
    public void testListWithTestCaseSet_Success() {
        // 准备测试数据
        List<CollectStrategy> strategies = Arrays.asList(mockStrategies.get(0), mockStrategies.get(1));

        // Mock 依赖服务
        when(testCaseSetService.getById(any())).thenReturn(mockTestCaseSets.get(0));
        when(testCaseService.getByTestCaseSetId(any())).thenReturn(Arrays.asList(mockTestCases.get(0)));

        // 执行测试
        List<CollectStrategyDTO> result = collectStrategyService.listWithTestCaseSet();

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("策略1", result.get(0).getName());
        assertEquals("策略2", result.get(1).getName());
    }

    @Test
    public void testPageWithTestCaseSet_WithNullTestCaseSet() {
        // 准备测试数据
        Page<CollectStrategy> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(Arrays.asList(mockStrategies.get(0)));

        // Mock 依赖服务 - 测试用例集不存在
        when(testCaseSetService.getById(1L)).thenReturn(null);
        when(testCaseService.getByTestCaseSetId(any())).thenReturn(new ArrayList<>());

        // 执行测试
        Page<CollectStrategyDTO> result = collectStrategyService.pageWithTestCaseSet(page, null, 1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        CollectStrategyDTO dto = result.getRecords().get(0);
        assertNull(dto.getTestCaseSetName());
        assertNull(dto.getTestCaseSetVersion());
        assertNull(dto.getTestCaseSetDescription());
        assertNull(dto.getTestCaseSetFileSize());
        assertNull(dto.getTestCaseSetGohttpserverUrl());
        assertNull(dto.getTestCaseList());
    }

    @Test
    public void testPageWithTestCaseSet_WithEmptyTestCases() {
        // 准备测试数据
        Page<CollectStrategy> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(Arrays.asList(mockStrategies.get(0)));

        // Mock 依赖服务
        when(testCaseSetService.getById(1L)).thenReturn(mockTestCaseSets.get(0));
        when(testCaseService.getByTestCaseSetId(1L)).thenReturn(new ArrayList<>());

        // 执行测试
        Page<CollectStrategyDTO> result = collectStrategyService.pageWithTestCaseSet(page, null, 1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        CollectStrategyDTO dto = result.getRecords().get(0);
        assertNotNull(dto.getTestCaseSetName());
        assertNotNull(dto.getTestCaseList());
        assertEquals(0, dto.getTestCaseList().size());
    }

    @Test
    public void testPageWithTestCaseSet_WithInvalidCustomParams() {
        // 准备测试数据
        CollectStrategy strategyWithInvalidJson = createMockCollectStrategy(4L, "策略4", "INTENT_4", "invalid json", 1L);
        Page<CollectStrategy> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(Arrays.asList(strategyWithInvalidJson));

        // Mock 依赖服务
        when(testCaseSetService.getById(1L)).thenReturn(mockTestCaseSets.get(0));
        when(testCaseService.getByTestCaseSetId(1L)).thenReturn(Arrays.asList(mockTestCases.get(0)));

        // 执行测试
        Page<CollectStrategyDTO> result = collectStrategyService.pageWithTestCaseSet(page, null, 1L);

        // 验证结果 - 应该处理无效的 JSON 格式
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        CollectStrategyDTO dto = result.getRecords().get(0);
        assertEquals("invalid json", dto.getCustomParams());
        assertNotNull(dto.getCustomParamList());
        assertEquals(0, dto.getCustomParamList().size()); // 无效JSON应该返回空列表
    }

    @Test
    public void testPageWithTestCaseSet_WithNullCustomParams() {
        // 准备测试数据
        CollectStrategy strategyWithNullParams = createMockCollectStrategy(5L, "策略5", "INTENT_5", null, 1L);
        Page<CollectStrategy> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(Arrays.asList(strategyWithNullParams));

        // Mock 依赖服务
        when(testCaseSetService.getById(1L)).thenReturn(mockTestCaseSets.get(0));
        when(testCaseService.getByTestCaseSetId(1L)).thenReturn(Arrays.asList(mockTestCases.get(0)));

        // 执行测试
        Page<CollectStrategyDTO> result = collectStrategyService.pageWithTestCaseSet(page, null, 1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        CollectStrategyDTO dto = result.getRecords().get(0);
        assertNull(dto.getCustomParams());
        assertNotNull(dto.getCustomParamList());
        assertEquals(0, dto.getCustomParamList().size()); // null参数应该返回空列表
    }

    // 辅助方法：创建 Mock CollectStrategy
    private CollectStrategy createMockCollectStrategy(Long id, String name, String intent, String customParams, Long testCaseSetId) {
        CollectStrategy strategy = new CollectStrategy();
        strategy.setId(id);
        strategy.setName(name);
        strategy.setIntent(intent);
        strategy.setCustomParams(customParams);
        strategy.setTestCaseSetId(testCaseSetId);
        strategy.setCollectCount(10);
        strategy.setBusinessCategory("业务大类");
        strategy.setApp("测试应用");
        strategy.setDescription("测试描述");
        strategy.setStatus(1);
        strategy.setCreateBy("测试用户");
        strategy.setUpdateBy("测试用户");
        strategy.setCreateTime(LocalDateTime.now());
        strategy.setUpdateTime(LocalDateTime.now());
        strategy.setDeleted(0);
        return strategy;
    }

    // 辅助方法：创建 Mock TestCaseSet
    private TestCaseSet createMockTestCaseSet(Long id, String name, String version, String description, Long fileSize, String gohttpserverUrl) {
        TestCaseSet testCaseSet = new TestCaseSet();
        testCaseSet.setId(id);
        testCaseSet.setName(name);
        testCaseSet.setVersion(version);
        testCaseSet.setFilePath("/path/to/file");
        testCaseSet.setGohttpserverUrl(gohttpserverUrl);
        testCaseSet.setFileSize(fileSize);
        testCaseSet.setDescription(description);
        testCaseSet.setStatus(1);
        testCaseSet.setCreateBy("测试用户");
        testCaseSet.setUpdateBy("测试用户");
        testCaseSet.setCreateTime(LocalDateTime.now());
        testCaseSet.setUpdateTime(LocalDateTime.now());
        testCaseSet.setDeleted(0);
        return testCaseSet;
    }

    // 辅助方法：创建 Mock TestCase
    private TestCase createMockTestCase(Long id, String name, String number, String logicNetwork, String businessCategory, String app, String testSteps, String expectedResult, Long testCaseSetId) {
        TestCase testCase = new TestCase();
        testCase.setId(id);
        testCase.setName(name);
        testCase.setNumber(number);
        testCase.setLogicNetwork(logicNetwork);
        testCase.setBusinessCategory(businessCategory);
        testCase.setApp(app);
        testCase.setTestSteps(testSteps);
        testCase.setExpectedResult(expectedResult);
        testCase.setTestCaseSetId(testCaseSetId);
        testCase.setCreateTime(LocalDateTime.now());
        testCase.setUpdateTime(LocalDateTime.now());
        testCase.setDeleted(0);
        return testCase;
    }
}
