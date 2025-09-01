package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.entity.Executor;
import com.datacollect.entity.Region;
import com.datacollect.entity.dto.ExecutorDTO;
import com.datacollect.mapper.ExecutorMapper;
import com.datacollect.mapper.RegionMapper;
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
public class ExecutorServiceImplTest {

    @InjectMocks
    private ExecutorServiceImpl executorService;

    @Mock
    private ExecutorMapper executorMapper;

    @Mock
    private RegionMapper regionMapper;

    private List<Executor> mockExecutors;
    private List<Region> mockRegions;

    @Before
    public void setUp() {
        // 准备测试数据
        mockExecutors = Arrays.asList(
            createMockExecutor(1L, "192.168.1.100", "执行器1", 1L),
            createMockExecutor(2L, "192.168.1.101", "执行器2", 2L),
            createMockExecutor(3L, "192.168.1.102", "执行器3", 3L)
        );

        mockRegions = Arrays.asList(
            createMockRegion(1L, "华北", 1L, 1),
            createMockRegion(2L, "华东", 1L, 1),
            createMockRegion(3L, "华南", 1L, 1)
        );
    }

    @Test
    public void testGetExecutorPageWithRegion_Success() {
        // 准备测试数据
        Integer current = 1;
        Integer size = 10;
        String name = "执行器";
        String ipAddress = "192.168";
        Long regionId = 1L;

        // Mock 分页结果
        Page<Executor> mockPage = PowerMockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(mockExecutors);
        when(mockPage.getTotal()).thenReturn(3L);
        when(mockPage.getCurrent()).thenReturn(current.longValue());
        when(mockPage.getSize()).thenReturn(size.longValue());

        // Mock Mapper 调用
        when(executorMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

        // 执行测试
        Page<ExecutorDTO> result = executorService.getExecutorPageWithRegion(current, size, name, ipAddress, regionId);

        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.getRecords().size());
        assertEquals(3L, result.getTotal());
        assertEquals(current.longValue(), result.getCurrent());
        assertEquals(size.longValue(), result.getSize());

        // 验证第一个执行器
        ExecutorDTO firstExecutor = result.getRecords().get(0);
        assertEquals("192.168.1.100", firstExecutor.getIpAddress());
        assertEquals("执行器1", firstExecutor.getName());
        assertEquals(Long.valueOf(1L), firstExecutor.getRegionId());
    }

    @Test
    public void testGetExecutorPageWithRegion_WithNameFilter() {
        // 准备测试数据
        Integer current = 1;
        Integer size = 10;
        String name = "执行器1";
        String ipAddress = null;
        Long regionId = null;

        // Mock 分页结果 - 只返回匹配名称的执行器
        List<Executor> filteredExecutors = Arrays.asList(mockExecutors.get(0));
        Page<Executor> mockPage = PowerMockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(filteredExecutors);
        when(mockPage.getTotal()).thenReturn(1L);
        when(mockPage.getCurrent()).thenReturn(current.longValue());
        when(mockPage.getSize()).thenReturn(size.longValue());

        // Mock Mapper 调用
        when(executorMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

        // 执行测试
        Page<ExecutorDTO> result = executorService.getExecutorPageWithRegion(current, size, name, ipAddress, regionId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());

        // 验证过滤后的执行器数据
        ExecutorDTO filteredExecutor = result.getRecords().get(0);
        assertEquals("执行器1", filteredExecutor.getName());
        assertEquals("192.168.1.100", filteredExecutor.getIpAddress());
    }

    @Test
    public void testGetExecutorPageWithRegion_WithIpAddressFilter() {
        // 准备测试数据
        Integer current = 1;
        Integer size = 10;
        String name = null;
        String ipAddress = "192.168.1.100";
        Long regionId = null;

        // Mock 分页结果 - 只返回匹配IP的执行器
        List<Executor> filteredExecutors = Arrays.asList(mockExecutors.get(0));
        Page<Executor> mockPage = PowerMockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(filteredExecutors);
        when(mockPage.getTotal()).thenReturn(1L);
        when(mockPage.getCurrent()).thenReturn(current.longValue());
        when(mockPage.getSize()).thenReturn(size.longValue());

        // Mock Mapper 调用
        when(executorMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

        // 执行测试
        Page<ExecutorDTO> result = executorService.getExecutorPageWithRegion(current, size, name, ipAddress, regionId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());

        // 验证过滤后的执行器数据
        ExecutorDTO filteredExecutor = result.getRecords().get(0);
        assertEquals("192.168.1.100", filteredExecutor.getIpAddress());
    }

    @Test
    public void testGetExecutorPageWithRegion_WithRegionIdFilter() {
        // 准备测试数据
        Integer current = 1;
        Integer size = 10;
        String name = null;
        String ipAddress = null;
        Long regionId = 1L;

        // Mock 分页结果 - 只返回匹配地域的执行器
        List<Executor> filteredExecutors = Arrays.asList(mockExecutors.get(0));
        Page<Executor> mockPage = PowerMockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(filteredExecutors);
        when(mockPage.getTotal()).thenReturn(1L);
        when(mockPage.getCurrent()).thenReturn(current.longValue());
        when(mockPage.getSize()).thenReturn(size.longValue());

        // Mock Mapper 调用
        when(executorMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

        // 执行测试
        Page<ExecutorDTO> result = executorService.getExecutorPageWithRegion(current, size, name, ipAddress, regionId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());

        // 验证过滤后的执行器数据
        ExecutorDTO filteredExecutor = result.getRecords().get(0);
        assertEquals(Long.valueOf(1L), filteredExecutor.getRegionId());
    }

    @Test
    public void testGetExecutorPageWithRegion_EmptyResult() {
        // 准备测试数据
        Integer current = 1;
        Integer size = 10;
        String name = "不存在的执行器";
        String ipAddress = null;
        Long regionId = null;

        // Mock 空分页结果
        Page<Executor> mockPage = PowerMockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(new ArrayList<>());
        when(mockPage.getTotal()).thenReturn(0L);
        when(mockPage.getCurrent()).thenReturn(current.longValue());
        when(mockPage.getSize()).thenReturn(size.longValue());

        // Mock Mapper 调用
        when(executorMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

        // 执行测试
        Page<ExecutorDTO> result = executorService.getExecutorPageWithRegion(current, size, name, ipAddress, regionId);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getRecords().size());
        assertEquals(0L, result.getTotal());
    }

    @Test
    public void testGetExecutorPageWithRegion_WithNullFilters() {
        // 准备测试数据 - 所有过滤器都为 null
        Integer current = 1;
        Integer size = 10;
        String name = null;
        String ipAddress = null;
        Long regionId = null;

        // Mock 分页结果
        Page<Executor> mockPage = PowerMockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(mockExecutors);
        when(mockPage.getTotal()).thenReturn(3L);
        when(mockPage.getCurrent()).thenReturn(current.longValue());
        when(mockPage.getSize()).thenReturn(size.longValue());

        // Mock Mapper 调用
        when(executorMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

        // 执行测试
        Page<ExecutorDTO> result = executorService.getExecutorPageWithRegion(current, size, name, ipAddress, regionId);

        // 验证结果 - 应该返回所有执行器
        assertNotNull(result);
        assertEquals(3, result.getRecords().size());
        assertEquals(3L, result.getTotal());
    }

    @Test
    public void testGetExecutorPageWithRegion_WithMultipleFilters() {
        // 准备测试数据 - 多个过滤器
        Integer current = 1;
        Integer size = 10;
        String name = "执行器";
        String ipAddress = "192.168.1";
        Long regionId = 1L;

        // Mock 分页结果 - 返回匹配多个条件的执行器
        List<Executor> filteredExecutors = Arrays.asList(mockExecutors.get(0));
        Page<Executor> mockPage = PowerMockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(filteredExecutors);
        when(mockPage.getTotal()).thenReturn(1L);
        when(mockPage.getCurrent()).thenReturn(current.longValue());
        when(mockPage.getSize()).thenReturn(size.longValue());

        // Mock Mapper 调用
        when(executorMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

        // 执行测试
        Page<ExecutorDTO> result = executorService.getExecutorPageWithRegion(current, size, name, ipAddress, regionId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());

        // 验证匹配多个条件的执行器
        ExecutorDTO filteredExecutor = result.getRecords().get(0);
        assertEquals("执行器1", filteredExecutor.getName());
        assertEquals("192.168.1.100", filteredExecutor.getIpAddress());
        assertEquals(Long.valueOf(1L), filteredExecutor.getRegionId());
    }

    // 辅助方法：创建 Mock Executor
    private Executor createMockExecutor(Long id, String ipAddress, String name, Long regionId) {
        Executor executor = new Executor();
        executor.setId(id);
        executor.setIpAddress(ipAddress);
        executor.setName(name);
        executor.setRegionId(regionId);
        executor.setStatus(1); // 在线状态
        return executor;
    }

    // 辅助方法：创建 Mock Region
    private Region createMockRegion(Long id, String name, Long parentId, Integer level) {
        Region regionObj = new Region();
        regionObj.setId(id);
        regionObj.setName(name);
        regionObj.setParentId(parentId);
        regionObj.setLevel(level);
        return regionObj;
    }
}
