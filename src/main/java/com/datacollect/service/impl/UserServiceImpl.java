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
     * @param rawPassword 用户输入的明文密码
     * @param encodedPassword 数据库中存储的BCrypt加密密码
     * @return true-密码匹配，false-密码不匹配
     */
    @Override
    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        // 使用BCrypt算法比较明文密码和加密密码
        // BCrypt会自动处理加密和比较，确保安全性
        return passwordEncoder.matches(rawPassword, encodedPassword);
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



