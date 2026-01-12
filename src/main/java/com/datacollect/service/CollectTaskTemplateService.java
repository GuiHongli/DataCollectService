package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.dto.CollectTaskTemplateRequest;
import com.datacollect.entity.CollectTaskTemplate;

/**
 * 采集任务模版服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface CollectTaskTemplateService extends IService<CollectTaskTemplate> {
    
    /**
     * 创建采集任务模版
     * 
     * @param request 模版请求
     * @param createBy 创建人
     * @return 模版ID
     */
    Long createTemplate(CollectTaskTemplateRequest request, String createBy);
    
    /**
     * 更新采集任务模版
     * 
     * @param request 模版请求
     * @return 是否成功
     */
    Boolean updateTemplate(CollectTaskTemplateRequest request);
}

