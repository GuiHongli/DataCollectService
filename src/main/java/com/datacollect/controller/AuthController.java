package com.datacollect.controller;

import com.datacollect.common.Result;
import com.datacollect.entity.User;
import com.datacollect.service.UserActivityService;
import com.datacollect.service.UserService;
import com.datacollect.util.JwtUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserActivityService userActivityService;
    
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
    
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            log.info("用户登录请求 - 用户名: {}", request.getUsername());
            
            if (request.getUsername() == null || request.getPassword() == null) {
                return Result.error("用户名和密码不能为空");
            }
            
            // 查找用户
            User user = userService.findByUsername(request.getUsername());
            if (user == null) {
                log.warn("用户不存在 - 用户名: {}", request.getUsername());
                return Result.error("用户名或密码错误");
            }
            
            // 检查用户状态
            if (user.getStatus() == 0) {
                log.warn("用户已被禁用 - 用户名: {}", request.getUsername());
                return Result.error("用户已被禁用");
            }
            
            // 验证密码（使用BCrypt加密比较）
            // 说明：
            // 1. request.getPassword() 是用户输入的明文密码（如：admin123）
            // 2. user.getPassword() 是数据库中存储的BCrypt加密密码（如：$2a$10$...）
            // 3. BCryptPasswordEncoder.matches() 方法会：
            //    - 从加密密码中提取盐值
            //    - 使用相同盐值对明文密码进行加密
            //    - 比较加密结果是否一致
            String rawPassword = request.getPassword();
            String encodedPassword = user.getPassword();
            
            log.debug("开始验证密码 - 用户名: {}, 明文密码长度: {}, 加密密码长度: {}, 加密密码前缀: {}", 
                    request.getUsername(), 
                    rawPassword != null ? rawPassword.length() : 0,
                    encodedPassword != null ? encodedPassword.length() : 0,
                    encodedPassword != null && encodedPassword.length() > 4 ? encodedPassword.substring(0, 4) : "null");
            
            if (!userService.matchesPassword(rawPassword, encodedPassword)) {
                log.warn("密码验证失败 - 用户名: {}", request.getUsername());
                return Result.error("用户名或密码错误");
            }
            
            log.debug("密码验证成功 - 用户名: {}", request.getUsername());
            
            // 生成JWT Token
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
            
            // 更新用户活跃时间
            userActivityService.updateLastActivityTime(user.getUsername());
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("username", user.getUsername());
            data.put("role", user.getRole());
            
            log.info("用户登录成功 - 用户名: {}, 角色: {}", user.getUsername(), user.getRole());
            return Result.success(data);
            
        } catch (Exception e) {
            log.error("登录异常: {}", e.getMessage(), e);
            return Result.error("登录失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                String username = jwtUtil.getUsernameFromToken(token);
                // 清除用户活跃时间
                userActivityService.clearUserActivity(username);
                log.info("用户退出登录 - 用户名: {}", username);
            } else {
                log.info("用户退出登录");
            }
        } catch (Exception e) {
            log.warn("退出登录时清除活跃时间失败: {}", e.getMessage());
        }
        return Result.success(null);
    }
    
    @GetMapping("/info")
    public Result<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            String username = jwtUtil.getUsernameFromToken(token);
            
            User user = userService.findByUsername(username);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("username", user.getUsername());
            data.put("role", user.getRole());
            data.put("status", user.getStatus());
            
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取用户信息异常: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败");
        }
    }
}

