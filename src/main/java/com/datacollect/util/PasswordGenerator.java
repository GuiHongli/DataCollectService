package com.datacollect.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码生成工具类
 * 用于生成BCrypt加密密码
 * 
 * 使用方法：
 * 1. 运行main方法
 * 2. 输入要加密的密码
 * 3. 复制生成的BCrypt密码到数据库
 */
public class PasswordGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 生成admin用户的密码（admin123）
        String adminPassword = "admin123";
        String adminEncoded = encoder.encode(adminPassword);
        System.out.println("========================================");
        System.out.println("admin用户密码生成：");
        System.out.println("明文密码: " + adminPassword);
        System.out.println("BCrypt加密密码: " + adminEncoded);
        System.out.println("========================================");
        
        // 验证密码是否正确
        boolean matches = encoder.matches(adminPassword, adminEncoded);
        System.out.println("密码验证结果: " + (matches ? "成功" : "失败"));
        System.out.println("========================================");
        
        // 生成user用户的密码（admin123）
        String userPassword = "admin123";
        String userEncoded = encoder.encode(userPassword);
        System.out.println("user用户密码生成：");
        System.out.println("明文密码: " + userPassword);
        System.out.println("BCrypt加密密码: " + userEncoded);
        System.out.println("========================================");
        
        // 验证密码是否正确
        boolean userMatches = encoder.matches(userPassword, userEncoded);
        System.out.println("密码验证结果: " + (userMatches ? "成功" : "失败"));
        System.out.println("========================================");
        
        // 注意：BCrypt每次加密结果都不同，但都可以用matches()方法验证
        System.out.println("\n注意：");
        System.out.println("1. BCrypt每次加密结果都不同（因为使用了随机盐值）");
        System.out.println("2. 但都可以用 matches(明文, 加密) 方法验证");
        System.out.println("3. 将生成的BCrypt密码更新到数据库的user表中");
    }
}


























