package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.dto.CollectTaskTemplateRequest;
import com.datacollect.entity.CollectTaskTemplate;
import com.datacollect.service.CollectTaskTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 采集任务模版控制器
 * 
 * @author system
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/collect-task-template")
@Validated
public class CollectTaskTemplateController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectTaskTemplateController.class);
    
    @Autowired
    private CollectTaskTemplateService collectTaskTemplateService;
    
    /**
     * 创建采集任务模版
     * 
     * @param request 模版请求
     * @param httpRequest HTTP请求对象，用于获取当前用户信息
     * @return 模版ID
     */
    @PostMapping
    public Result<Map<String, Object>> createTemplate(@Valid @RequestBody CollectTaskTemplateRequest request, HttpServletRequest httpRequest) {
        LOGGER.info("创建采集任务模版 - 模版名称: {}, 采集策略ID: {}", request.getName(), request.getCollectStrategyId());
        
        try {
            // 从请求中get当前用户名（create人）
            String createBy = (String) httpRequest.getAttribute("username");
            LOGGER.info("创建采集任务模版 - createBy: {}", createBy);
            
            // 调用服务create模版
            Long templateId = collectTaskTemplateService.createTemplate(request, createBy);
            
            Map<String, Object> result = new HashMap<>();
            result.put("templateId", templateId);
            result.put("message", "采集任务模版创建成功");
            result.put("timestamp", System.currentTimeMillis());
            
            LOGGER.info("采集任务模版createsuccess - 模版ID: {}, createBy: {}", templateId, createBy);
            return Result.success(result);
            
        } catch (Exception e) {
            LOGGER.error("create采集任务模版failed - 模版名称: {}, error: {}", request.getName(), e.getMessage(), e);
            return Result.error("创建采集任务模版失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新采集任务模版
     * 
     * @param request 模版请求
     * @return 更新结果
     */
    @PutMapping
    public Result<Boolean> updateTemplate(@Valid @RequestBody CollectTaskTemplateRequest request) {
        LOGGER.info("update采集任务模版 - 模版ID: {}, 模版名称: {}", request.getId(), request.getName());
        
        try {
            Boolean success = collectTaskTemplateService.updateTemplate(request);
            LOGGER.info("采集任务模版updatesuccess - 模版ID: {}", request.getId());
            return Result.success(success);
            
        } catch (Exception e) {
            LOGGER.error("update采集任务模版failed - 模版ID: {}, error: {}", request.getId(), e.getMessage(), e);
            return Result.error("更新采集任务模版失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除采集任务模版（逻辑删除）
     * 
     * @param id 模版ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteTemplate(@PathVariable @NotNull Long id) {
        LOGGER.info("delete采集任务模版 - 模版ID: {}", id);
        
        try {
            CollectTaskTemplate template = collectTaskTemplateService.getById(id);
            if (template == null) {
                return Result.error("模版不存在");
            }
            
            template.setDeleted(1);
            boolean success = collectTaskTemplateService.updateById(template);
            
            LOGGER.info("采集任务模版deletesuccess - 模版ID: {}", id);
            return Result.success(success);
            
        } catch (Exception e) {
            LOGGER.error("delete采集任务模版failed - 模版ID: {}, error: {}", id, e.getMessage(), e);
            return Result.error("删除采集任务模版失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取采集任务模版
     * 
     * @param id 模版ID
     * @return 模版信息
     */
    @GetMapping("/{id}")
    public Result<CollectTaskTemplate> getById(@PathVariable @NotNull Long id) {
        LOGGER.info("get采集任务模版 - 模版ID: {}", id);
        
        try {
            CollectTaskTemplate template = collectTaskTemplateService.getById(id);
            if (template == null || (template.getDeleted() != null && template.getDeleted() == 1)) {
                return Result.error("模版不存在");
            }
            
            return Result.success(template);
            
        } catch (Exception e) {
            LOGGER.error("get采集任务模版failed - 模版ID: {}, error: {}", id, e.getMessage(), e);
            return Result.error("获取采集任务模版失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页查询采集任务模版列表
     * 
     * @param current 当前页
     * @param size 每页大小
     * @param name 模版名称（模糊查询）
     * @param httpRequest HTTP请求对象
     * @return 模版列表
     */
    @GetMapping("/page")
    public Result<Page<CollectTaskTemplate>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            HttpServletRequest httpRequest) {
        
        LOGGER.info("分页查询采集任务模版列表 - 当前页: {}, 每页大小: {}, 模版名称: {}", current, size, name);
        
        try {
            Page<CollectTaskTemplate> page = new Page<>(current, size);
            QueryWrapper<CollectTaskTemplate> queryWrapper = new QueryWrapper<>();
            
            // 只query未delete的模版
            queryWrapper.eq("deleted", 0);
            
            // 获取当前用户信息
            String role = (String) httpRequest.getAttribute("role");
            String username = (String) httpRequest.getAttribute("username");
            
            // 根据用户角色过滤数据
            // admin 可以查看全部模版，普通用户只能查看自己创建的模版
            if (role != null && !"admin".equals(role) && username != null) {
                queryWrapper.eq("create_by", username);
                LOGGER.debug("普通用户query采集任务模版 - 用户名: {}, 只能查看自己create的模版", username);
            } else {
                LOGGER.debug("管理员query采集任务模版 - 角色: {}, 可以查看全部模版", role);
            }
            
            if (name != null && !name.isEmpty()) {
                queryWrapper.like("name", name);
            }
            
            queryWrapper.orderByDesc("create_time");
            Page<CollectTaskTemplate> result = collectTaskTemplateService.page(page, queryWrapper);
            
            return Result.success(result);
            
        } catch (Exception e) {
            LOGGER.error("分页query采集任务模版列表failed - error: {}", e.getMessage(), e);
            return Result.error("分页查询采集任务模版列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有采集任务模版列表
     * 
     * @param httpRequest HTTP请求对象
     * @return 模版列表
     */
    @GetMapping("/list")
    public Result<List<CollectTaskTemplate>> list(HttpServletRequest httpRequest) {
        LOGGER.info("获取所有采集任务模版列表");
        
        try {
            QueryWrapper<CollectTaskTemplate> queryWrapper = new QueryWrapper<>();
            
            // 只query未delete的模版
            queryWrapper.eq("deleted", 0);
            
            // 获取当前用户信息
            String role = (String) httpRequest.getAttribute("role");
            String username = (String) httpRequest.getAttribute("username");
            
            // 根据用户角色过滤数据
            // admin 可以查看全部模版，普通用户只能查看自己创建的模版
            if (role != null && !"admin".equals(role) && username != null) {
                queryWrapper.eq("create_by", username);
                LOGGER.debug("普通用户query采集任务模版列表 - 用户名: {}, 只能查看自己create的模版", username);
            } else {
                LOGGER.debug("管理员query采集任务模版列表 - 角色: {}, 可以查看全部模版", role);
            }
            
            queryWrapper.orderByDesc("create_time");
            List<CollectTaskTemplate> list = collectTaskTemplateService.list(queryWrapper);
            
            return Result.success(list);
            
        } catch (Exception e) {
            LOGGER.error("get采集任务模版列表failed - error: {}", e.getMessage(), e);
            return Result.error("获取采集任务模版列表失败: " + e.getMessage());
        }
    }
}

