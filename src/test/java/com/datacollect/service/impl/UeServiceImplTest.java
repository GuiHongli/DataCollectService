package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.entity.NetworkType;
import com.datacollect.entity.Ue;
import com.datacollect.entity.dto.UeDTO;
import com.datacollect.mapper.NetworkTypeMapper;
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
public class UeServiceImplTest {

    @InjectMocks
    private UeServiceImpl ueService;

    @Mock
    private UeMapper ueMapper;

    @Mock
    private NetworkTypeMapper networkTypeMapper;

    private List<Ue> mockUes;
    private List<NetworkType> mockNetworkTypes;

    @Before
    public void setUp() {
        // 准备测试数据
        mockUes = Arrays.asList(
            createMockUe(1L, "UE001", "测试UE1", "测试目的1", 1L, "华为", "COM1"),
            createMockUe(2L, "UE002", "测试UE2", "测试目的2", 2L, "中兴", "COM2"),
            createMockUe(3L, "UE003", "测试UE3", "测试目的3", 1L, "华为", "COM3")
        );

        mockNetworkTypes = Arrays.asList(
            createMockNetworkType(1L, "4G"),
            createMockNetworkType(2L, "5G")
        );
    }

    @Test
    public void testGetUePageWithNetworkType_Success() {
        // 准备测试数据
        Integer current = 1;
        Integer size = 10;
        String name = "测试";
        String ueId = "UE";
        String purpose = "测试";
        Long networkTypeId = 1L;

        // Mock 分页结果
        Page<Ue> mockPage = PowerMockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(mockUes);
        when(mockPage.getTotal()).thenReturn(3L);
        when(mockPage.getCurrent()).thenReturn(current.longValue());
        when(mockPage.getSize()).thenReturn(size.longValue());

        // Mock Mapper 调用
        when(ueMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);
        when(networkTypeMapper.selectList(any())).thenReturn(mockNetworkTypes);

        // 执行测试
        Page<UeDTO> result = ueService.getUePageWithNetworkType(current, size, name, ueId, purpose, networkTypeId);

        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.getRecords().size());
        assertEquals(3L, result.getTotal());
        assertEquals(current.longValue(), result.getCurrent());
        assertEquals(size.longValue(), result.getSize());

        // 验证第一个 UE 的数据
        UeDTO firstUe = result.getRecords().get(0);
        assertEquals("UE001", firstUe.getUeId());
        assertEquals("测试UE1", firstUe.getName());
        assertEquals("华为", firstUe.getVendorName());
        assertEquals("COM1", firstUe.getPort());
    }

    @Test
    public void testGetUePageWithNetworkType_WithFilters() {
        // 准备测试数据
        Integer current = 1;
        Integer size = 10;
        String name = "测试UE1";
        String ueId = "UE001";
        String purpose = "测试目的1";
        Long networkTypeId = 1L;

        // Mock 分页结果
        Page<Ue> mockPage = PowerMockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(Arrays.asList(mockUes.get(0)));
        when(mockPage.getTotal()).thenReturn(1L);
        when(mockPage.getCurrent()).thenReturn(current.longValue());
        when(mockPage.getSize()).thenReturn(size.longValue());

        // Mock Mapper 调用
        when(ueMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);
        when(networkTypeMapper.selectList(any())).thenReturn(mockNetworkTypes);

        // 执行测试
        Page<UeDTO> result = ueService.getUePageWithNetworkType(current, size, name, ueId, purpose, networkTypeId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());

        // 验证过滤后的 UE 数据
        UeDTO filteredUe = result.getRecords().get(0);
        assertEquals("UE001", filteredUe.getUeId());
        assertEquals("测试UE1", filteredUe.getName());
    }

    @Test
    public void testGetUePageWithNetworkType_EmptyResult() {
        // 准备测试数据
        Integer current = 1;
        Integer size = 10;
        String name = "不存在的UE";
        String ueId = "";
        String purpose = "";
        Long networkTypeId = null;

        // Mock 空分页结果
        Page<Ue> mockPage = PowerMockito.mock(Page.class);
        when(mockPage.getRecords()).thenReturn(new ArrayList<>());
        when(mockPage.getTotal()).thenReturn(0L);
        when(mockPage.getCurrent()).thenReturn(current.longValue());
        when(mockPage.getSize()).thenReturn(size.longValue());

        // Mock Mapper 调用
        when(ueMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

        // 执行测试
        Page<UeDTO> result = ueService.getUePageWithNetworkType(current, size, name, ueId, purpose, networkTypeId);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getRecords().size());
        assertEquals(0L, result.getTotal());
    }

    @Test
    public void testGetUeOptionsForSelect_Success() {
        // Mock 启用的 UEs
        List<Ue> enabledUes = Arrays.asList(
            createMockUe(1L, "UE001", "测试UE1", "测试目的1", 1L, "华为", "COM1"),
            createMockUe(2L, "UE002", "测试UE2", "测试目的2", 2L, "中兴", "COM2")
        );

        // Mock Mapper 调用
        when(ueMapper.selectList(any(QueryWrapper.class))).thenReturn(enabledUes);
        when(networkTypeMapper.selectList(any())).thenReturn(mockNetworkTypes);

        // 执行测试
        List<Map<String, Object>> result = ueService.getUeOptionsForSelect();

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());

        // 验证第一个选项
        Map<String, Object> firstOption = result.get(0);
        assertEquals(1L, firstOption.get("value"));
        assertEquals("UE001 - 测试UE1 (4G) - 华为", firstOption.get("label"));
        assertEquals("UE001", firstOption.get("ueId"));
        assertEquals("测试UE1", firstOption.get("name"));
        assertEquals("4G", firstOption.get("networkTypeName"));
        assertEquals("华为", firstOption.get("vendorName"));
        assertEquals("COM1", firstOption.get("port"));
    }

    @Test
    public void testGetUeOptionsForSelect_EmptyResult() {
        // Mock 空的 UE 列表
        when(ueMapper.selectList(any(QueryWrapper.class))).thenReturn(new ArrayList<>());

        // 执行测试
        List<Map<String, Object>> result = ueService.getUeOptionsForSelect();

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testGetUeOptionsForSelect_WithNullNetworkType() {
        // 准备测试数据 - 包含 null 网络类型
        List<Ue> uesWithNullNetwork = Arrays.asList(
            createMockUe(1L, "UE001", "测试UE1", "测试目的1", null, "华为", "COM1")
        );

        // Mock Mapper 调用
        when(ueMapper.selectList(any(QueryWrapper.class))).thenReturn(uesWithNullNetwork);
        when(networkTypeMapper.selectList(any())).thenReturn(mockNetworkTypes);

        // 执行测试
        List<Map<String, Object>> result = ueService.getUeOptionsForSelect();

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());

        // 验证网络类型为 null 的情况
        Map<String, Object> option = result.get(0);
        assertEquals("未知", option.get("networkTypeName"));
    }

    // 辅助方法：创建 Mock UE
    private Ue createMockUe(Long id, String ueId, String name, String purpose, Long networkTypeId, String vendor, String port) {
        Ue ue = new Ue();
        ue.setId(id);
        ue.setUeId(ueId);
        ue.setName(name);
        ue.setPurpose(purpose);
        ue.setNetworkTypeId(networkTypeId);
        ue.setVendor(vendor);
        ue.setPort(port);
        ue.setStatus(1); // 可用状态
        return ue;
    }

    // 辅助方法：创建 Mock NetworkType
    private NetworkType createMockNetworkType(Long id, String name) {
        NetworkType networkType = new NetworkType();
        networkType.setId(id);
        networkType.setName(name);
        return networkType;
    }
}
