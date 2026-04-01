package com.datacollect.service;

import com.datacollect.dto.AppCheckRequest;
import com.datacollect.dto.AppCheckResponse;
import com.datacollect.dto.GetDailyRankRequest;
import com.datacollect.dto.GetDailyRankResponse;
import com.datacollect.dto.GetVersionHistoryRequest;
import com.datacollect.dto.GetVersionHistoryResponse;
import com.datacollect.dto.GetSingleAppVersionHistoryRequest;
import com.datacollect.dto.GetSingleAppVersionHistoryResponse;
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
    
    /**
     * 获取每日排名
     * 
     * @param request 获取每日排名请求
     * @return 获取每日排名响应
     */
    GetDailyRankResponse getDailyRank(GetDailyRankRequest request);
    
    /**
     * 获取版本历史
     * 
     * @param request 获取版本历史请求
     * @return 获取版本历史响应
     */
    GetVersionHistoryResponse getVersionHistory(GetVersionHistoryRequest request);
    
    /**
     * 获取单个应用版本历史
     * 
     * @param request 获取单个应用版本历史请求
     * @return 获取单个应用版本历史响应
     */
    GetSingleAppVersionHistoryResponse getSingleAppVersionHistory(GetSingleAppVersionHistoryRequest request);
}

    /**
     * 获取单个应用版本历史
     * 
     * @param request 获取单个应用版本历史请求
     * @return 获取单个应用版本历史响应
     */
    GetSingleAppVersionHistoryResponse getSingleAppVersionHistory(GetSingleAppVersionHistoryRequest request);
}

