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
}

