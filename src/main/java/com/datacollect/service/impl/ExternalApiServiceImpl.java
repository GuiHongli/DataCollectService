package com.datacollect.service.impl;

import com.datacollect.dto.AppCheckRequest;
import com.datacollect.dto.AppCheckResponse;
import com.datacollect.dto.GetDailyRankRequest;
import com.datacollect.dto.GetDailyRankResponse;
import com.datacollect.dto.GetVersionHistoryRequest;
import com.datacollect.dto.GetVersionHistoryResponse;
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
        
        log.info("Calling external API to check if app is new - URL: {}, request parameters: {}", url, appCheckRequests);
        
        try {
            ResponseEntity<AppCheckResponse> response = 
                httpClientUtil.post(url, appCheckRequests, AppCheckResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                AppCheckResponse result = response.getBody();
                log.info("External API call successful - response: {}", result);
                return result;
            } else {
                log.error("External API call failed - HTTP status code: {}", response.getStatusCode());
                throw new RuntimeException("External API call failed, HTTP status code: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Exception calling external API - URL: {}, error message: {}", url, e.getMessage(), e);
            throw new RuntimeException("Exception calling external API: " + e.getMessage(), e);
        }
    }
    
    @Override
    public UpdateProbedStatusResponse updateProbedStatus(List<String> appNames) {
        String url = externalApiHost + "/api/apps/updata_probed_status";
        
        log.info("Calling external API to update probed status - URL: {}, request parameters: {}", url, appNames);
        
        try {
            UpdateProbedStatusRequest request = new UpdateProbedStatusRequest(appNames);
            ResponseEntity<UpdateProbedStatusResponse> response = 
                httpClientUtil.post(url, request, UpdateProbedStatusResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                UpdateProbedStatusResponse result = response.getBody();
                log.info("External API call successful - response: {}", result);
                return result;
            } else {
                log.error("External API call failed - HTTP status code: {}", response.getStatusCode());
                throw new RuntimeException("External API call failed, HTTP status code: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Exception calling external API - URL: {}, error message: {}", url, e.getMessage(), e);
            throw new RuntimeException("Exception calling external API: " + e.getMessage(), e);
        }
    }
    
    @Override
    public GetDailyRankResponse getDailyRank(GetDailyRankRequest request) {
        String url = externalApiHost + "/api/apps/get_daily_rank";
        
        log.info("Calling external API to get daily rank - URL: {}, request parameters: {}", url, request);
        
        try {
            ResponseEntity<GetDailyRankResponse> response = 
                httpClientUtil.post(url, request, GetDailyRankResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                GetDailyRankResponse result = response.getBody();
                log.info("External API call successful - response: {}", result);
                return result;
            } else {
                log.error("External API call failed - HTTP status code: {}", response.getStatusCode());
                throw new RuntimeException("External API call failed, HTTP status code: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Exception calling external API - URL: {}, error message: {}", url, e.getMessage(), e);
            throw new RuntimeException("Exception calling external API: " + e.getMessage(), e);
        }
    }
    
    @Override
    public GetVersionHistoryResponse getVersionHistory(GetVersionHistoryRequest request) {
        String url = externalApiHost + "/api/apps/get_version_history";
        
        log.info("Calling external API to get version history - URL: {}, request parameters: {}", url, request);
        
        try {
            ResponseEntity<GetVersionHistoryResponse> response = 
                httpClientUtil.post(url, request, GetVersionHistoryResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                GetVersionHistoryResponse result = response.getBody();
                log.info("External API call successful - response: {}", result);
                return result;
            } else {
                log.error("External API call failed - HTTP status code: {}", response.getStatusCode());
                throw new RuntimeException("External API call failed, HTTP status code: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Exception calling external API - URL: {}, error message: {}", url, e.getMessage(), e);
            throw new RuntimeException("Exception calling external API: " + e.getMessage(), e);
        }
    }
}

