package com.datacollect.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

/**
 * UpdateProbedStatusRequest测试类
 * 
 * @author system
 * @since 2024-01-01
 */
public class UpdateProbedStatusRequestTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    public void testJsonSerialization() throws Exception {
        // 创建测试对象
        List<String> appNames = Arrays.asList("微信", "微博");
        UpdateProbedStatusRequest request = new UpdateProbedStatusRequest(appNames);
        
        // 序列化为JSON
        String json = objectMapper.writeValueAsString(request);
        
        // 验证JSON包含下划线命名的字段
        assertTrue(json.contains("\"app_names\""));
        assertTrue(json.contains("\"微信\""));
        assertTrue(json.contains("\"微博\""));
        
        System.out.println("序列化结果: " + json);
    }
    
    @Test
    public void testJsonDeserialization() throws Exception {
        // JSON字符串
        String json = "{\"app_names\":[\"微信\",\"微博\"]}";
        
        // 反序列化为对象
        UpdateProbedStatusRequest request = objectMapper.readValue(json, UpdateProbedStatusRequest.class);
        
        // 验证对象属性
        assertNotNull(request.getAppNames());
        assertEquals(2, request.getAppNames().size());
        assertTrue(request.getAppNames().contains("微信"));
        assertTrue(request.getAppNames().contains("微博"));
        
        System.out.println("反序列化结果: " + request);
    }
}


