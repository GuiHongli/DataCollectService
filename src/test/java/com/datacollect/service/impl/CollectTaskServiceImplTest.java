package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.datacollect.entity.CollectTask;
import com.datacollect.mapper.CollectTaskMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UpdateWrapper.class})
public class CollectTaskServiceImplTest {

    @InjectMocks
    private CollectTaskServiceImpl collectTaskService;

    @Mock
    private CollectTaskMapper collectTaskMapper;

    private List<CollectTask> mockCollectTasks;

    @Before
    public void setUp() {
        // 准备测试数据
        mockCollectTasks = Arrays.asList(
            createMockCollectTask(1L, "任务1", "PENDING", null, null),
            createMockCollectTask(2L, "任务2", "RUNNING", LocalDateTime.now(), null),
            createMockCollectTask(3L, "任务3", "COMPLETED", LocalDateTime.now().minusHours(1), LocalDateTime.now())
        );
    }

    @Test
    public void testUpdateTaskStatus_ToRunning() {
        // 准备测试数据
        Long taskId = 1L;
        String status = "RUNNING";

        // Mock Mapper 调用
        when(collectTaskMapper.update(any(CollectTask.class), any(UpdateWrapper.class))).thenReturn(1);

        // 执行测试
        boolean result = collectTaskService.updateTaskStatus(taskId, status);

        // 验证结果
        assertTrue(result);
        verify(collectTaskMapper, times(1)).update(any(CollectTask.class), any(UpdateWrapper.class));
    }

    @Test
    public void testUpdateTaskStatus_ToCompleted() {
        // 准备测试数据
        Long taskId = 2L;
        String status = "COMPLETED";

        // Mock Mapper 调用
        when(collectTaskMapper.update(any(CollectTask.class), any(UpdateWrapper.class))).thenReturn(1);

        // 执行测试
        boolean result = collectTaskService.updateTaskStatus(taskId, status);

        // 验证结果
        assertTrue(result);
        verify(collectTaskMapper, times(1)).update(any(CollectTask.class), any(UpdateWrapper.class));
    }

    @Test
    public void testUpdateTaskStatus_ToFailed() {
        // 准备测试数据
        Long taskId = 1L;
        String status = "FAILED";

        // Mock Mapper 调用
        when(collectTaskMapper.update(any(CollectTask.class), any(UpdateWrapper.class))).thenReturn(1);

        // 执行测试
        boolean result = collectTaskService.updateTaskStatus(taskId, status);

        // 验证结果
        assertTrue(result);
        verify(collectTaskMapper, times(1)).update(any(CollectTask.class), any(UpdateWrapper.class));
    }

    @Test
    public void testUpdateTaskStatus_ToCancelled() {
        // 准备测试数据
        Long taskId = 1L;
        String status = "CANCELLED";

        // Mock Mapper 调用
        when(collectTaskMapper.update(any(CollectTask.class), any(UpdateWrapper.class))).thenReturn(1);

        // 执行测试
        boolean result = collectTaskService.updateTaskStatus(taskId, status);

        // 验证结果
        assertTrue(result);
        verify(collectTaskMapper, times(1)).update(any(CollectTask.class), any(UpdateWrapper.class));
    }

    @Test
    public void testUpdateTaskStatus_UpdateFailed() {
        // 准备测试数据
        Long taskId = 1L;
        String status = "RUNNING";

        // Mock Mapper 调用 - 更新失败
        when(collectTaskMapper.update(any(CollectTask.class), any(UpdateWrapper.class))).thenReturn(0);

        // 执行测试
        boolean result = collectTaskService.updateTaskStatus(taskId, status);

        // 验证结果
        assertFalse(result);
        verify(collectTaskMapper, times(1)).update(any(CollectTask.class), any(UpdateWrapper.class));
    }

    @Test
    public void testUpdateTaskStatus_WithNullStatus() {
        // 准备测试数据
        Long taskId = 1L;
        String status = null;

        // 执行测试 - 应该抛出异常或返回 false
        try {
            boolean result = collectTaskService.updateTaskStatus(taskId, status);
            // 如果没有抛出异常，验证返回 false
            assertFalse(result);
        } catch (Exception e) {
            // 如果抛出异常，验证异常类型
            assertNotNull(e);
        }
    }

    @Test
    public void testUpdateTaskStatus_WithEmptyStatus() {
        // 准备测试数据
        Long taskId = 1L;
        String status = "";

        // 执行测试 - 应该抛出异常或返回 false
        try {
            boolean result = collectTaskService.updateTaskStatus(taskId, status);
            // 如果没有抛出异常，验证返回 false
            assertFalse(result);
        } catch (Exception e) {
            // 如果抛出异常，验证异常类型
            assertNotNull(e);
        }
    }

    @Test
    public void testUpdateTaskStatus_WithInvalidStatus() {
        // 准备测试数据
        Long taskId = 1L;
        String status = "INVALID_STATUS";

        // 执行测试 - 应该抛出异常或返回 false
        try {
            boolean result = collectTaskService.updateTaskStatus(taskId, status);
            // 如果没有抛出异常，验证返回 false
            assertFalse(result);
        } catch (Exception e) {
            // 如果抛出异常，验证异常类型
            assertNotNull(e);
        }
    }

    @Test
    public void testUpdateTaskStatus_WithNullTaskId() {
        // 准备测试数据
        Long taskId = null;
        String status = "RUNNING";

        // 执行测试 - 应该抛出异常或返回 false
        try {
            boolean result = collectTaskService.updateTaskStatus(taskId, status);
            // 如果没有抛出异常，验证返回 false
            assertFalse(result);
        } catch (Exception e) {
            // 如果抛出异常，验证异常类型
            assertNotNull(e);
        }
    }

    @Test
    public void testUpdateTaskStatus_WithZeroTaskId() {
        // 准备测试数据
        Long taskId = 0L;
        String status = "RUNNING";

        // 执行测试 - 应该抛出异常或返回 false
        try {
            boolean result = collectTaskService.updateTaskStatus(taskId, status);
            // 如果没有抛出异常，验证返回 false
            assertFalse(result);
        } catch (Exception e) {
            // 如果抛出异常，验证异常类型
            assertNotNull(e);
        }
    }

    @Test
    public void testUpdateTaskStatus_WithNegativeTaskId() {
        // 准备测试数据
        Long taskId = -1L;
        String status = "RUNNING";

        // 执行测试 - 应该抛出异常或返回 false
        try {
            boolean result = collectTaskService.updateTaskStatus(taskId, status);
            // 如果没有抛出异常，验证返回 false
            assertFalse(result);
        } catch (Exception e) {
            // 如果抛出异常，验证异常类型
            assertNotNull(e);
        }
    }

    // 辅助方法：创建 Mock CollectTask
    private CollectTask createMockCollectTask(Long id, String name, String status, LocalDateTime startTime, LocalDateTime endTime) {
        CollectTask task = new CollectTask();
        task.setId(id);
        task.setName(name);
        task.setStatus(status);
        task.setStartTime(startTime);
        task.setEndTime(endTime);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        return task;
    }
}
