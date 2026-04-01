package com.datacollect.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

/**
 * AppCheckResponse测试类
 * 
 * @author system
 * @since 2024-01-01
 */
public class AppCheckResponseTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    public void testJsonSerialization() throws Exception {
        // 创建测试对象
        AppCheckResponse.AppCheckData data = new AppCheckResponse.AppCheckData("微信", true, false);
        AppCheckResponse response = new AppCheckResponse("success", Arrays.asList(data));
        
        // 序列化为JSON
        String json = objectMapper.writeValueAsString(response);
        
        // 验证JSON包含下划线命名的字段
        assertTrue(json.contains("\"app_name\""));
        assertTrue(json.contains("\"is_ios\""));
        assertTrue(json.contains("\"is_new\""));
        assertTrue(json.contains("\"微信\""));
        assertTrue(json.contains("true"));
        assertTrue(json.contains("false"));
        
        System.out.println("序列化结果: " + json);
    }
    
    @Test
    public void testJsonDeserialization() throws Exception {
        // JSON字符串
        String json = "{\"message\":\"success\",\"data\":[{\"app_name\":\"微信\",\"is_ios\":true,\"is_new\":false}]}";
        
        // 反序列化为对象
        AppCheckResponse response = objectMapper.readValue(json, AppCheckResponse.class);
        
        // 验证对象属性
        assertEquals("success", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        
        AppCheckResponse.AppCheckData data = response.getData().get(0);
        assertEquals("微信", data.getAppName());
        assertTrue(data.getIsIos());
        assertFalse(data.getIsNew());
        
        System.out.println("反序列化结果: " + response);
    }
}


