package com.cpoa.config;

import com.cpoa.security.JwtCredentialsMatcher;
import com.cpoa.security.JwtFilter;
import com.cpoa.security.JwtRealm;
import com.cpoa.security.JwtUtil;
import com.cpoa.service.SysUserService;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.spring.web.config.DefaultShiroFilterChainDefinition;
import org.apache.shiro.spring.web.config.ShiroFilterChainDefinition;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class ShiroConfig {

    @Bean
    public JwtRealm jwtRealm(JwtUtil jwtUtil,
                             SysUserService userService,
                             StringRedisTemplate redisTemplate) {
        return new JwtRealm(jwtUtil, userService, new JwtCredentialsMatcher(redisTemplate));
    }

    /**
     * 无状态认证：关闭 Shiro Session 存储，登录态完全由 JWT + Redis 白名单承载。
     */
    @Bean
    public DefaultWebSecurityManager securityManager(JwtRealm jwtRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager(jwtRealm);

        DefaultSessionStorageEvaluator evaluator = new DefaultSessionStorageEvaluator();
        evaluator.setSessionStorageEnabled(false);

        DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
        subjectDAO.setSessionStorageEvaluator(evaluator);
        securityManager.setSubjectDAO(subjectDAO);
        return securityManager;
    }

    /**
     * 注册为 Bean 后，Shiro 3 会自动把它纳入过滤器链（按 Bean 名称 jwtFilter 引用）。
     */
    @Bean
    public JwtFilter jwtFilter(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        return new JwtFilter(jwtUtil, objectMapper);
    }

    /**
     * 防止 JwtFilter 被 Servlet 容器重复注册，它只应由 Shiro 过滤器链管理。
     */
    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter jwtFilter) {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(jwtFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public ShiroFilterChainDefinition shiroFilterChainDefinition() {
        DefaultShiroFilterChainDefinition chain = new DefaultShiroFilterChainDefinition();
        chain.addPathDefinition("/api/auth/login", "anon");
        chain.addPathDefinition("/api/health", "anon");
        // 飞书事件回调由飞书服务器直接访问，使用 Verification Token / 签名校验来源
        chain.addPathDefinition("/api/org/feishu/events", "anon");
        chain.addPathDefinition("/error", "anon");
        chain.addPathDefinition("/favicon.ico", "anon");
        chain.addPathDefinition("/**", "jwtFilter");
        return chain;
    }
}
