package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.entity.Executor;
import com.datacollect.entity.LogicEnvironment;
import com.datacollect.entity.LogicEnvironmentUe;
import com.datacollect.entity.LogicNetwork;
import com.datacollect.entity.Ue;
import com.datacollect.entity.dto.LogicEnvironmentDTO;
import com.datacollect.mapper.ExecutorMapper;
import com.datacollect.mapper.LogicEnvironmentMapper;
import com.datacollect.mapper.LogicEnvironmentUeMapper;
import com.datacollect.mapper.LogicNetworkMapper;
import com.datacollect.mapper.UeMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Page.class, QueryWrapper.class})
public class LogicEnvironmentServiceImplTest {

    @InjectMocks
    private LogicEnvironmentServiceImpl logicEnvironmentService;

    @Mock
    private LogicEnvironmentMapper logicEnvironmentMapper;

    @Mock
    private LogicEnvironmentUeMapper logicEnvironmentUeMapper;

    @Mock
    private LogicNetworkMapper logicNetworkMapper;

    @Mock
    private UeMapper ueMapper;

    @Mock
    private ExecutorMapper executorMapper;

    private List<LogicEnvironment> mockEnvironments;
    private List<LogicEnvironmentUe> mockEnvironmentUes;
    private List<LogicNetwork> mockNetworks;
    private List<Ue> mockUes;
    private List<Executor> mockExecutors;

    @Before
    public void setUp() {
        // 准备测试数据
        mockEnvironments = Arrays.asList(
            createMockLogicEnvironment(1L, "环境1", "描述1", 1L),
            createMockLogicEnvironment(2L, "环境2", "描述2", 2L),
            createMockLogicEnvironment(3L, "环境3", "描述3", null)
        );

        mockEnvironmentUes = Arrays.asList(
            createMockLogicEnvironmentUe(1L, 1L, 1L),
            createMockLogicEnvironmentUe(2L, 1L, 2L),
            createMockLogicEnvironmentUe(3L, 2L, 3L)
        );

        mockNetworks = Arrays.asList(
            createMockLogicNetwork(1L, "网络1", 1L),
            createMockLogicNetwork(2L, "网络2", 1L),
            createMockLogicNetwork(3L, "网络3", 2L)
        );

        mockUes = Arrays.asList(
            createMockUe(1L, "UE001", "UE1", 1L),
            createMockUe(2L, "UE002", "UE2", 1L),
            createMockUe(3L, "UE003", "UE3", 2L)
        );

        mockExecutors = Arrays.asList(
            createMockExecutor(1L, "192.168.1.100", "执行器1"),
            createMockExecutor(2L, "192.168.1.101", "执行器2")
        );
    }

    @Test
    public void testGetLogicEnvironmentPageWithDetails_Success() {
        // 准备测试数据
        Integer current = 1;
        Integer size = 10;
        String name = "环境";
        Long executorId = 1L;

        // Mock 分页结果
        Page<LogicEnvironment> mockPage = PowerMockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(mockEnvironments);
        when(mockPage.getTotal()).thenReturn(3L);
        when(mockPage.getCurrent()).thenReturn(current.longValue());
        when(mockPage.getSize()).thenReturn(size.longValue());

        // Mock Mapper 调用
        when(logicEnvironmentMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

        // 执行测试
        Page<LogicEnvironmentDTO> result = logicEnvironmentService.getLogicEnvironmentPageWithDetails(current, size, name, executorId);

        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.getRecords().size());
        assertEquals(3L, result.getTotal());
        assertEquals(current.longValue(), result.getCurrent());
        assertEquals(size.longValue(), result.getSize());

        // 验证第一个环境的数据
        LogicEnvironmentDTO firstEnvironment = result.getRecords().get(0);
        assertEquals("环境1", firstEnvironment.getName());
        assertEquals("描述1", firstEnvironment.getDescription());
        assertEquals(Long.valueOf(1L), firstEnvironment.getExecutorId());
    }

    @Test
    public void testGetLogicEnvironmentPageWithDetails_WithNameFilter() {
        // 准备测试数据
        Integer current = 1;
        Integer size = 10;
        String name = "环境1";
        Long executorId = null;

        // Mock 分页结果 - 只返回匹配名称的环境
        List<LogicEnvironment> filteredEnvironments = Arrays.asList(mockEnvironments.get(0));
        Page<LogicEnvironment> mockPage = PowerMockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(filteredEnvironments);
        when(mockPage.getTotal()).thenReturn(1L);
        when(mockPage.getCurrent()).thenReturn(current.longValue());
        when(mockPage.getSize()).thenReturn(size.longValue());

        // Mock Mapper 调用
        when(logicEnvironmentMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

        // 执行测试
        Page<LogicEnvironmentDTO> result = logicEnvironmentService.getLogicEnvironmentPageWithDetails(current, size, name, executorId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());

        // 验证过滤后的环境数据
        LogicEnvironmentDTO filteredEnvironment = result.getRecords().get(0);
        assertEquals("环境1", filteredEnvironment.getName());
    }

    @Test
    public void testGetLogicEnvironmentPageWithDetails_WithExecutorIdFilter() {
        // 准备测试数据
        Integer current = 1;
        Integer size = 10;
        String name = null;
        Long executorId = 1L;

        // Mock 分页结果 - 只返回匹配执行器的环境
        List<LogicEnvironment> filteredEnvironments = Arrays.asList(mockEnvironments.get(0));
        Page<LogicEnvironment> mockPage = PowerMockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(filteredEnvironments);
        when(mockPage.getTotal()).thenReturn(1L);
        when(mockPage.getCurrent()).thenReturn(current.longValue());
        when(mockPage.getSize()).thenReturn(size.longValue());

        // Mock Mapper 调用
        when(logicEnvironmentMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

        // 执行测试
        Page<LogicEnvironmentDTO> result = logicEnvironmentService.getLogicEnvironmentPageWithDetails(current, size, name, executorId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());

        // 验证过滤后的环境数据
        LogicEnvironmentDTO filteredEnvironment = result.getRecords().get(0);
        assertEquals(Long.valueOf(1L), filteredEnvironment.getExecutorId());
    }

    @Test
    public void testGetLogicEnvironmentPageWithDetails_EmptyResult() {
        // 准备测试数据
        Integer current = 1;
        Integer size = 10;
        String name = "不存在的环境";
        Long executorId = null;

        // Mock 空分页结果
        Page<LogicEnvironment> mockPage = PowerMockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(new ArrayList<>());
        when(mockPage.getTotal()).thenReturn(0L);
        when(mockPage.getCurrent()).thenReturn(current.longValue());
        when(mockPage.getSize()).thenReturn(size.longValue());

        // Mock Mapper 调用
        when(logicEnvironmentMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

        // 执行测试
        Page<LogicEnvironmentDTO> result = logicEnvironmentService.getLogicEnvironmentPageWithDetails(current, size, name, executorId);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getRecords().size());
        assertEquals(0L, result.getTotal());
    }

    @Test
    public void testGetLogicEnvironmentPageWithDetails_WithNullFilters() {
        // 准备测试数据 - 所有过滤器都为 null
        Integer current = 1;
        Integer size = 10;
        String name = null;
        Long executorId = null;

        // Mock 分页结果
        Page<LogicEnvironment> mockPage = PowerMockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(mockEnvironments);
        when(mockPage.getTotal()).thenReturn(3L);
        when(mockPage.getCurrent()).thenReturn(current.longValue());
        when(mockPage.getSize()).thenReturn(size.longValue());

        // Mock Mapper 调用
        when(logicEnvironmentMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

        // 执行测试
        Page<LogicEnvironmentDTO> result = logicEnvironmentService.getLogicEnvironmentPageWithDetails(current, size, name, executorId);

        // 验证结果 - 应该返回所有环境
        assertNotNull(result);
        assertEquals(3, result.getRecords().size());
        assertEquals(3L, result.getTotal());
    }

    @Test
    public void testGetLogicEnvironmentPageWithDetails_WithMultipleFilters() {
        // 准备测试数据 - 多个过滤器
        Integer current = 1;
        Integer size = 10;
        String name = "环境";
        Long executorId = 1L;

        // Mock 分页结果 - 返回匹配多个条件的环境
        List<LogicEnvironment> filteredEnvironments = Arrays.asList(mockEnvironments.get(0));
        Page<LogicEnvironment> mockPage = PowerMockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(filteredEnvironments);
        when(mockPage.getTotal()).thenReturn(1L);
        when(mockPage.getCurrent()).thenReturn(current.longValue());
        when(mockPage.getSize()).thenReturn(size.longValue());

        // Mock Mapper 调用
        when(logicEnvironmentMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

        // 执行测试
        Page<LogicEnvironmentDTO> result = logicEnvironmentService.getLogicEnvironmentPageWithDetails(current, size, name, executorId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());

        // 验证匹配多个条件的环境
        LogicEnvironmentDTO filteredEnvironment = result.getRecords().get(0);
        assertEquals("环境1", filteredEnvironment.getName());
        assertEquals(Long.valueOf(1L), filteredEnvironment.getExecutorId());
    }

    // 辅助方法：创建 Mock LogicEnvironment
    private LogicEnvironment createMockLogicEnvironment(Long id, String name, String description, Long executorId) {
        LogicEnvironment environment = new LogicEnvironment();
        environment.setId(id);
        environment.setName(name);
        environment.setDescription(description);
        environment.setExecutorId(executorId);
        return environment;
    }

    // 辅助方法：创建 Mock LogicEnvironmentUe
    private LogicEnvironmentUe createMockLogicEnvironmentUe(Long id, Long environmentId, Long ueId) {
        LogicEnvironmentUe environmentUe = new LogicEnvironmentUe();
        environmentUe.setId(id);
        environmentUe.setLogicEnvironmentId(environmentId);
        environmentUe.setUeId(ueId);
        return environmentUe;
    }

    // 辅助方法：创建 Mock LogicNetwork
    private LogicNetwork createMockLogicNetwork(Long id, String name, Long environmentId) {
        LogicNetwork network = new LogicNetwork();
        network.setId(id);
        network.setName(name);
        network.setDescription("网络描述");
        return network;
    }

    // 辅助方法：创建 Mock Ue
    private Ue createMockUe(Long id, String ueId, String name, Long networkTypeId) {
        Ue ue = new Ue();
        ue.setId(id);
        ue.setUeId(ueId);
        ue.setName(name);
        ue.setNetworkTypeId(networkTypeId);
        return ue;
    }

    // 辅助方法：创建 Mock Executor
    private Executor createMockExecutor(Long id, String ipAddress, String name) {
        Executor executor = new Executor();
        executor.setId(id);
        executor.setIpAddress(ipAddress);
        executor.setName(name);
        return executor;
    }
}
