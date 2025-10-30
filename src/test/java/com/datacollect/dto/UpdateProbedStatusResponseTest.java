package com.datacollect.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * UpdateProbedStatusResponse测试类
 * 
 * @author system
 * @since 2024-01-01
 */
public class UpdateProbedStatusResponseTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    public void testJsonSerialization() throws Exception {
        // 创建测试对象
        UpdateProbedStatusResponse.UpdateProbedStatusData data = 
            new UpdateProbedStatusResponse.UpdateProbedStatusData(3);
        UpdateProbedStatusResponse response = new UpdateProbedStatusResponse("success", data);
        
        // 序列化为JSON
        String json = objectMapper.writeValueAsString(response);
        
        // 验证JSON包含下划线命名的字段
        assertTrue(json.contains("\"updata_count\""));
        assertTrue(json.contains("\"success\""));
        assertTrue(json.contains("3"));
        
        System.out.println("序列化结果: " + json);
    }
    
    @Test
    public void testJsonDeserialization() throws Exception {
        // JSON字符串
        String json = "{\"message\":\"success\",\"data\":{\"updata_count\":3}}";
        
        // 反序列化为对象
        UpdateProbedStatusResponse response = objectMapper.readValue(json, UpdateProbedStatusResponse.class);
        
        // 验证对象属性
        assertEquals("success", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(3, response.getData().getUpdataCount());
        
        System.out.println("反序列化结果: " + response);
    }
}
