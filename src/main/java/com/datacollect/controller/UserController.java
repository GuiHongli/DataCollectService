package com.datacollect.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.User;
import com.datacollect.service.UserService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Data
    public static class CreateUserRequest {
        private String username;
        private String password;
        private String role;
    }
    
    @Data
    public static class UpdatePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
    
    /**
     * 创建用户（只有admin可以创建）
     */
    @PostMapping
    public Result<User> createUser(@RequestBody CreateUserRequest request, HttpServletRequest httpRequest) {
        try {
            // 检查当前用户是否为admin
            String role = (String) httpRequest.getAttribute("role");
            if (role == null || !"admin".equals(role)) {
                log.warn("非管理员用户尝试创建用户");
                return Result.error("只有管理员可以创建用户");
            }
            
            String createBy = (String) httpRequest.getAttribute("username");
            log.info("创建用户 - 用户名: {}, 角色: {}, 创建人: {}", request.getUsername(), request.getRole(), createBy);
            
            User user = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getRole() != null ? request.getRole() : "user",
                createBy
            );
            
            // 清除密码字段，不返回给前端
            user.setPassword(null);
            
            return Result.success(user);
        } catch (Exception e) {
            log.error("创建用户失败: {}", e.getMessage(), e);
            return Result.error("创建用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页查询用户
     * 非管理员只能查看自己的信息
     */
    @GetMapping("/page")
    public Result<Page<User>> getUserPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            HttpServletRequest httpRequest) {
        try {
            String currentRole = (String) httpRequest.getAttribute("role");
            String currentUsername = (String) httpRequest.getAttribute("username");
            
            Page<User> page;
            
            // 非管理员只能查看自己的信息
            if (currentRole == null || !"admin".equals(currentRole)) {
                log.info("非管理员用户查询自己的信息 - 用户名: {}", currentUsername);
                // 查询当前用户
                User currentUser = userService.findByUsername(currentUsername);
                if (currentUser == null) {
                    return Result.error("用户不存在");
                }
                
                // 创建只包含当前用户的页面结果
                page = new Page<>(1, 1);
                page.setTotal(1);
                page.setRecords(java.util.Arrays.asList(currentUser));
            } else {
                // 管理员可以查看所有用户
                page = userService.getUserPage(current, size, username, role);
            }
            
            // 清除所有用户的密码字段
            page.getRecords().forEach(user -> user.setPassword(null));
            
            return Result.success(page);
        } catch (Exception e) {
            log.error("查询用户列表失败: {}", e.getMessage(), e);
            return Result.error("查询用户列表失败");
        }
    }
    
    /**
     * 根据ID查询用户
     * 非管理员只能查询自己的信息
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            String currentRole = (String) httpRequest.getAttribute("role");
            String currentUsername = (String) httpRequest.getAttribute("username");
            
            User user = userService.getById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 非管理员只能查询自己的信息
            if (currentRole == null || !"admin".equals(currentRole)) {
                if (!user.getUsername().equals(currentUsername)) {
                    log.warn("非管理员用户尝试查询其他用户信息 - 当前用户: {}, 查询用户: {}", 
                            currentUsername, user.getUsername());
                    return Result.error("无权查看其他用户信息");
                }
            }
            
            user.setPassword(null);
            return Result.success(user);
        } catch (Exception e) {
            log.error("查询用户失败: {}", e.getMessage(), e);
            return Result.error("查询用户失败");
        }
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User user, HttpServletRequest httpRequest) {
        try {
            String currentRole = (String) httpRequest.getAttribute("role");
            String currentUsername = (String) httpRequest.getAttribute("username");
            
            User existingUser = userService.getById(id);
            if (existingUser == null) {
                return Result.error("用户不存在");
            }
            
            // 只有admin可以修改其他用户，普通用户只能修改自己的信息
            if (!"admin".equals(currentRole) && !existingUser.getUsername().equals(currentUsername)) {
                return Result.error("无权修改其他用户信息");
            }
            
            user.setId(id);
            // 不允许通过此接口修改密码
            user.setPassword(null);
            
            user.setUpdateBy(currentUsername);
            boolean success = userService.updateById(user);
            
            if (success) {
                User updatedUser = userService.getById(id);
                updatedUser.setPassword(null);
                return Result.success(updatedUser);
            } else {
                return Result.error("更新用户失败");
            }
        } catch (Exception e) {
            log.error("更新用户失败: {}", e.getMessage(), e);
            return Result.error("更新用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除用户（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            String role = (String) httpRequest.getAttribute("role");
            if (role == null || !"admin".equals(role)) {
                return Result.error("只有管理员可以删除用户");
            }
            
            boolean success = userService.removeById(id);
            if (success) {
                log.info("删除用户成功 - 用户ID: {}", id);
                return Result.success(null);
            } else {
                return Result.error("删除用户失败");
            }
        } catch (Exception e) {
            log.error("删除用户失败: {}", e.getMessage(), e);
            return Result.error("删除用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 修改密码
     * 非管理员只能修改自己的密码，且需要验证旧密码
     */
    @PostMapping("/{id}/password")
    public Result<Void> updatePassword(@PathVariable Long id, @RequestBody UpdatePasswordRequest request, HttpServletRequest httpRequest) {
        try {
            String currentUsername = (String) httpRequest.getAttribute("username");
            String currentRole = (String) httpRequest.getAttribute("role");
            
            User user = userService.getById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 只有admin可以修改其他用户密码，普通用户只能修改自己的密码
            if (!"admin".equals(currentRole) && !user.getUsername().equals(currentUsername)) {
                log.warn("非管理员用户尝试修改其他用户密码 - 当前用户: {}, 目标用户: {}", 
                        currentUsername, user.getUsername());
                return Result.error("无权修改其他用户密码");
            }
            
            // 如果不是admin，需要验证旧密码
            if (!"admin".equals(currentRole)) {
                if (request.getOldPassword() == null || request.getOldPassword().trim().isEmpty()) {
                    return Result.error("请输入旧密码");
                }
                
                // 验证旧密码
                if (!userService.matchesPassword(request.getOldPassword(), user.getPassword())) {
                    log.warn("修改密码失败：旧密码错误 - 用户名: {}", currentUsername);
                    return Result.error("旧密码错误");
                }
            }
            
            if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
                return Result.error("新密码不能为空");
            }
            
            if (request.getNewPassword().length() < 6) {
                return Result.error("新密码长度不能少于6位");
            }
            
            boolean success = userService.updatePassword(id, request.getNewPassword());
            if (success) {
                log.info("修改密码成功 - 用户ID: {}, 用户名: {}", id, user.getUsername());
                return Result.success(null);
            } else {
                return Result.error("修改密码失败");
            }
        } catch (Exception e) {
            log.error("修改密码失败: {}", e.getMessage(), e);
            return Result.error("修改密码失败: " + e.getMessage());
        }
    }
}

