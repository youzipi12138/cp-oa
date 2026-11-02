package com.cpoa.common;

/**
 * Redis key 统一定义，避免散落在各处。
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 登录 Token 白名单：存在即有效，退出登录时删除 */
    public static final String AUTH_TOKEN_PREFIX = "cp-oa:auth:token:";

    /** 飞书事件幂等去重：飞书在未及时收到响应时会重试同一事件 */
    public static final String FEISHU_EVENT_PREFIX = "cp-oa:feishu:event:";

    public static String authToken(String token) {
        return AUTH_TOKEN_PREFIX + token;
    }

    public static String feishuEvent(String eventId) {
        return FEISHU_EVENT_PREFIX + eventId;
    }
}
