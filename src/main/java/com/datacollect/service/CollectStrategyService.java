package com.datacollect.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.dto.CollectStrategyDTO;
import com.datacollect.entity.CollectStrategy;

import java.util.List;

public interface CollectStrategyService extends IService<CollectStrategy> {

    /**
     * 分页查询采集策略（包含用例集详细信息）
     */
    Page<CollectStrategyDTO> pageWithTestCaseSet(Page<CollectStrategy> page, String name, Long testCaseSetId);

    /**
     * 获取采集策略列表（包含用例集详细信息）
     */
    List<CollectStrategyDTO> listWithTestCaseSet();
}
