package com.datacollect.config;

import com.datacollect.service.UserActivityService;
import com.datacollect.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserActivityService userActivityService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跳过OPTIONS请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        String requestURI = request.getRequestURI();
        
        // 跳过登录相关接口和静态资源
        if (requestURI.startsWith("/api/auth/") || 
            requestURI.startsWith("/static/") || 
            requestURI.equals("/") ||
            requestURI.endsWith(".html") ||
            requestURI.endsWith(".js") ||
            requestURI.endsWith(".css") ||
            requestURI.endsWith(".ico")) {
            return true;
        }
        
        // 检查JWT token
        String token = getTokenFromRequest(request);
        
        if (token == null || !jwtUtil.validateToken(token)) {
            // Token无效或过期，返回401状态码
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\",\"data\":null}");
            return false;
        }
        
        // 获取用户名
        String username = jwtUtil.getUsernameFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);
        
        // 检查用户活跃时间（1小时无操作则过期）
        if (!userActivityService.isUserActive(username)) {
            log.info("用户活跃时间已过期，拒绝访问 - 用户名: {}", username);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\",\"data\":null}");
            return false;
        }
        
        // 更新用户最后活跃时间
        userActivityService.updateLastActivityTime(username);
        
        // 将用户信息存储到request中，供后续使用
        request.setAttribute("username", username);
        request.setAttribute("role", role);
        
        return true;
    }
    
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

