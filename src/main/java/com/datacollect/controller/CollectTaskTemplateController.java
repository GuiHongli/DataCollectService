package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.dto.CollectTaskTemplateRequest;
import com.datacollect.entity.CollectTaskTemplate;
import com.datacollect.service.CollectTaskTemplateService;
import lombok.extern.slf4j.Slf4j;
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

/**
 * 采集任务模版控制器
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/collect-task-template")
@Validated
public class CollectTaskTemplateController {
    
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
        log.info("创建采集任务模版 - 模版名称: {}, 采集策略ID: {}", request.getName(), request.getCollectStrategyId());
        
        try {
            // 从请求中获取当前用户名（创建人）
            String createBy = (String) httpRequest.getAttribute("username");
            log.info("创建采集任务模版 - createBy: {}", createBy);
            
            // 调用服务创建模版
            Long templateId = collectTaskTemplateService.createTemplate(request, createBy);
            
            Map<String, Object> result = new HashMap<>();
            result.put("templateId", templateId);
            result.put("message", "采集任务模版创建成功");
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("采集任务模版创建成功 - 模版ID: {}, createBy: {}", templateId, createBy);
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("创建采集任务模版失败 - 模版名称: {}, 错误: {}", request.getName(), e.getMessage(), e);
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
        log.info("更新采集任务模版 - 模版ID: {}, 模版名称: {}", request.getId(), request.getName());
        
        try {
            Boolean success = collectTaskTemplateService.updateTemplate(request);
            log.info("采集任务模版更新成功 - 模版ID: {}", request.getId());
            return Result.success(success);
            
        } catch (Exception e) {
            log.error("更新采集任务模版失败 - 模版ID: {}, 错误: {}", request.getId(), e.getMessage(), e);
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
        log.info("删除采集任务模版 - 模版ID: {}", id);
        
        try {
            CollectTaskTemplate template = collectTaskTemplateService.getById(id);
            if (template == null) {
                return Result.error("模版不存在");
            }
            
            template.setDeleted(1);
            boolean success = collectTaskTemplateService.updateById(template);
            
            log.info("采集任务模版删除成功 - 模版ID: {}", id);
            return Result.success(success);
            
        } catch (Exception e) {
            log.error("删除采集任务模版失败 - 模版ID: {}, 错误: {}", id, e.getMessage(), e);
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
        log.info("获取采集任务模版 - 模版ID: {}", id);
        
        try {
            CollectTaskTemplate template = collectTaskTemplateService.getById(id);
            if (template == null || (template.getDeleted() != null && template.getDeleted() == 1)) {
                return Result.error("模版不存在");
            }
            
            return Result.success(template);
            
        } catch (Exception e) {
            log.error("获取采集任务模版失败 - 模版ID: {}, 错误: {}", id, e.getMessage(), e);
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
        
        log.info("分页查询采集任务模版列表 - 当前页: {}, 每页大小: {}, 模版名称: {}", current, size, name);
        
        try {
            Page<CollectTaskTemplate> page = new Page<>(current, size);
            QueryWrapper<CollectTaskTemplate> queryWrapper = new QueryWrapper<>();
            
            // 只查询未删除的模版
            queryWrapper.eq("deleted", 0);
            
            // 获取当前用户信息
            String role = (String) httpRequest.getAttribute("role");
            String username = (String) httpRequest.getAttribute("username");
            
            // 根据用户角色过滤数据
            // admin 可以查看全部模版，普通用户只能查看自己创建的模版
            if (role != null && !"admin".equals(role) && username != null) {
                queryWrapper.eq("create_by", username);
                log.debug("普通用户查询采集任务模版 - 用户名: {}, 只能查看自己创建的模版", username);
            } else {
                log.debug("管理员查询采集任务模版 - 角色: {}, 可以查看全部模版", role);
            }
            
            if (name != null && !name.isEmpty()) {
                queryWrapper.like("name", name);
            }
            
            queryWrapper.orderByDesc("create_time");
            Page<CollectTaskTemplate> result = collectTaskTemplateService.page(page, queryWrapper);
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("分页查询采集任务模版列表失败 - 错误: {}", e.getMessage(), e);
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
        log.info("获取所有采集任务模版列表");
        
        try {
            QueryWrapper<CollectTaskTemplate> queryWrapper = new QueryWrapper<>();
            
            // 只查询未删除的模版
            queryWrapper.eq("deleted", 0);
            
            // 获取当前用户信息
            String role = (String) httpRequest.getAttribute("role");
            String username = (String) httpRequest.getAttribute("username");
            
            // 根据用户角色过滤数据
            // admin 可以查看全部模版，普通用户只能查看自己创建的模版
            if (role != null && !"admin".equals(role) && username != null) {
                queryWrapper.eq("create_by", username);
                log.debug("普通用户查询采集任务模版列表 - 用户名: {}, 只能查看自己创建的模版", username);
            } else {
                log.debug("管理员查询采集任务模版列表 - 角色: {}, 可以查看全部模版", role);
            }
            
            queryWrapper.orderByDesc("create_time");
            List<CollectTaskTemplate> list = collectTaskTemplateService.list(queryWrapper);
            
            return Result.success(list);
            
        } catch (Exception e) {
            log.error("获取采集任务模版列表失败 - 错误: {}", e.getMessage(), e);
            return Result.error("获取采集任务模版列表失败: " + e.getMessage());
        }
    }
}

