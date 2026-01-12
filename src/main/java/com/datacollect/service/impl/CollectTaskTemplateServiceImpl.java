package com.datacollect.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.dto.CollectTaskTemplateRequest;
import com.datacollect.entity.CollectTaskTemplate;
import com.datacollect.mapper.CollectTaskTemplateMapper;
import com.datacollect.service.CollectTaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 采集任务模版服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class CollectTaskTemplateServiceImpl extends ServiceImpl<CollectTaskTemplateMapper, CollectTaskTemplate> implements CollectTaskTemplateService {
    
    @Override
    public Long createTemplate(CollectTaskTemplateRequest request, String createBy) {
        log.info("创建采集任务模版 - 模版名称: {}, 采集策略ID: {}, 创建人: {}", 
                request.getName(), request.getCollectStrategyId(), createBy);
        
        CollectTaskTemplate template = new CollectTaskTemplate();
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        
        // 将网元ID列表转换为JSON字符串存储
        if (request.getNetworkElementIds() != null && !request.getNetworkElementIds().isEmpty()) {
            template.setNetworkElementIds(JSON.toJSONString(request.getNetworkElementIds()));
        }
        
        template.setCollectStrategyId(request.getCollectStrategyId());
        template.setCollectCount(request.getCollectCount());
        template.setRegionId(request.getRegionId());
        template.setCountryId(request.getCountryId());
        template.setProvinceId(request.getProvinceId());
        template.setCityId(request.getCityId());
        template.setNetwork(request.getNetwork());
        
        // 将厂商列表转换为JSON字符串存储
        if (request.getManufacturer() != null && !request.getManufacturer().isEmpty()) {
            template.setManufacturer(JSON.toJSONString(request.getManufacturer()));
        }
        
        // 将逻辑环境ID列表转换为JSON字符串存储
        if (request.getLogicEnvironmentIds() != null && !request.getLogicEnvironmentIds().isEmpty()) {
            template.setLogicEnvironmentIds(JSON.toJSONString(request.getLogicEnvironmentIds()));
        }
        
        template.setTaskCustomParams(request.getTaskCustomParams());
        template.setCustomParams(request.getCustomParams());
        template.setCreateBy(createBy);
        template.setDeleted(0);
        
        LocalDateTime now = LocalDateTime.now();
        template.setCreateTime(now);
        template.setUpdateTime(now);
        
        boolean success = save(template);
        if (success) {
            log.info("采集任务模版创建成功 - 模版ID: {}", template.getId());
            return template.getId();
        } else {
            log.error("采集任务模版创建失败 - 模版名称: {}", request.getName());
            throw new RuntimeException("采集任务模版创建失败");
        }
    }
    
    @Override
    public Boolean updateTemplate(CollectTaskTemplateRequest request) {
        log.info("更新采集任务模版 - 模版ID: {}, 模版名称: {}", request.getId(), request.getName());
        
        if (request.getId() == null) {
            log.error("更新采集任务模版失败 - 模版ID不能为空");
            throw new RuntimeException("模版ID不能为空");
        }
        
        CollectTaskTemplate template = getById(request.getId());
        if (template == null) {
            log.error("更新采集任务模版失败 - 模版不存在，模版ID: {}", request.getId());
            throw new RuntimeException("模版不存在");
        }
        
        if (template.getDeleted() != null && template.getDeleted() == 1) {
            log.error("更新采集任务模版失败 - 模版已删除，模版ID: {}", request.getId());
            throw new RuntimeException("模版已删除");
        }
        
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        
        // 将网元ID列表转换为JSON字符串存储
        if (request.getNetworkElementIds() != null && !request.getNetworkElementIds().isEmpty()) {
            template.setNetworkElementIds(JSON.toJSONString(request.getNetworkElementIds()));
        } else {
            template.setNetworkElementIds(null);
        }
        
        template.setCollectStrategyId(request.getCollectStrategyId());
        template.setCollectCount(request.getCollectCount());
        template.setRegionId(request.getRegionId());
        template.setCountryId(request.getCountryId());
        template.setProvinceId(request.getProvinceId());
        template.setCityId(request.getCityId());
        template.setNetwork(request.getNetwork());
        
        // 将厂商列表转换为JSON字符串存储
        if (request.getManufacturer() != null && !request.getManufacturer().isEmpty()) {
            template.setManufacturer(JSON.toJSONString(request.getManufacturer()));
        } else {
            template.setManufacturer(null);
        }
        
        // 将逻辑环境ID列表转换为JSON字符串存储
        if (request.getLogicEnvironmentIds() != null && !request.getLogicEnvironmentIds().isEmpty()) {
            template.setLogicEnvironmentIds(JSON.toJSONString(request.getLogicEnvironmentIds()));
        } else {
            template.setLogicEnvironmentIds(null);
        }
        
        template.setTaskCustomParams(request.getTaskCustomParams());
        template.setCustomParams(request.getCustomParams());
        template.setUpdateTime(LocalDateTime.now());
        
        boolean success = updateById(template);
        if (success) {
            log.info("采集任务模版更新成功 - 模版ID: {}", template.getId());
            return true;
        } else {
            log.error("采集任务模版更新失败 - 模版ID: {}", request.getId());
            throw new RuntimeException("采集任务模版更新失败");
        }
    }
}

