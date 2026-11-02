package com.cpoa.service;

import com.cpoa.common.BusinessException;
import com.cpoa.common.RedisKeys;
import com.cpoa.config.FeishuProperties;
import com.cpoa.feishu.FeishuEventCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;

/**
 * 飞书事件订阅回调处理：URL 校验、来源校验、幂等去重，并触发组织架构同步。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuEventService {

    /** 飞书重试同一事件的间隔通常不超过 1 小时，这里只保留 10 分钟窗口 */
    private static final Duration EVENT_DEDUP_TTL = Duration.ofMinutes(10);

    private final FeishuProperties feishuProperties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final FeishuSyncTrigger syncTrigger;

    public Map<String, Object> handle(String body, String timestamp, String nonce, String signature) {
        if (!feishuProperties.isWebhookConfigured()) {
            throw new BusinessException(503, "未配置 FEISHU_VERIFICATION_TOKEN，无法接收飞书事件");
        }
        String payload = verifyAndDecrypt(body, timestamp, nonce, signature);
        JsonNode root = parse(payload);

        // 在飞书后台配置请求地址时发送的 URL 校验
        if ("url_verification".equals(text(root, "type"))) {
            verifyToken(text(root, "token"));
            String challenge = text(root, "challenge");
            if (!StringUtils.hasText(challenge)) {
                throw new BusinessException(400, "飞书 URL 校验缺少 challenge");
            }
            return Map.of("challenge", challenge);
        }

        JsonNode header = root.path("header");
        verifyToken(text(header, "token"));

        String eventType = text(header, "event_type");
        if (eventType == null || !eventType.startsWith("contact.")) {
            log.debug("忽略非通讯录事件: {}", eventType);
            return Map.of("code", 0);
        }

        String eventId = text(header, "event_id");
        if (!firstDelivery(eventId)) {
            log.debug("忽略重复的飞书事件: {}", eventId);
            return Map.of("code", 0);
        }

        boolean scheduled = syncTrigger.trigger("飞书事件 " + eventType);
        log.info("收到飞书通讯录事件 {}，{}", eventType, scheduled ? "已安排同步" : "已合并到待执行的同步");
        return Map.of("code", 0);
    }

    /**
     * 配置了 Encrypt Key 时校验签名并解密报文，否则按明文处理。
     */
    private String verifyAndDecrypt(String body, String timestamp, String nonce, String signature) {
        String encryptKey = feishuProperties.getEncryptKey();
        if (StringUtils.hasText(encryptKey)) {
            if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)
                    || !StringUtils.hasText(signature)) {
                throw new BusinessException(401, "缺少飞书事件签名头");
            }
            String expected = FeishuEventCrypto.sign(timestamp, nonce, encryptKey, body);
            if (!constantTimeEquals(expected, signature)) {
                throw new BusinessException(401, "飞书事件签名校验失败");
            }
        }

        JsonNode root = parse(body);
        String encrypted = text(root, "encrypt");
        if (!StringUtils.hasText(encrypted)) {
            return body;
        }
        if (!StringUtils.hasText(encryptKey)) {
            throw new BusinessException(401, "收到加密事件，但未配置 FEISHU_ENCRYPT_KEY");
        }
        return FeishuEventCrypto.decrypt(encryptKey, encrypted);
    }

    private void verifyToken(String token) {
        if (!constantTimeEquals(feishuProperties.getVerificationToken(), token)) {
            throw new BusinessException(401, "飞书事件 Verification Token 校验失败");
        }
    }

    private boolean firstDelivery(String eventId) {
        if (!StringUtils.hasText(eventId)) {
            return true;
        }
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(RedisKeys.feishuEvent(eventId), "1", EVENT_DEDUP_TTL);
        return Boolean.TRUE.equals(first);
    }

    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new BusinessException(400, "飞书事件报文格式错误");
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }
}
