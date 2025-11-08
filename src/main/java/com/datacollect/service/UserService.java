package com.datacollect.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.User;

public interface UserService extends IService<User> {
    
    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);
    
    /**
     * 创建用户
     */
    User createUser(String username, String password, String role, String createBy);
    
    /**
     * 更新用户密码
     */
    boolean updatePassword(Long userId, String newPassword);
    
    /**
     * 分页查询用户
     */
    Page<User> getUserPage(Integer current, Integer size, String username, String role);
    
    /**
     * 验证密码（使用BCrypt加密比较）
     * 
     * @param rawPassword 用户输入的明文密码
     * @param encodedPassword 数据库中存储的BCrypt加密密码
     * @return true-密码匹配，false-密码不匹配
     */
    boolean matchesPassword(String rawPassword, String encodedPassword);
}



