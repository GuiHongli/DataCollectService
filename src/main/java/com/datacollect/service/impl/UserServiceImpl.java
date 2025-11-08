package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.User;
import com.datacollect.mapper.UserMapper;
import com.datacollect.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @Override
    public User findByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return this.getOne(queryWrapper);
    }
    
    @Override
    public User createUser(String username, String password, String role, String createBy) {
        // 检查用户名是否已存在
        User existingUser = findByUsername(username);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在: " + username);
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // 使用BCrypt加密密码
        user.setRole(role != null ? role : "user");
        user.setStatus(1);
        user.setCreateBy(createBy);
        
        this.save(user);
        log.info("创建用户成功 - 用户名: {}, 角色: {}", username, role);
        
        return user;
    }
    
    @Override
    public boolean updatePassword(Long userId, String newPassword) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        return this.updateById(user);
    }
    
    @Override
    public Page<User> getUserPage(Integer current, Integer size, String username, String role) {
        Page<User> page = new Page<>(current, size);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        
        if (username != null && !username.trim().isEmpty()) {
            queryWrapper.like("username", username);
        }
        
        if (role != null && !role.trim().isEmpty()) {
            queryWrapper.eq("role", role);
        }
        
        queryWrapper.orderByDesc("create_time");
        
        return this.page(page, queryWrapper);
    }
    
    /**
     * 验证密码（使用BCrypt加密比较）
     * 
     * BCryptPasswordEncoder.matches() 方法的工作原理：
     * 1. 接收用户输入的明文密码和数据库中存储的BCrypt加密密码
     * 2. 从加密密码中提取盐值（salt）
     * 3. 使用相同的盐值对明文密码进行BCrypt加密
     * 4. 比较加密结果是否一致
     * 
     * @param rawPassword 用户输入的明文密码
     * @param encodedPassword 数据库中存储的BCrypt加密密码（格式：$2a$10$...）
     * @return true-密码匹配，false-密码不匹配
     */
    @Override
    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            log.warn("密码验证失败：密码为空 - rawPassword: {}, encodedPassword: {}", 
                    rawPassword != null, encodedPassword != null);
            return false;
        }
        
        // 检查加密密码格式是否正确（BCrypt密码以 $2a$、$2b$ 或 $2y$ 开头）
        if (!encodedPassword.startsWith("$2a$") && 
            !encodedPassword.startsWith("$2b$") && 
            !encodedPassword.startsWith("$2y$")) {
            log.error("密码格式错误：不是有效的BCrypt格式 - encodedPassword: {}", 
                    encodedPassword.length() > 20 ? encodedPassword.substring(0, 20) + "..." : encodedPassword);
            return false;
        }
        
        try {
            // 使用BCrypt算法比较明文密码和加密密码
            // BCryptPasswordEncoder.matches() 会自动：
            // 1. 从加密密码中提取盐值
            // 2. 使用相同盐值加密明文密码
            // 3. 比较加密结果
            boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
            
            if (!matches) {
                log.debug("密码验证失败：密码不匹配 - 用户名: {}, 密码长度: {}", 
                        rawPassword.length(), encodedPassword.length());
            } else {
                log.debug("密码验证成功");
            }
            
            return matches;
        } catch (Exception e) {
            log.error("密码验证异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 验证密码（兼容旧方法，已废弃，请使用matchesPassword）
     * @deprecated 请使用 matchesPassword 方法
     */
    @Deprecated
    public boolean matches(String rawPassword, String encodedPassword) {
        return matchesPassword(rawPassword, encodedPassword);
    }
}



