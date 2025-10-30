package com.datacollect.service;

import com.datacollect.dto.AppCheckRequest;
import com.datacollect.dto.AppCheckResponse;
import com.datacollect.dto.UpdateProbedStatusRequest;
import com.datacollect.dto.UpdateProbedStatusResponse;

import java.util.List;

/**
 * 外部接口调用服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface ExternalApiService {
    
    /**
     * 检查应用是否为新应用
     * 
     * @param appCheckRequests 应用检查请求列表
     * @return 应用检查响应
     */
    AppCheckResponse checkAppIsNew(List<AppCheckRequest> appCheckRequests);
    
    /**
     * 更新探测状态
     * 
     * @param appNames 应用名称列表
     * @return 更新探测状态响应
     */
    UpdateProbedStatusResponse updateProbedStatus(List<String> appNames);
}

