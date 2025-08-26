package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.dto.CollectStrategyDTO;
import com.datacollect.entity.CollectStrategy;
import com.datacollect.service.CollectStrategyService;
import com.datacollect.enums.CollectIntentEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/collect-strategy")
@Validated
public class CollectStrategyController {

    @Autowired
    private CollectStrategyService collectStrategyService;

    @PostMapping
    public Result<CollectStrategy> create(@Valid @RequestBody CollectStrategy collectStrategy) {
        collectStrategyService.save(collectStrategy);
        return Result.success(collectStrategy);
    }

    @PutMapping("/{id}")
    public Result<CollectStrategy> update(@PathVariable @NotNull Long id, @Valid @RequestBody CollectStrategy collectStrategy) {
        collectStrategy.setId(id);
        collectStrategyService.updateById(collectStrategy);
        return Result.success(collectStrategy);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable @NotNull Long id) {
        boolean result = collectStrategyService.removeById(id);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<CollectStrategy> getById(@PathVariable @NotNull Long id) {
        CollectStrategy collectStrategy = collectStrategyService.getById(id);
        return Result.success(collectStrategy);
    }

    @GetMapping("/page")
    public Result<Page<CollectStrategyDTO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long testCaseSetId) {
        
        Page<CollectStrategy> page = new Page<>(current, size);
        Page<CollectStrategyDTO> result = collectStrategyService.pageWithTestCaseSet(page, name, testCaseSetId);
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result<List<CollectStrategyDTO>> list() {
        List<CollectStrategyDTO> list = collectStrategyService.listWithTestCaseSet();
        return Result.success(list);
    }

    @GetMapping("/test-case-set/{testCaseSetId}")
    public Result<List<CollectStrategyDTO>> getByTestCaseSetId(@PathVariable @NotNull Long testCaseSetId) {
        QueryWrapper<CollectStrategy> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("test_case_set_id", testCaseSetId);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("name");
        List<CollectStrategy> strategies = collectStrategyService.list(queryWrapper);
        List<CollectStrategyDTO> dtoList = strategies.stream()
                .map(strategy -> {
                    CollectStrategyDTO dto = new CollectStrategyDTO();
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
                    return dto;
                })
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    @GetMapping("/intents")
    public Result<List<Map<String, String>>> getIntents() {
        List<Map<String, String>> intents = new ArrayList<>();
        for (CollectIntentEnum intent : CollectIntentEnum.values()) {
            Map<String, String> intentMap = new HashMap<>();
            intentMap.put("code", intent.getCode());
            intentMap.put("name", intent.getName());
            intents.add(intentMap);
        }
        return Result.success(intents);
    }
}
