package com.datacollect.util;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * HTTP客户端工具类
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Component
public class HttpClientUtil {

    private final RestTemplate restTemplate;

    public HttpClientUtil() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 发送POST请求
     * 
     * @param url 请求URL
     * @param requestBody 请求体
     * @param responseType 响应类型
     * @return 响应结果
     */
    public <T> ResponseEntity<T> post(String url, Object requestBody, Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);
            
            log.debug("Sending POST request - URL: {}, Request body: {}", url, requestBody);
            
            ResponseEntity<T> response = restTemplate.postForEntity(url, entity, responseType);
            
            log.debug("Received response - Status code: {}, Response body: {}", response.getStatusCode(), response.getBody());
            
            return response;
            
        } catch (Exception e) {
            log.error("HTTP request exception - URL: {}, Error: {}", url, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 发送GET请求
     * 
     * @param url 请求URL
     * @param responseType 响应类型
     * @return 响应结果
     */
    public <T> ResponseEntity<T> get(String url, Class<T> responseType) {
        try {
            log.debug("Sending GET request - URL: {}", url);
            
            ResponseEntity<T> response = restTemplate.getForEntity(url, responseType);
            
            log.debug("Received response - Status code: {}, Response body: {}", response.getStatusCode(), response.getBody());
            
            return response;
            
        } catch (Exception e) {
            log.error("HTTP request exception - URL: {}, Error: {}", url, e.getMessage(), e);
            throw e;
        }
    }
}
