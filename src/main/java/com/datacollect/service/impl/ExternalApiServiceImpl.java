package com.datacollect.service.impl;

import com.datacollect.dto.AppCheckRequest;
import com.datacollect.dto.AppCheckResponse;
import com.datacollect.dto.GetDailyRankRequest;
import com.datacollect.dto.GetDailyRankResponse;
import com.datacollect.dto.UpdateProbedStatusRequest;
import com.datacollect.dto.UpdateProbedStatusResponse;
import com.datacollect.service.ExternalApiService;
import com.datacollect.util.HttpClientUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 外部接口调用服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class ExternalApiServiceImpl implements ExternalApiService {
    
    @Autowired
    private HttpClientUtil httpClientUtil;
    
    @Value("${external.api.host:http://localhost:9000}")
    private String externalApiHost;
    
    @Override
    public AppCheckResponse checkAppIsNew(List<AppCheckRequest> appCheckRequests) {
        String url = externalApiHost + "/api/apps/check_is_new";
        
        log.info("调用外部接口检查应用是否为新应用 - URL: {}, 请求参数: {}", url, appCheckRequests);
        
        try {
            ResponseEntity<AppCheckResponse> response = 
                httpClientUtil.post(url, appCheckRequests, AppCheckResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                AppCheckResponse result = response.getBody();
                log.info("外部接口调用成功 - 响应: {}", result);
                return result;
            } else {
                log.error("外部接口调用失败 - HTTP状态码: {}", response.getStatusCode());
                throw new RuntimeException("外部接口调用失败，HTTP状态码: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("调用外部接口异常 - URL: {}, 错误信息: {}", url, e.getMessage(), e);
            throw new RuntimeException("调用外部接口异常: " + e.getMessage(), e);
        }
    }
    
    @Override
    public UpdateProbedStatusResponse updateProbedStatus(List<String> appNames) {
        String url = externalApiHost + "/api/apps/updata_probed_status";
        
        log.info("调用外部接口更新探测状态 - URL: {}, 请求参数: {}", url, appNames);
        
        try {
            UpdateProbedStatusRequest request = new UpdateProbedStatusRequest(appNames);
            ResponseEntity<UpdateProbedStatusResponse> response = 
                httpClientUtil.post(url, request, UpdateProbedStatusResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                UpdateProbedStatusResponse result = response.getBody();
                log.info("外部接口调用成功 - 响应: {}", result);
                return result;
            } else {
                log.error("外部接口调用失败 - HTTP状态码: {}", response.getStatusCode());
                throw new RuntimeException("外部接口调用失败，HTTP状态码: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("调用外部接口异常 - URL: {}, 错误信息: {}", url, e.getMessage(), e);
            throw new RuntimeException("调用外部接口异常: " + e.getMessage(), e);
        }
    }
    
    @Override
    public GetDailyRankResponse getDailyRank(GetDailyRankRequest request) {
        String url = externalApiHost + "/api/apps/get_daily_rank";
        
        log.info("调用外部接口获取每日排名 - URL: {}, 请求参数: {}", url, request);
        
        try {
            ResponseEntity<GetDailyRankResponse> response = 
                httpClientUtil.post(url, request, GetDailyRankResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                GetDailyRankResponse result = response.getBody();
                log.info("外部接口调用成功 - 响应: {}", result);
                return result;
            } else {
                log.error("外部接口调用失败 - HTTP状态码: {}", response.getStatusCode());
                throw new RuntimeException("外部接口调用失败，HTTP状态码: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("调用外部接口异常 - URL: {}, 错误信息: {}", url, e.getMessage(), e);
            throw new RuntimeException("调用外部接口异常: " + e.getMessage(), e);
        }
    }
}

