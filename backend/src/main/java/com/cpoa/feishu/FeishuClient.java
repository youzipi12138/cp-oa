package com.cpoa.feishu;

import com.cpoa.common.BusinessException;
import com.cpoa.config.FeishuProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 飞书开放平台通讯录 API 封装（应用身份 tenant_access_token）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuClient {

    private static final String TOKEN_CACHE_KEY = "cp-oa:feishu:tenant-token";
    private static final int PAGE_SIZE = 50;

    private final RestClient feishuRestClient;
    private final FeishuProperties properties;
    private final StringRedisTemplate redisTemplate;

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    /**
     * 获取并缓存 tenant_access_token。飞书 token 有效期通常为 2 小时，提前 2 分钟过期。
     */
    public String getTenantAccessToken() {
        if (!properties.isConfigured()) {
            throw new BusinessException("未配置飞书应用凭证，请设置 FEISHU_APP_ID / FEISHU_APP_SECRET");
        }

        String cached = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
        if (StringUtils.hasText(cached)) {
            return cached;
        }

        JsonNode response;
        try {
            response = feishuRestClient.post()
                    .uri("/open-apis/auth/v3/tenant_access_token/internal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "app_id", properties.getAppId(),
                            "app_secret", properties.getAppSecret()))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new BusinessException(502, "获取飞书 tenant_access_token 失败: " + e.getMessage());
        }

        if (response == null || response.path("code").asInt(-1) != 0) {
            String message = response == null ? "空响应" : response.path("msg").asString("");
            throw new BusinessException(502, "获取飞书 tenant_access_token 失败: " + message);
        }

        String token = response.path("tenant_access_token").asString();
        int expireSeconds = response.path("expire").asInt(7200);
        redisTemplate.opsForValue().set(
                TOKEN_CACHE_KEY,
                token,
                Duration.ofSeconds(Math.max(60, expireSeconds - 120L)));
        return token;
    }

    /**
     * 递归拉取根部门下的全部部门。
     */
    public List<JsonNode> listAllDepartments() {
        List<JsonNode> departments = new ArrayList<>();
        String pageToken = null;
        do {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("parent_department_id", "0");
            params.put("fetch_child", true);
            params.put("page_size", PAGE_SIZE);
            params.put("department_id_type", "open_department_id");
            if (pageToken != null) {
                params.put("page_token", pageToken);
            }

            JsonNode data = get("/open-apis/contact/v3/departments", params);
            data.path("items").forEach(departments::add);
            pageToken = nextPageToken(data);
        } while (pageToken != null);
        return departments;
    }

    /**
     * 获取某个部门下的直属用户（分页）。
     *
     * @param openDepartmentId 飞书 open_department_id，根部门为 0
     */
    public List<JsonNode> listDepartmentUsers(String openDepartmentId) {
        List<JsonNode> users = new ArrayList<>();
        String pageToken = null;
        do {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("department_id", openDepartmentId);
            params.put("page_size", PAGE_SIZE);
            params.put("user_id_type", "open_id");
            params.put("department_id_type", "open_department_id");
            if (pageToken != null) {
                params.put("page_token", pageToken);
            }

            JsonNode data = get("/open-apis/contact/v3/users/find_by_department", params);
            data.path("items").forEach(users::add);
            pageToken = nextPageToken(data);
        } while (pageToken != null);
        return users;
    }

    private JsonNode get(String path, Map<String, Object> params) {
        String token = getTenantAccessToken();
        try {
            JsonNode response = feishuRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(path);
                        params.forEach(uriBuilder::queryParam);
                        return uriBuilder.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new BusinessException(502, "飞书接口返回为空: " + path);
            }
            int code = response.path("code").asInt(-1);
            if (code != 0) {
                throw new BusinessException(502,
                        "飞书接口调用失败(" + code + "): " + response.path("msg").asString(""));
            }
            return response.path("data");
        } catch (RestClientException e) {
            throw new BusinessException(502, "调用飞书接口失败 " + path + ": " + e.getMessage());
        }
    }

    private String nextPageToken(JsonNode data) {
        if (!data.path("has_more").asBoolean(false)) {
            return null;
        }
        String token = data.path("page_token").asString(null);
        return StringUtils.hasText(token) ? token : null;
    }
}
