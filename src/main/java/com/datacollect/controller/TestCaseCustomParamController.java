package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datacollect.common.Result;
import com.datacollect.dto.TestCaseCustomParamDTO;
import com.datacollect.entity.TestCaseCustomParam;
import com.datacollect.service.TestCaseCustomParamService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/test-case-custom-param")
@Validated
public class TestCaseCustomParamController {

    @Autowired
    private TestCaseCustomParamService testCaseCustomParamService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 新增用例自定义参数
     */
    @PostMapping
    public Result<TestCaseCustomParamDTO> create(@Valid @RequestBody TestCaseCustomParamDTO dto) {
        try {
            TestCaseCustomParam entity = convertDTOToEntity(dto);
            testCaseCustomParamService.save(entity);
            
            TestCaseCustomParamDTO result = convertEntityToDTO(entity);
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建用例自定义参数失败", e);
            return Result.error("创建用例自定义参数失败: " + e.getMessage());
        }
    }

    /**
     * 更新用例自定义参数
     */
    @PutMapping("/{id}")
    public Result<TestCaseCustomParamDTO> update(
            @PathVariable @NotNull Long id,
            @Valid @RequestBody TestCaseCustomParamDTO dto) {
        try {
            TestCaseCustomParam entity = convertDTOToEntity(dto);
            entity.setId(id);
            testCaseCustomParamService.updateById(entity);
            
            TestCaseCustomParamDTO result = convertEntityToDTO(entity);
            return Result.success(result);
        } catch (Exception e) {
            log.error("更新用例自定义参数失败", e);
            return Result.error("更新用例自定义参数失败: " + e.getMessage());
        }
    }

    /**
     * 删除用例自定义参数
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable @NotNull Long id) {
        try {
            boolean result = testCaseCustomParamService.removeById(id);
            return Result.success(result);
        } catch (Exception e) {
            log.error("删除用例自定义参数失败", e);
            return Result.error("删除用例自定义参数失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取用例自定义参数
     */
    @GetMapping("/{id}")
    public Result<TestCaseCustomParamDTO> getById(@PathVariable @NotNull Long id) {
        try {
            TestCaseCustomParam entity = testCaseCustomParamService.getById(id);
            if (entity == null) {
                return Result.error("用例自定义参数不存在");
            }
            TestCaseCustomParamDTO result = convertEntityToDTO(entity);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取用例自定义参数失败", e);
            return Result.error("获取用例自定义参数失败: " + e.getMessage());
        }
    }

    /**
     * 获取用例自定义参数列表
     */
    @GetMapping("/list")
    public Result<List<TestCaseCustomParamDTO>> list() {
        try {
            QueryWrapper<TestCaseCustomParam> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByDesc("create_time");
            List<TestCaseCustomParam> entities = testCaseCustomParamService.list(queryWrapper);
            
            List<TestCaseCustomParamDTO> result = entities.stream()
                    .map(this::convertEntityToDTO)
                    .collect(Collectors.toList());
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取用例自定义参数列表失败", e);
            return Result.error("获取用例自定义参数列表失败: " + e.getMessage());
        }
    }

    /**
     * 将DTO转换为Entity
     */
    private TestCaseCustomParam convertDTOToEntity(TestCaseCustomParamDTO dto) throws Exception {
        TestCaseCustomParam entity = new TestCaseCustomParam();
        entity.setId(dto.getId());
        entity.setBusinessCategory(dto.getBusinessCategory());
        entity.setApp(dto.getApp());
        entity.setParamName(dto.getParamName());
        
        // 将参数值列表转换为JSON字符串
        String paramValuesJson = objectMapper.writeValueAsString(dto.getParamValues());
        entity.setParamValues(paramValuesJson);
        
        return entity;
    }

    /**
     * 将Entity转换为DTO
     */
    private TestCaseCustomParamDTO convertEntityToDTO(TestCaseCustomParam entity) {
        try {
            TestCaseCustomParamDTO dto = new TestCaseCustomParamDTO();
            dto.setId(entity.getId());
            dto.setBusinessCategory(entity.getBusinessCategory());
            dto.setApp(entity.getApp());
            dto.setParamName(entity.getParamName());
            
            // 将JSON字符串转换为参数值列表
            List<String> paramValues = objectMapper.readValue(
                    entity.getParamValues(),
                    new TypeReference<List<String>>() {}
            );
            dto.setParamValues(paramValues);
            
            return dto;
        } catch (Exception e) {
            log.error("转换Entity到DTO失败", e);
            // 如果转换失败，返回空列表
            TestCaseCustomParamDTO dto = new TestCaseCustomParamDTO();
            dto.setId(entity.getId());
            dto.setBusinessCategory(entity.getBusinessCategory());
            dto.setApp(entity.getApp());
            dto.setParamName(entity.getParamName());
            dto.setParamValues(java.util.Collections.emptyList());
            return dto;
        }
    }
}


