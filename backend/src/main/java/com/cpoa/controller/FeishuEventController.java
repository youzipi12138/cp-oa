package com.cpoa.controller;

import com.cpoa.service.FeishuEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 飞书事件订阅回调。该接口由飞书服务器直接访问，不走 JWT 鉴权，
 * 来源可信性由 Verification Token / Encrypt Key 签名保证。
 */
@RestController
@RequestMapping("/api/org/feishu")
@RequiredArgsConstructor
public class FeishuEventController {

    private final FeishuEventService feishuEventService;

    @PostMapping("/events")
    public Map<String, Object> events(
            @RequestBody String body,
            @RequestHeader(value = "X-Lark-Request-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Lark-Request-Nonce", required = false) String nonce,
            @RequestHeader(value = "X-Lark-Signature", required = false) String signature) {
        return feishuEventService.handle(body, timestamp, nonce, signature);
    }
}
