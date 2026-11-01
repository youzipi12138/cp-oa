package com.cpoa.security;

import com.cpoa.common.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 校验 Redis 中的 Token 白名单，实现服务端可主动失效（退出登录、单点下线）。
 */
@RequiredArgsConstructor
public class JwtCredentialsMatcher implements CredentialsMatcher {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        String jwt = String.valueOf(token.getCredentials());
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.authToken(jwt)));
    }
}
