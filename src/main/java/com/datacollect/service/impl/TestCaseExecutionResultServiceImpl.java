package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.dto.TestCaseExecutionResult;
import com.datacollect.mapper.TestCaseExecutionResultMapper;
import com.datacollect.service.TestCaseExecutionResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用例执行结果服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class TestCaseExecutionResultServiceImpl extends ServiceImpl<TestCaseExecutionResultMapper, com.datacollect.entity.TestCaseExecutionResult> implements TestCaseExecutionResultService {

    @Override
    public boolean saveTestCaseExecutionResult(TestCaseExecutionResult result) {
        log.info("保存用例执行结果 - 任务ID: {}, 用例ID: {}, 轮次: {}, 状态: {}", 
                result.getTaskId(), result.getTestCaseId(), result.getRound(), result.getStatus());
        
        try {
            com.datacollect.entity.TestCaseExecutionResult entity = new com.datacollect.entity.TestCaseExecutionResult();
            BeanUtils.copyProperties(result, entity);
            
            // 设置创建和更新时间
            LocalDateTime now = LocalDateTime.now();
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            
            boolean success = save(entity);
            if (success) {
                log.info("用例执行结果保存成功 - 任务ID: {}, 用例ID: {}, 轮次: {}", 
                        result.getTaskId(), result.getTestCaseId(), result.getRound());
            } else {
                log.error("用例执行结果保存失败 - 任务ID: {}, 用例ID: {}, 轮次: {}", 
                        result.getTaskId(), result.getTestCaseId(), result.getRound());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("保存用例执行结果异常 - 任务ID: {}, 用例ID: {}, 轮次: {}, 错误: {}", 
                    result.getTaskId(), result.getTestCaseId(), result.getRound(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<com.datacollect.entity.TestCaseExecutionResult> getByTaskId(String taskId) {
        log.debug("根据任务ID查询执行结果 - 任务ID: {}", taskId);
        
        QueryWrapper<com.datacollect.entity.TestCaseExecutionResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId);
        queryWrapper.orderByDesc("create_time");
        
        List<com.datacollect.entity.TestCaseExecutionResult> results = list(queryWrapper);
        log.debug("查询到执行结果数量: {}", results.size());
        
        return results;
    }

    @Override
    public List<com.datacollect.entity.TestCaseExecutionResult> getByTestCaseId(Long testCaseId) {
        log.debug("根据用例ID查询执行结果 - 用例ID: {}", testCaseId);
        
        QueryWrapper<com.datacollect.entity.TestCaseExecutionResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("test_case_id", testCaseId);
        queryWrapper.orderByDesc("create_time");
        
        List<com.datacollect.entity.TestCaseExecutionResult> results = list(queryWrapper);
        log.debug("查询到执行结果数量: {}", results.size());
        
        return results;
    }
}
