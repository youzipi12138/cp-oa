package com.cpoa.security;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * Shiro 使用的 JWT 认证令牌。
 */
public class JwtToken implements AuthenticationToken {

    private final String token;

    public JwtToken(String token) {
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}
