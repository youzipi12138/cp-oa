package com.cpoa.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cp-oa.jwt")
public class JwtProperties {

    /** HMAC 密钥，长度至少 32 字节 */
    private String secret;

    /** Token 有效期（分钟） */
    private long expireMinutes = 720;
}
