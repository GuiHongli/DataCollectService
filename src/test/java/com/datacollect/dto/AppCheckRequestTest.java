package com.datacollect.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AppCheckRequest测试类
 * 
 * @author system
 * @since 2024-01-01
 */
public class AppCheckRequestTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    public void testJsonSerialization() throws Exception {
        // 创建测试对象
        AppCheckRequest request = new AppCheckRequest("微信", true);
        
        // 序列化为JSON
        String json = objectMapper.writeValueAsString(request);
        
        // 验证JSON包含下划线命名的字段
        assertTrue(json.contains("\"app_name\""));
        assertTrue(json.contains("\"is_ios\""));
        assertTrue(json.contains("\"微信\""));
        assertTrue(json.contains("true"));
        
        System.out.println("序列化结果: " + json);
    }
    
    @Test
    public void testJsonDeserialization() throws Exception {
        // JSON字符串
        String json = "{\"app_name\":\"微信\",\"is_ios\":true}";
        
        // 反序列化为对象
        AppCheckRequest request = objectMapper.readValue(json, AppCheckRequest.class);
        
        // 验证对象属性
        assertEquals("微信", request.getAppName());
        assertTrue(request.getIsIos());
        
        System.out.println("反序列化结果: " + request);
    }
}


