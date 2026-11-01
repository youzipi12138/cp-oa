package com.cpoa.common;

/**
 * Redis key 统一定义，避免散落在各处。
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 登录 Token 白名单：存在即有效，退出登录时删除 */
    public static final String AUTH_TOKEN_PREFIX = "cp-oa:auth:token:";

    public static String authToken(String token) {
        return AUTH_TOKEN_PREFIX + token;
    }
}
